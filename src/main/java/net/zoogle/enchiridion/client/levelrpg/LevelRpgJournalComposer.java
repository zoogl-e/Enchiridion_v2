package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class LevelRpgJournalComposer {
    static final String READY_MARK = "Skill Point Ready";
    private static final int BOX_FILL = 0x1CF0E0BC;
    private static final int BOX_BORDER = 0x7A8B7148;
    private static final int EMPHASIS_FILL = 0x24E7D4AC;
    private static final int EMPHASIS_BORDER = 0xA08F7646;

    private LevelRpgJournalComposer() {}

    static List<BookPage> buildLegacyOpeningPages(BookContext context) {
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

    static List<BookPage> buildIdentityPages(JournalCharacterSheet characterSheet) {
        return List.of(buildCharacterRecordPage(characterSheet));
    }

    static int ledgerPageCount(List<JournalCharacterStat> stats) {
        return Math.max(1, (int) Math.ceil(stats.size() / (double) ledgerRowsPerPage()));
    }

    static List<BookPage> buildLedgerPages(List<JournalCharacterStat> stats, Map<String, Integer> skillStartPages, int firstLedgerPageIndex) {
        int pageCount = ledgerPageCount(stats);
        List<BookPage> pages = new ArrayList<>(pageCount);
        int rowsPerPage = ledgerRowsPerPage();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            List<net.zoogle.enchiridion.api.BookPageElement> elements = new ArrayList<>();
            int absolutePageIndex = firstLedgerPageIndex + pageIndex;
            BookPageSide pageSide = Math.floorMod(absolutePageIndex, 2) == 0 ? BookPageSide.LEFT : BookPageSide.RIGHT;
            JournalFlowLayout layout = new JournalFlowLayout(elements, pageSide);
            if (pageIndex == 0) {
                layout.addText(BookTextBlock.Kind.SECTION, "Stat Ledger");
                layout.addSpacing(10);
            }
            int rowStart = pageIndex * rowsPerPage;
            int rowEnd = Math.min(stats.size(), rowStart + rowsPerPage);
            for (int statIndex = rowStart; statIndex < rowEnd; statIndex++) {
                JournalCharacterStat stat = stats.get(statIndex);
                Integer targetPageIndex = skillStartPages.get(stat.name());
                if (targetPageIndex == null) {
                    continue;
                }
                layout.addLedgerRow(stat, targetPageIndex);
            }
            pages.add(BookPage.of(List.of(), elements));
        }
        return List.copyOf(pages);
    }

    static List<BookPage> buildSkillPages(JournalSkillEntry skill) {
        return List.of(SkillPageTemplate.build(skill));
    }

    private static BookPage buildCharacterRecordPage(JournalCharacterSheet characterSheet) {
        List<net.zoogle.enchiridion.api.BookPageElement> elements = new ArrayList<>();
        IdentityRecord record = parseIdentityRecord(characterSheet.identitySummary());
        JournalFlowLayout layout = new JournalFlowLayout(elements);
        layout.addCenteredText(BookTextBlock.Kind.TITLE, characterSheet.title());
        layout.addSpacing(8);

        layout.addPanel(
                "",
                List.of(
                        "Name  " + record.name(),
                        "Archetype  " + record.archetype()
                ),
                false,
                BOX_FILL,
                BOX_BORDER
        );
        layout.addSpacing(JournalFlowLayout.defaultSpacing());
        layout.addPanel(
                "",
                clampLines(firstNonBlank(characterSheet.ledgerNote(), record.summary()), JournalFlowLayout.contentWidth() - 12, 3),
                false,
                BOX_FILL,
                BOX_BORDER
        );
        layout.addSpacing(JournalFlowLayout.defaultSpacing());
        layout.addPanel(
                "Disposition",
                clampLines(characterSheet.allocationStatus(), JournalFlowLayout.contentWidth() - 12, 2),
                true,
                EMPHASIS_FILL,
                EMPHASIS_BORDER
        );

        return BookPage.of(List.of(), elements);
    }

    private static IdentityRecord parseIdentityRecord(String identitySummary) {
        String[] lines = identitySummary == null ? new String[0] : identitySummary.split("\\R");
        String name = lines.length > 0 ? nonBlankOr(lines[0], "Unknown") : "Unknown";
        String title = lines.length > 1 ? nonBlankOr(lines[1], "Warden of the unwritten path") : "Warden of the unwritten path";
        String archetype = lines.length > 2 ? nonBlankOr(lines[2], "Archetype Unchosen") : "Archetype Unchosen";
        String summary = lines.length > 3 ? nonBlankOr(String.join(" ", List.of(lines).subList(3, lines.length)), title) : title;
        return new IdentityRecord(name, title, archetype, summary);
    }

    private static List<String> clampLines(String text, int width, int maxLines) {
        List<JournalPaginationEngine.MeasuredLine> wrapped = JournalPaginationEngine.wrapBodyText(text, width);
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < wrapped.size() && lines.size() < maxLines; index++) {
            lines.add(wrapped.get(index).text());
        }
        if (wrapped.size() > maxLines && !lines.isEmpty()) {
            int lastIndex = lines.size() - 1;
            lines.set(lastIndex, withEllipsis(lines.get(lastIndex), width));
        }
        return lines;
    }

    private static String withEllipsis(String line, int width) {
        String base = line == null ? "" : line.trim();
        if (base.isEmpty()) {
            return "...";
        }
        String candidate = base + "...";
        while (!base.isEmpty() && Minecraft.getInstance().font.width(candidate) > width) {
            base = base.substring(0, base.length() - 1).trim();
            candidate = base + "...";
        }
        return candidate;
    }

    private static String firstNonBlank(String primary, String fallback) {
        String primaryTrimmed = primary == null ? "" : primary.trim();
        return !primaryTrimmed.isEmpty() ? primaryTrimmed : fallback;
    }

    private static String nonBlankOr(String text, String fallback) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static int ledgerRowsPerPage() {
        int headingHeight = JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.SECTION)
                + 2
                + JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.SUBTITLE)
                + 10;
        int usableBodyHeight = JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - headingHeight;
        int lineHeight = JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY);
        return Math.max(1, usableBodyHeight / lineHeight);
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

    private record IdentityRecord(String name, String title, String archetype, String summary) {}
}
