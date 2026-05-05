package net.zoogle.enchiridion.client.levelrpg.journal.layout;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class JournalPaginationEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_FRONT_MATTER_PAGINATION = false;

    private JournalPaginationEngine() {}

    public static List<BookPage> paginateBlocksIntoPages(List<BookTextBlock> blocks) {
        return paginateBlocksIntoPages(blocks, null);
    }

    public static List<BookPage> paginateBlocksIntoPages(List<BookTextBlock> blocks, String debugLabel) {
        List<MeasuredBlock> measuredBlocks = measureBlocks(blocks);
        List<BookPage> pages = new ArrayList<>();
        List<BookTextBlock> currentPageBlocks = new ArrayList<>();
        int usedHeight = 0;
        int pageIndex = 0;
        boolean hasMajorHeaderOnPage = false;

        if (shouldDebugPagination(debugLabel)) {
            logWrappedBlocks(debugLabel, measuredBlocks);
        }

        for (int index = 0; index < measuredBlocks.size(); ) {
            MeasuredBlock block = measuredBlocks.get(index);
            if (block.lines().isEmpty()) {
                index++;
                continue;
            }

            if (block.kind() != BookTextBlock.Kind.BODY) {
                int headerRunEnd = index;
                while (headerRunEnd < measuredBlocks.size() && measuredBlocks.get(headerRunEnd).kind() != BookTextBlock.Kind.BODY) {
                    headerRunEnd++;
                }

                boolean headerRunStartsMajorSection = startsMajorHeaderRun(measuredBlocks, index, headerRunEnd, currentPageBlocks);
                if (!currentPageBlocks.isEmpty() && hasMajorHeaderOnPage && headerRunStartsMajorSection) {
                    debugPageBreak(debugLabel, pageIndex, "major_header_push", currentPageBlocks, usedHeight, -1, -1, -1);
                    pages.add(new BookPage(currentPageBlocks));
                    currentPageBlocks = new ArrayList<>();
                    usedHeight = 0;
                    pageIndex++;
                    hasMajorHeaderOnPage = false;
                    continue;
                }

                int minimumGroupHeight = heightOfBlockRun(measuredBlocks, index, headerRunEnd);
                if (headerRunEnd < measuredBlocks.size() && !measuredBlocks.get(headerRunEnd).lines().isEmpty()) {
                    minimumGroupHeight += PageCanvasRenderer.BLOCK_SPACING + JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY);
                }

                int requiredHeight = minimumGroupHeight + (currentPageBlocks.isEmpty() ? 0 : PageCanvasRenderer.BLOCK_SPACING);
                if (!currentPageBlocks.isEmpty() && usedHeight + requiredHeight > JournalLayoutMetrics.PAGE_CONTENT_HEIGHT) {
                    debugPageBreak(debugLabel, pageIndex, "header_run_push", currentPageBlocks, usedHeight, -1, -1, -1);
                    pages.add(new BookPage(currentPageBlocks));
                    currentPageBlocks = new ArrayList<>();
                    usedHeight = 0;
                    pageIndex++;
                    hasMajorHeaderOnPage = false;
                    continue;
                }

                while (index < headerRunEnd) {
                    MeasuredBlock header = measuredBlocks.get(index);
                    int spacingBefore = currentPageBlocks.isEmpty() ? 0 : PageCanvasRenderer.BLOCK_SPACING;
                    int blockHeight = JournalLayoutMetrics.heightFor(header.kind(), header.lines().size());
                    if (!currentPageBlocks.isEmpty() && usedHeight + spacingBefore + blockHeight > JournalLayoutMetrics.PAGE_CONTENT_HEIGHT) {
                        debugPageBreak(debugLabel, pageIndex, "header_block_push", currentPageBlocks, usedHeight, -1, -1, -1);
                        pages.add(new BookPage(currentPageBlocks));
                        currentPageBlocks = new ArrayList<>();
                        usedHeight = 0;
                        pageIndex++;
                        hasMajorHeaderOnPage = false;
                        continue;
                    }

                    currentPageBlocks.add(new BookTextBlock(header.kind(), Component.literal(joinMeasuredLines(header.lines()))));
                    usedHeight += spacingBefore + blockHeight;
                    if (isMajorHeaderBlock(header.kind(), currentPageBlocks.size() == 1)) {
                        hasMajorHeaderOnPage = true;
                    }
                    index++;
                }
                continue;
            }

            int lineCursor = 0;
            while (lineCursor < block.lines().size()) {
                int spacingBefore = currentPageBlocks.isEmpty() ? 0 : PageCanvasRenderer.BLOCK_SPACING;
                int availableHeight = JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - usedHeight - spacingBefore;
                int lineCapacity = availableHeight / JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY);

                if (lineCapacity <= 0) {
                    if (!currentPageBlocks.isEmpty()) {
                        debugPageBreak(debugLabel, pageIndex, "body_no_capacity", currentPageBlocks, usedHeight, lineCursor, lineCursor, block.lines().size());
                        pages.add(new BookPage(currentPageBlocks));
                        currentPageBlocks = new ArrayList<>();
                        usedHeight = 0;
                        pageIndex++;
                        hasMajorHeaderOnPage = false;
                        continue;
                    }
                    lineCapacity = 1;
                }

                int take = chooseBodySplit(block.lines(), lineCursor, lineCapacity);
                currentPageBlocks.add(BookTextBlock.body(Component.literal(joinMeasuredLines(block.lines().subList(lineCursor, lineCursor + take)))));
                usedHeight += spacingBefore + (take * JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY));
                int chunkStart = lineCursor;
                lineCursor += take;

                if (lineCursor < block.lines().size()) {
                    debugPageBreak(debugLabel, pageIndex, "body_split", currentPageBlocks, usedHeight, chunkStart, lineCursor - 1, block.lines().size());
                    pages.add(new BookPage(currentPageBlocks));
                    currentPageBlocks = new ArrayList<>();
                    usedHeight = 0;
                    pageIndex++;
                    hasMajorHeaderOnPage = false;
                }
            }
            index++;
        }

        if (!currentPageBlocks.isEmpty()) {
            debugPageBreak(debugLabel, pageIndex, "final_page", currentPageBlocks, usedHeight, -1, -1, -1);
            pages.add(new BookPage(currentPageBlocks));
        }
        if (pages.isEmpty()) {
            pages.add(BookPage.empty());
        }
        return List.copyOf(pages);
    }

    public static List<MeasuredLine> wrapStaticText(String text, int width) {
        List<MeasuredLine> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String[] paragraphs = text.split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add(new MeasuredLine("", BreakPriority.NONE));
                continue;
            }
            List<String> wrapped = splitParagraph(paragraph, width);
            for (String line : wrapped) {
                lines.add(new MeasuredLine(line, BreakPriority.NONE));
            }
        }
        trimTrailingBlankLines(lines);
        return lines;
    }

    public static List<MeasuredLine> wrapBodyText(String text, int width) {
        List<MeasuredLine> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String[] paragraphs = text.split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add(new MeasuredLine("", BreakPriority.BLANK_LINE));
                continue;
            }
            List<String> wrapped = splitParagraph(paragraph, width);
            boolean bulletParagraph = isBulletParagraph(paragraph);
            for (int lineIndex = 0; lineIndex < wrapped.size(); lineIndex++) {
                String line = wrapped.get(lineIndex);
                BreakPriority priority = classifyInlineBreak(line);
                if (lineIndex == wrapped.size() - 1) {
                    priority = stronger(priority, bulletParagraph ? BreakPriority.BULLET_BOUNDARY : BreakPriority.PARAGRAPH_BOUNDARY);
                }
                lines.add(new MeasuredLine(line, priority));
            }
        }
        trimTrailingBlankLines(lines);
        return lines;
    }

    private static List<MeasuredBlock> measureBlocks(List<BookTextBlock> blocks) {
        List<MeasuredBlock> measuredBlocks = new ArrayList<>(blocks.size());
        for (BookTextBlock block : blocks) {
            measuredBlocks.add(new MeasuredBlock(block.kind(), wrapBlock(block)));
        }
        return measuredBlocks;
    }

    private static List<MeasuredLine> wrapBlock(BookTextBlock block) {
        return switch (block.kind()) {
            case BODY -> wrapBodyText(block.text().getString(), JournalLayoutMetrics.PAGE_CONTENT_WIDTH);
            case TITLE, SUBTITLE, LEVEL, SECTION -> wrapStaticText(block.text().getString(), JournalLayoutMetrics.PAGE_CONTENT_WIDTH);
        };
    }

    private static int chooseBodySplit(List<MeasuredLine> lines, int startIndex, int lineCapacity) {
        int remaining = lines.size() - startIndex;
        if (remaining <= lineCapacity) {
            return remaining;
        }
        int maxEndExclusive = startIndex + lineCapacity;
        int preferredStart = Math.max(startIndex, maxEndExclusive - JournalLayoutMetrics.BREAK_SEARCH_WINDOW);
        int bestIndex = bestBreakIndex(lines, preferredStart, maxEndExclusive);
        if (bestIndex < 0) {
            bestIndex = bestBreakIndex(lines, startIndex, maxEndExclusive);
        }
        if (bestIndex < 0) {
            return lineCapacity;
        }
        return (bestIndex - startIndex) + 1;
    }

    private static int bestBreakIndex(List<MeasuredLine> lines, int startInclusive, int endExclusive) {
        int bestIndex = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int index = startInclusive; index < endExclusive; index++) {
            BreakPriority priority = lines.get(index).breakPriority();
            if (priority == BreakPriority.NONE) {
                continue;
            }
            int score = (priority.weight() * 100) + index;
            if (score > bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private static boolean startsMajorHeaderRun(List<MeasuredBlock> measuredBlocks, int startInclusive, int endExclusive, List<BookTextBlock> currentPageBlocks) {
        boolean firstBlockOnFreshPage = currentPageBlocks.isEmpty();
        for (int index = startInclusive; index < endExclusive; index++) {
            BookTextBlock.Kind kind = measuredBlocks.get(index).kind();
            if (isMajorHeaderBlock(kind, firstBlockOnFreshPage && index == startInclusive)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMajorHeaderBlock(BookTextBlock.Kind kind, boolean firstBlockOnFreshPage) {
        if (kind == BookTextBlock.Kind.SECTION) {
            return true;
        }
        return kind == BookTextBlock.Kind.SUBTITLE && !firstBlockOnFreshPage;
    }

    private static List<String> splitParagraph(String paragraph, int width) {
        List<String> lines = new ArrayList<>();
        for (FormattedCharSequence sequence : Minecraft.getInstance().font.split(Component.literal(paragraph), width)) {
            lines.add(formattedLineToString(sequence));
        }
        if (lines.isEmpty()) {
            lines.add(paragraph);
        }
        return lines;
    }

    private static int heightOfBlockRun(List<MeasuredBlock> blocks, int startInclusive, int endExclusive) {
        int height = 0;
        boolean first = true;
        for (int index = startInclusive; index < endExclusive; index++) {
            MeasuredBlock block = blocks.get(index);
            if (block.lines().isEmpty()) {
                continue;
            }
            if (!first) {
                height += PageCanvasRenderer.BLOCK_SPACING;
            }
            height += JournalLayoutMetrics.heightFor(block.kind(), block.lines().size());
            first = false;
        }
        return height;
    }

    private static boolean isBulletParagraph(String paragraph) {
        String trimmed = paragraph.stripLeading();
        return trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("o ") || trimmed.matches("^\\d+[.)].*");
    }

    private static BreakPriority classifyInlineBreak(String line) {
        String trimmed = line.stripTrailing();
        if (trimmed.isEmpty()) {
            return BreakPriority.BLANK_LINE;
        }
        char last = trimmed.charAt(trimmed.length() - 1);
        return switch (last) {
            case '.', '!', '?' -> BreakPriority.SENTENCE_END;
            case ';', ':' -> BreakPriority.CLAUSE_BREAK;
            case ',' -> BreakPriority.COMMA_BREAK;
            default -> BreakPriority.NONE;
        };
    }

    private static BreakPriority stronger(BreakPriority left, BreakPriority right) {
        return left.weight() >= right.weight() ? left : right;
    }

    private static void trimTrailingBlankLines(List<MeasuredLine> lines) {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).text().isBlank()) {
            lines.remove(lines.size() - 1);
        }
    }

    private static String joinMeasuredLines(List<MeasuredLine> lines) {
        List<String> text = new ArrayList<>(lines.size());
        for (MeasuredLine line : lines) {
            text.add(line.text());
        }
        return String.join("\n", text);
    }

    private static String formattedLineToString(FormattedCharSequence line) {
        if (line == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private static boolean shouldDebugPagination(String debugLabel) {
        return DEBUG_FRONT_MATTER_PAGINATION && debugLabel != null && debugLabel.startsWith("front_matter");
    }

    private static void logWrappedBlocks(String debugLabel, List<MeasuredBlock> measuredBlocks) {
        for (int blockIndex = 0; blockIndex < measuredBlocks.size(); blockIndex++) {
            MeasuredBlock block = measuredBlocks.get(blockIndex);
            LOGGER.info("[Enchiridion] {} block {} kind={} wrappedLines={} blockHeight={} text={}",
                    debugLabel, blockIndex, block.kind(), block.lines().size(),
                    JournalLayoutMetrics.heightFor(block.kind(), block.lines().size()), previewBlockText(block.lines()));
        }
    }

    private static void debugPageBreak(String debugLabel, int pageIndex, String reason, List<BookTextBlock> currentPageBlocks, int usedHeight, int lineStart, int lineEnd, int totalLines) {
        if (!shouldDebugPagination(debugLabel)) {
            return;
        }
        int finalY = PageCanvasRenderer.PAGE_MARGIN_Y + usedHeight;
        LOGGER.info("[Enchiridion] {} page {} reason={} blocks={} usedHeight={} finalY={} safeBottom={} lineRange={}..{} of {}",
                debugLabel, pageIndex, reason, currentPageBlocks.size(), usedHeight, finalY,
                PageCanvasRenderer.PAGE_MARGIN_Y + JournalLayoutMetrics.PAGE_CONTENT_HEIGHT,
                lineStart, lineEnd, totalLines);
    }

    private static String previewBlockText(List<MeasuredLine> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int previewLines = Math.min(2, lines.size());
        for (int i = 0; i < previewLines; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(lines.get(i).text());
        }
        return builder.toString();
    }

    public record MeasuredBlock(BookTextBlock.Kind kind, List<MeasuredLine> lines) {}
    public record MeasuredLine(String text, BreakPriority breakPriority) {}

    enum BreakPriority {
        NONE(0), COMMA_BREAK(1), CLAUSE_BREAK(2), SENTENCE_END(3), BULLET_BOUNDARY(4), PARAGRAPH_BOUNDARY(5), BLANK_LINE(6);

        private final int weight;

        BreakPriority(int weight) {
            this.weight = weight;
        }

        int weight() {
            return weight;
        }
    }
}
