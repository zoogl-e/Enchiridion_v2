package net.zoogle.enchiridion.client.levelrpg;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public final class LevelRpgJournalBookProvider implements BookPageProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_FRONT_MATTER_PAGINATION = false;
    private static final int TEXTURE_PAGE_TEXT_WIDTH = 115;
    private static final int TEXTURE_PAGE_TEXT_HEIGHT = 145;
    private static final int PAGE_BOTTOM_SAFE_BUFFER = 10;
    // Provider pagination must stay ahead of the final textured render, especially near the lower page edge.
    private static final int PAGE_CONTENT_WIDTH = TEXTURE_PAGE_TEXT_WIDTH - 3;
    private static final int PAGE_CONTENT_HEIGHT = TEXTURE_PAGE_TEXT_HEIGHT - PAGE_BOTTOM_SAFE_BUFFER;
    private static final int BREAK_SEARCH_WINDOW = 3;
    private static final String READY_MARK = "Skill Point Ready";
    private static final int XP_BAR_SEGMENTS = 12;

    @Override
    public int spreadCount(BookContext context) {
        return buildDocument(context).spreads().size();
    }

    @Override
    public BookSpread getSpread(BookContext context, int spreadIndex) {
        List<BookSpread> spreads = buildDocument(context).spreads();
        return spreadIndex >= 0 && spreadIndex < spreads.size()
                ? spreads.get(spreadIndex)
                : BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    @Override
    public List<BookInteractiveRegion> interactiveRegions(BookContext context, int spreadIndex) {
        return buildDocument(context).interactiveRegionsBySpread().getOrDefault(spreadIndex, List.of());
    }

    @Override
    public BookProjectionView projection(BookContext context, String focusId) {
        if (focusId == null || focusId.isBlank()) {
            return null;
        }
        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        for (JournalSkillEntry skill : snapshot.skills()) {
            if (!focusId.equals(skill.name())) {
                continue;
            }
            float progress = skill.masteryRequiredForNextLevel() <= 0L
                    ? 1.0f
                    : Math.clamp((float) skill.masteryProgress() / (float) skill.masteryRequiredForNextLevel(), 0.0f, 1.0f);
            return new BookProjectionView(
                    skill.name(),
                    Component.literal(skill.name()),
                    Component.literal(String.valueOf(skill.investedSkillLevel())),
                    Component.literal("Mastery " + skill.masteryLevel()),
                    Component.literal(compactSkillDescription(skill.roleSummary())),
                    Component.literal("Mastery " + skill.masteryProgressText()),
                    progress,
                    Component.literal(skill.canSpendSkillPoint()
                            ? "A Skill Point is ready for this discipline."
                            : "No Skill Point is ready for this discipline."),
                    skill.canSpendSkillPoint(),
                    skill.canSpendSkillPoint() ? Component.literal("Spend Point") : null,
                    skill.canSpendSkillPoint()
                            ? (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                            && LevelRpgJournalInteractionBridge.requestSpendSkillPoint(bookContext, skill.name())
                            : null
            );
        }
        return null;
    }

    @Override
    public String nextProjectionFocus(BookContext context, String currentFocusId) {
        return adjacentProjectionFocus(buildDocument(context).projectionFocusOrder(), currentFocusId, 1);
    }

    @Override
    public String previousProjectionFocus(BookContext context, String currentFocusId) {
        return adjacentProjectionFocus(buildDocument(context).projectionFocusOrder(), currentFocusId, -1);
    }

    @Override
    public int spreadForProjectionFocus(BookContext context, String focusId) {
        return buildDocument(context).projectionSpreadByFocus().getOrDefault(focusId, -1);
    }

    @Override
    public int pageIndexForProjectionFocus(BookContext context, String focusId) {
        return buildDocument(context).projectionPageByFocus().getOrDefault(focusId, -1);
    }

    private static DocumentLayout buildDocument(BookContext context) {
        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        List<BookPage> pages = new ArrayList<>();
        Map<Integer, List<BookInteractiveRegion>> pageRegions = new LinkedHashMap<>();

        List<BookPage> introPages = buildLegacyOpeningPages(context);
        List<NamedSectionLayout> skillSections = new ArrayList<>();
        for (JournalSkillEntry skill : snapshot.skills()) {
            skillSections.add(new NamedSectionLayout(skill.name(), buildSkillPages(skill)));
        }

        List<BookPage> identityPages = buildIdentityPages(snapshot.characterSheet());
        int characterPageStart = introPages.size();
        Map<String, Integer> skillStartPages = new LinkedHashMap<>();
        int nextPageStart = characterPageStart + identityPages.size() + ledgerPageCount(snapshot.characterSheet().stats());
        for (NamedSectionLayout skillSection : skillSections) {
            skillStartPages.put(skillSection.label(), nextPageStart);
            nextPageStart += skillSection.pages().size();
        }

        List<BookPage> ledgerPages = buildLedgerPages(snapshot.characterSheet().stats(), skillStartPages);

        pages.addAll(introPages);
        pages.addAll(identityPages);
        pages.addAll(ledgerPages);
        for (NamedSectionLayout skillSection : skillSections) {
            pages.addAll(skillSection.pages());
        }

        List<String> projectionFocusOrder = snapshot.skills().stream().map(JournalSkillEntry::name).toList();
        Map<String, Integer> projectionSpreadByFocus = new LinkedHashMap<>();
        Map<String, Integer> projectionPageByFocus = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : skillStartPages.entrySet()) {
            projectionSpreadByFocus.put(entry.getKey(), entry.getValue() / 2);
            projectionPageByFocus.put(entry.getKey(), entry.getValue());
        }

        return pairPagesIntoSpreads(pages, pageRegions, projectionFocusOrder, projectionSpreadByFocus, projectionPageByFocus);
    }

    private static List<BookPage> buildLegacyOpeningPages(BookContext context) {
        String playerName = context != null && context.player() != null
                ? context.player().getName().getString()
                : "Unknown";
        return List.of(
                BookPage.of(
                        BookTextBlock.title(Component.literal(playerName + "'s")),
                        BookTextBlock.title(Component.literal("Legacy"))
                ),
                BookPage.empty()
        );
    }

    private static List<BookPage> buildIdentityPages(JournalCharacterSheet characterSheet) {
        List<BookPage> pages = new ArrayList<>();
        pages.addAll(paginateBlocksIntoPages(List.of(
                BookTextBlock.title(Component.literal(characterSheet.title())),
                BookTextBlock.body(Component.literal(formatIdentityRecord(characterSheet.identitySummary())))
        )));
        return List.copyOf(pages);
    }

    private static int ledgerPageCount(List<JournalCharacterStat> stats) {
        return Math.max(1, (int) Math.ceil(stats.size() / (double) ledgerRowsPerPage()));
    }

    private static List<BookPage> buildLedgerPages(List<JournalCharacterStat> stats, Map<String, Integer> skillStartPages) {
        int pageCount = ledgerPageCount(stats);
        List<BookPage> pages = new ArrayList<>(pageCount);
        int rowsPerPage = ledgerRowsPerPage();
        int bodyStartY = ledgerBodyStartY();
        int lineHeight = lineHeightFor(BookTextBlock.Kind.BODY);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            List<BookPageElement> elements = new ArrayList<>();
            if (pageIndex == 0) {
                appendTextElements(elements, BookTextBlock.Kind.SECTION, "Stat Ledger", PageCanvasRenderer.PAGE_MARGIN_X, PageCanvasRenderer.PAGE_MARGIN_Y, PAGE_CONTENT_WIDTH, false, null, null);
            }
            int rowStart = pageIndex * rowsPerPage;
            int rowEnd = Math.min(stats.size(), rowStart + rowsPerPage);
            for (int statIndex = rowStart; statIndex < rowEnd; statIndex++) {
                JournalCharacterStat stat = stats.get(statIndex);
                Integer targetPageIndex = skillStartPages.get(stat.name());
                if (targetPageIndex == null) {
                    continue;
                }
                int rowY = bodyStartY + ((statIndex - rowStart) * lineHeight);
                LedgerRowLayout rowLayout = ledgerRowLayout(stat, rowY, targetPageIndex);
                elements.add(ledgerLabel(rowLayout));
                elements.add(new BookPageElement.DecorationElement(BookTextBlock.Kind.BODY, Component.literal(rowLayout.dotsText()), rowLayout.dotsX(), rowLayout.labelY(), Math.max(1, Minecraft.getInstance().font.width(rowLayout.dotsText())), rowLayout.rowHeight()));
                elements.add(new BookPageElement.DecorationElement(BookTextBlock.Kind.BODY, Component.literal(rowLayout.valueText()), rowLayout.valueX(), rowLayout.labelY(), Math.max(1, Minecraft.getInstance().font.width(rowLayout.valueText())), rowLayout.rowHeight()));
            }
            pages.add(BookPage.of(List.of(), elements));
        }
        return List.copyOf(pages);
    }

    private static List<BookPage> buildSkillPages(JournalSkillEntry skill) {
        String masteryRank = "Mastery " + skill.masteryLevel();
        String masteryBar = formatProgressBar(skill.masteryProgress(), skill.masteryRequiredForNextLevel());
        String masteryText = skill.masteryRequiredForNextLevel() > 0L
                ? "Toward next mastery: " + skill.masteryProgressText()
                : "Mastery recorded: " + skill.masteryProgressText();

        List<BookPageElement> elements = new ArrayList<>();
        int cursorY = PageCanvasRenderer.PAGE_MARGIN_Y;
        elements.add(skillTitleLink(skill.name()));
        cursorY += lineHeightFor(BookTextBlock.Kind.TITLE) + PageCanvasRenderer.BLOCK_SPACING;
        cursorY = appendTextElements(elements, BookTextBlock.Kind.LEVEL, String.valueOf(skill.investedSkillLevel()), PageCanvasRenderer.PAGE_MARGIN_X, cursorY, PAGE_CONTENT_WIDTH, false, null, null);
        cursorY += PageCanvasRenderer.BLOCK_SPACING;
        cursorY = appendTextElements(elements, BookTextBlock.Kind.SUBTITLE, masteryRank, PageCanvasRenderer.PAGE_MARGIN_X, cursorY, PAGE_CONTENT_WIDTH, false, null, null);
        cursorY += PageCanvasRenderer.BLOCK_SPACING;
        cursorY = appendTextElements(elements, BookTextBlock.Kind.BODY, compactSkillDescription(skill.roleSummary()), PageCanvasRenderer.PAGE_MARGIN_X, cursorY, PAGE_CONTENT_WIDTH, false, null, null);
        cursorY += PageCanvasRenderer.BLOCK_SPACING;
        cursorY = appendTextElements(elements, BookTextBlock.Kind.BODY, masteryBar, PageCanvasRenderer.PAGE_MARGIN_X, cursorY, PAGE_CONTENT_WIDTH, false, null, null);
        cursorY += PageCanvasRenderer.BLOCK_SPACING;
        cursorY = appendTextElements(elements, BookTextBlock.Kind.BODY, masteryText, PageCanvasRenderer.PAGE_MARGIN_X, cursorY, PAGE_CONTENT_WIDTH, false, null, null);
        cursorY += PageCanvasRenderer.BLOCK_SPACING;
        cursorY = appendTextElements(elements, BookTextBlock.Kind.BODY, compactStandingGifts(skill.passiveSummary()), PageCanvasRenderer.PAGE_MARGIN_X, cursorY, PAGE_CONTENT_WIDTH, false, null, null);
        cursorY += PageCanvasRenderer.BLOCK_SPACING;
        appendTextElements(
                elements,
                BookTextBlock.Kind.BODY,
                skill.canSpendSkillPoint() ? READY_MARK : compactMasteryIntent(skill.pathForward()),
                PageCanvasRenderer.PAGE_MARGIN_X,
                cursorY,
                PAGE_CONTENT_WIDTH,
                false,
                null,
                null
        );
        return List.of(BookPage.of(List.of(), elements));
    }

    private static BookPageElement.InteractiveTextElement skillTitleLink(String skillName) {
        return toInteractiveTextElement(pageTextLayout(
                BookTextBlock.Kind.TITLE,
                skillName,
                PageCanvasRenderer.PAGE_MARGIN_X,
                PageCanvasRenderer.PAGE_MARGIN_Y,
                PAGE_CONTENT_WIDTH,
                Component.literal("Open " + skillName),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.openSkillProjection(bookContext, skillName)
        ));
    }

    private static DocumentLayout pairPagesIntoSpreads(
            List<BookPage> pages,
            Map<Integer, List<BookInteractiveRegion>> pageRegions,
            List<String> projectionFocusOrder,
            Map<String, Integer> projectionSpreadByFocus,
            Map<String, Integer> projectionPageByFocus
    ) {
        List<BookSpread> spreads = new ArrayList<>();
        Map<Integer, List<BookInteractiveRegion>> spreadRegions = new LinkedHashMap<>();

        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex += 2) {
            BookPage left = pages.get(pageIndex);
            BookPage right = pageIndex + 1 < pages.size() ? pages.get(pageIndex + 1) : BookPage.empty();
            int spreadIndex = spreads.size();
            spreads.add(BookSpread.of(left, right));

            List<BookInteractiveRegion> regions = new ArrayList<>();
            List<BookInteractiveRegion> leftRegions = pageRegions.get(pageIndex);
            if (leftRegions != null) {
                for (BookInteractiveRegion region : leftRegions) {
                    regions.add(region.pageSide() == BookPageSide.LEFT
                            ? region
                            : BookInteractiveRegion.of(BookPageSide.LEFT, region.x(), region.y(), region.width(), region.height(), region.tooltip(), region.visibleLabel(), region.interactiveText(), region.action()));
                }
            }
            List<BookInteractiveRegion> rightRegions = pageRegions.get(pageIndex + 1);
            if (rightRegions != null) {
                for (BookInteractiveRegion region : rightRegions) {
                    regions.add(region.pageSide() == BookPageSide.RIGHT
                            ? region
                            : BookInteractiveRegion.of(BookPageSide.RIGHT, region.x(), region.y(), region.width(), region.height(), region.tooltip(), region.visibleLabel(), region.interactiveText(), region.action()));
                }
            }
            if (!regions.isEmpty()) {
                spreadRegions.put(spreadIndex, List.copyOf(regions));
            }

        }

        if (spreads.isEmpty()) {
            spreads.add(BookSpread.of(BookPage.empty(), BookPage.empty()));
        }
        return new DocumentLayout(
                List.copyOf(spreads),
                Map.copyOf(spreadRegions),
                List.copyOf(projectionFocusOrder),
                Map.copyOf(projectionSpreadByFocus),
                Map.copyOf(projectionPageByFocus)
        );
    }

    private static String adjacentProjectionFocus(List<String> focusOrder, String currentFocusId, int direction) {
        if (currentFocusId == null || currentFocusId.isBlank() || focusOrder.isEmpty()) {
            return null;
        }
        int currentIndex = focusOrder.indexOf(currentFocusId);
        if (currentIndex < 0) {
            return null;
        }
        int nextIndex = Math.floorMod(currentIndex + direction, focusOrder.size());
        return focusOrder.get(nextIndex);
    }

    private static List<BookPage> paginateBlocksIntoPages(List<BookTextBlock> blocks) {
        return paginateBlocksIntoPages(blocks, null);
    }

    private static List<BookPage> paginateBlocksIntoPages(List<BookTextBlock> blocks, String debugLabel) {
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
                    minimumGroupHeight += PageCanvasRenderer.BLOCK_SPACING + lineHeightFor(BookTextBlock.Kind.BODY);
                }

                int requiredHeight = minimumGroupHeight + (currentPageBlocks.isEmpty() ? 0 : PageCanvasRenderer.BLOCK_SPACING);
                if (!currentPageBlocks.isEmpty() && usedHeight + requiredHeight > PAGE_CONTENT_HEIGHT) {
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
                    int blockHeight = heightFor(header.kind(), header.lines().size());
                    if (!currentPageBlocks.isEmpty() && usedHeight + spacingBefore + blockHeight > PAGE_CONTENT_HEIGHT) {
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
                int availableHeight = PAGE_CONTENT_HEIGHT - usedHeight - spacingBefore;
                int lineCapacity = availableHeight / lineHeightFor(BookTextBlock.Kind.BODY);

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
                usedHeight += spacingBefore + (take * lineHeightFor(BookTextBlock.Kind.BODY));
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

    private static int chooseBodySplit(List<MeasuredLine> lines, int startIndex, int lineCapacity) {
        int remaining = lines.size() - startIndex;
        if (remaining <= lineCapacity) {
            return remaining;
        }

        int maxEndExclusive = startIndex + lineCapacity;
        int preferredStart = Math.max(startIndex, maxEndExclusive - BREAK_SEARCH_WINDOW);

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

    private static boolean startsMajorHeaderRun(
            List<MeasuredBlock> measuredBlocks,
            int startInclusive,
            int endExclusive,
            List<BookTextBlock> currentPageBlocks
    ) {
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

    private static List<MeasuredBlock> measureBlocks(List<BookTextBlock> blocks) {
        List<MeasuredBlock> measuredBlocks = new ArrayList<>(blocks.size());
        for (BookTextBlock block : blocks) {
            measuredBlocks.add(new MeasuredBlock(block.kind(), wrapBlock(block)));
        }
        return measuredBlocks;
    }

    private static List<MeasuredLine> wrapBlock(BookTextBlock block) {
        return switch (block.kind()) {
            case BODY -> wrapBodyText(block.text().getString(), PAGE_CONTENT_WIDTH);
            case TITLE, SUBTITLE, LEVEL, SECTION -> wrapStaticText(block.text().getString(), PAGE_CONTENT_WIDTH);
        };
    }

    private static List<MeasuredLine> wrapStaticText(String text, int width) {
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

    private static List<MeasuredLine> wrapBodyText(String text, int width) {
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
            height += heightFor(block.kind(), block.lines().size());
            first = false;
        }
        return height;
    }

    private static int heightFor(BookTextBlock.Kind kind, int lineCount) {
        return lineCount * lineHeightFor(kind);
    }

    private static PageInteractiveTextLayout pageTextLayout(
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int contentY,
            int contentWidth,
            Component tooltip,
            net.zoogle.enchiridion.api.BookRegionAction action
    ) {
        int drawX = alignedTextX(text, contentX, contentWidth, kind);
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(text));
        int textHeight = lineHeightFor(kind);
        return new PageInteractiveTextLayout(kind, text, drawX, contentY, textWidth, textHeight, tooltip, action);
    }

    private static int appendTextElements(
            List<BookPageElement> elements,
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int startY,
            int contentWidth,
            boolean interactive,
            Component tooltip,
            net.zoogle.enchiridion.api.BookRegionAction action
    ) {
        List<MeasuredLine> lines = kind == BookTextBlock.Kind.BODY ? wrapBodyText(text, contentWidth) : wrapStaticText(text, contentWidth);
        int cursorY = startY;
        for (MeasuredLine line : lines) {
            if (line.text().isBlank()) {
                cursorY += lineHeightFor(kind);
                continue;
            }
            int drawX = alignedTextX(line.text(), contentX, contentWidth, kind);
            int textWidth = Math.max(1, Minecraft.getInstance().font.width(line.text()));
            if (interactive && action != null) {
                elements.add(toInteractiveTextElement(new PageInteractiveTextLayout(
                        kind,
                        line.text(),
                        drawX,
                        cursorY,
                        textWidth,
                        lineHeightFor(kind),
                        tooltip,
                        action
                )));
            } else {
                elements.add(new BookPageElement.TextElement(kind, Component.literal(line.text()), drawX, cursorY, textWidth, lineHeightFor(kind)));
            }
            cursorY += lineHeightFor(kind);
        }
        return cursorY;
    }

    private static int lineHeightFor(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> PageCanvasRenderer.TITLE_LINE_HEIGHT;
            case SUBTITLE -> PageCanvasRenderer.SUBTITLE_LINE_HEIGHT;
            case LEVEL -> PageCanvasRenderer.LEVEL_LINE_HEIGHT;
            case SECTION -> PageCanvasRenderer.SECTION_LINE_HEIGHT;
            case BODY -> PageCanvasRenderer.BODY_LINE_HEIGHT;
        } + PageCanvasRenderer.LINE_SPACING;
    }

    private static int alignedTextX(String text, int contentX, int contentWidth, BookTextBlock.Kind kind) {
        if (kind == BookTextBlock.Kind.TITLE) {
            int textWidth = Minecraft.getInstance().font.width(text);
            return contentX + Math.max(0, (contentWidth - textWidth) / 2);
        }
        return contentX;
    }

    private static boolean isBulletParagraph(String paragraph) {
        String trimmed = paragraph.stripLeading();
        return trimmed.startsWith("- ")
                || trimmed.startsWith("* ")
                || trimmed.startsWith("o ")
                || trimmed.matches("^\\d+[.)].*");
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
            LOGGER.info(
                    "[Enchiridion] {} block {} kind={} wrappedLines={} blockHeight={} text={}",
                    debugLabel,
                    blockIndex,
                    block.kind(),
                    block.lines().size(),
                    heightFor(block.kind(), block.lines().size()),
                    previewBlockText(block.lines())
            );
        }
    }

    private static void debugPageBreak(
            String debugLabel,
            int pageIndex,
            String reason,
            List<BookTextBlock> currentPageBlocks,
            int usedHeight,
            int lineStart,
            int lineEnd,
            int totalLines
    ) {
        if (!shouldDebugPagination(debugLabel)) {
            return;
        }
        int finalY = PageCanvasRenderer.PAGE_MARGIN_Y + usedHeight;
        LOGGER.info(
                "[Enchiridion] {} page {} reason={} blocks={} usedHeight={} finalY={} safeBottom={} lineRange={}..{} of {}",
                debugLabel,
                pageIndex,
                reason,
                currentPageBlocks.size(),
                usedHeight,
                finalY,
                PageCanvasRenderer.PAGE_MARGIN_Y + PAGE_CONTENT_HEIGHT,
                lineStart,
                lineEnd,
                totalLines
        );
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

    private static String formatIdentityRecord(String identitySummary) {
        String[] lines = identitySummary.split("\\R");
        String name = lines.length > 0 ? lines[0].trim() : "Unknown";
        String title = lines.length > 1 ? lines[1].trim() : "Warden of the unwritten path";
        String archetype = lines.length > 2 ? lines[2].trim() : "Archetype Unchosen";
        return "Name ........ " + name
                + "\nTitle ....... " + title
                + "\nArchetype ... " + archetype;
    }

    private static String compactLedgerNote(String ledgerNote, String allocationStatus) {
        List<String> lines = new ArrayList<>();
        appendFirstLines(lines, ledgerNote, 2);
        appendFirstLines(lines, allocationStatus, 1);
        return String.join("\n", lines);
    }

    private static void appendFirstLines(List<String> target, String text, int maxLines) {
        if (text == null || text.isBlank()) {
            return;
        }
        String[] lines = text.split("\\R");
        for (int index = 0; index < lines.length && index < maxLines; index++) {
            String line = lines[index].trim();
            if (!line.isEmpty()) {
                target.add(line);
            }
        }
    }

    private static String compactSkillDescription(String description) {
        if (description == null || description.isBlank()) {
            return "No field note is inscribed for this discipline.";
        }
        String trimmed = description.trim();
        int sentenceEnd = firstSentenceBoundary(trimmed);
        return sentenceEnd > 0 ? trimmed.substring(0, sentenceEnd).trim() : trimmed;
    }

    private static int firstSentenceBoundary(String text) {
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == '.' || c == '!' || c == '?') {
                return index + 1;
            }
        }
        return -1;
    }

    private static String formatProgressBar(long currentValue, long requiredValue) {
        if (requiredValue <= 0L) {
            return "[" + "|".repeat(XP_BAR_SEGMENTS) + "]";
        }
        float progress = Math.clamp((float) currentValue / (float) requiredValue, 0.0f, 1.0f);
        int filled = Math.clamp(Math.round(progress * XP_BAR_SEGMENTS), 0, XP_BAR_SEGMENTS);
        return "[" + "|".repeat(filled) + ".".repeat(Math.max(0, XP_BAR_SEGMENTS - filled)) + "]";
    }

    private static String compactStandingGifts(String passiveSummary) {
        if (passiveSummary == null || passiveSummary.isBlank()) {
            return "No standing gift is inscribed here.";
        }
        return firstLine(passiveSummary);
    }

    private static String compactMasteryIntent(String pathForward) {
        if (pathForward == null || pathForward.isBlank()) {
            return "Open this discipline to review its deeper path.";
        }
        return firstLine(pathForward);
    }

    private static String firstLine(String text) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return "";
    }

    private static int ledgerBodyStartY() {
        List<MeasuredLine> sectionLines = wrapStaticText("Stat Ledger", PAGE_CONTENT_WIDTH);
        return PageCanvasRenderer.PAGE_MARGIN_Y
                + heightFor(BookTextBlock.Kind.SECTION, sectionLines.size()) + PageCanvasRenderer.BLOCK_SPACING;
    }

    private static int ledgerRowsPerPage() {
        int usableBodyHeight = PAGE_CONTENT_HEIGHT - ledgerBodyStartY();
        int lineHeight = lineHeightFor(BookTextBlock.Kind.BODY);
        return Math.max(1, usableBodyHeight / lineHeight);
    }

    private static LedgerRowLayout ledgerRowLayout(JournalCharacterStat stat, int rowY, int targetPageIndex) {
        String label = stat.name();
        String valueText = String.valueOf(stat.value());
        int rowHeight = lineHeightFor(BookTextBlock.Kind.BODY);
        int labelHeight = Minecraft.getInstance().font.lineHeight;
        int labelY = rowY + Math.max(0, (rowHeight - labelHeight) / 2);
        int labelX = PageCanvasRenderer.PAGE_MARGIN_X;
        int labelWidth = Math.max(1, Minecraft.getInstance().font.width(label));
        int valueWidth = Math.max(1, Minecraft.getInstance().font.width(valueText));
        int valueX = PageCanvasRenderer.PAGE_MARGIN_X + PAGE_CONTENT_WIDTH - valueWidth;
        int dotsX = labelX + labelWidth + 6;
        int dotWidth = Math.max(0, valueX - dotsX - 4);
        int dotCount = Math.max(2, dotWidth / Math.max(1, Minecraft.getInstance().font.width(".")));
        return new LedgerRowLayout(
                label,
                labelX,
                labelY,
                labelWidth,
                labelHeight,
                dotsX,
                valueX,
                rowY,
                rowHeight,
                ".".repeat(dotCount),
                valueText,
                targetPageIndex
        );
    }

    private static BookPageElement.InteractiveTextElement ledgerLabel(LedgerRowLayout rowLayout) {
        return toInteractiveTextElement(new PageInteractiveTextLayout(
                BookTextBlock.Kind.BODY,
                rowLayout.labelText(),
                rowLayout.labelX(),
                rowLayout.labelY(),
                rowLayout.labelWidth(),
                rowLayout.labelHeight(),
                Component.literal("Open " + rowLayout.labelText() + " entry"),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.jumpToJournalPage(bookContext, rowLayout.targetPageIndex())
        ));
    }

    private static BookPageElement.InteractiveTextElement toInteractiveTextElement(PageInteractiveTextLayout layout) {
        return new BookPageElement.InteractiveTextElement(
                layout.kind(),
                Component.literal(layout.text()),
                layout.x(),
                layout.y(),
                layout.width(),
                layout.height(),
                layout.tooltip(),
                layout.action()
        );
    }

    private static String formatUnlocks(List<JournalUnlockEntry> unlocks, String fallback) {
        if (unlocks.isEmpty()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < unlocks.size(); i++) {
            JournalUnlockEntry unlock = unlocks.get(i);
            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append("* ")
                    .append(unlock.title())
                    .append('\n')
                    .append(unlock.description());
        }
        return builder.toString();
    }

    private static String formatMilestones(List<JournalMilestoneEntry> milestones, String fallback) {
        if (milestones.isEmpty()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < milestones.size(); i++) {
            JournalMilestoneEntry milestone = milestones.get(i);
            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append("* ")
                    .append(milestone.title())
                    .append('\n')
                    .append(milestone.description());
        }
        return builder.toString();
    }

    private static String formatCharacterPassives(List<JournalCharacterStat> stats) {
        StringBuilder builder = new StringBuilder();
        for (JournalCharacterStat stat : stats) {
            if (stat.passiveSummary().isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("* ")
                    .append(stat.name())
                    .append('\n')
                    .append(stat.passiveSummary());
        }
        return builder.toString();
    }

    private record DocumentLayout(
            List<BookSpread> spreads,
            Map<Integer, List<BookInteractiveRegion>> interactiveRegionsBySpread,
            List<String> projectionFocusOrder,
            Map<String, Integer> projectionSpreadByFocus,
            Map<String, Integer> projectionPageByFocus
    ) {}

    private record NamedSectionLayout(String label, List<BookPage> pages) {}

    private record LedgerRowLayout(
            String labelText,
            int labelX,
            int labelY,
            int labelWidth,
            int labelHeight,
            int dotsX,
            int valueX,
            int rowY,
            int rowHeight,
            String dotsText,
            String valueText,
            int targetPageIndex
    ) {}

    private record PageInteractiveTextLayout(
            BookTextBlock.Kind kind,
            String text,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            net.zoogle.enchiridion.api.BookRegionAction action
    ) {}

    private record MeasuredBlock(BookTextBlock.Kind kind, List<MeasuredLine> lines) {}

    private record MeasuredLine(String text, BreakPriority breakPriority) {}

    private enum BreakPriority {
        NONE(0),
        COMMA_BREAK(1),
        CLAUSE_BREAK(2),
        SENTENCE_END(3),
        BULLET_BOUNDARY(4),
        PARAGRAPH_BOUNDARY(5),
        BLANK_LINE(6);

        private final int weight;

        BreakPriority(int weight) {
            this.weight = weight;
        }

        int weight() {
            return weight;
        }
    }
}
