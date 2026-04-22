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
    private static final int STANDING_BLOCK_LINE_GAP = 5;

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

    static List<BookPage> buildIdentityPages(BookContext context, JournalCharacterSheet characterSheet, int firstLedgerPageIndex) {
        return List.of(
                buildCharacterIdentityPage(characterSheet, firstLedgerPageIndex - 2),
                buildStandingPage(context, characterSheet, firstLedgerPageIndex, firstLedgerPageIndex - 1)
        );
    }

    static int ledgerPageCount(List<JournalCharacterStat> stats) {
        return Math.max(1, (int) Math.ceil(stats.size() / (double) JournalPageStyleSystem.ledgerRowsPerPage(BookPageSide.LEFT)));
    }

    static List<BookPage> buildLedgerPages(List<JournalCharacterStat> stats, Map<String, Integer> skillStartPages, int firstLedgerPageIndex) {
        JournalContentStore content = JournalContentStore.instance();
        int rowsPerPage = JournalPageStyleSystem.ledgerRowsPerPage(BookPageSide.LEFT);
        int pageCount = Math.max(1, (int) Math.ceil(stats.size() / (double) rowsPerPage));
        List<BookPage> pages = new ArrayList<>(pageCount);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int absolutePageIndex = firstLedgerPageIndex + pageIndex;
            BookPageSide pageSide = Math.floorMod(absolutePageIndex, 2) == 0 ? BookPageSide.LEFT : BookPageSide.RIGHT;
            JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.LEDGER, pageSide);
            page.addTitle(content.text(absolutePageIndex, JournalPageSlot.TITLE, pageIndex == 0 ? "Stat Ledger" : "Ledger"));
            int rowStart = pageIndex * rowsPerPage;
            int rowEnd = Math.min(stats.size(), rowStart + rowsPerPage);
            List<JournalCharacterStat> defaultRows = stats.subList(rowStart, rowEnd);
            page.addLedgerRows(
                    JournalPageSlot.ROWS,
                    ledgerRowsForPage(content.text(absolutePageIndex, JournalPageSlot.ROWS, defaultLedgerRows(defaultRows)), defaultRows),
                    skillStartPages
            );
            page.addFooter(content.text(absolutePageIndex, JournalPageSlot.FOOTER, "Folio " + (pageIndex + 1) + " of " + pageCount));
            pages.add(page.build());
        }
        return List.copyOf(pages);
    }

    static List<BookPage> buildSkillPages(JournalSkillEntry skill, int pageIndex) {
        return List.of(SkillPageTemplate.build(skill, pageIndex));
    }

    private static BookPage buildCharacterIdentityPage(JournalCharacterSheet characterSheet, int pageIndex) {
        JournalContentStore content = JournalContentStore.instance();
        IdentityRecord record = parseIdentityRecord(characterSheet.identitySummary());
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.CHARACTER_IDENTITY, BookPageSide.LEFT);
        page.addTitle(content.text(pageIndex, JournalPageSlot.TITLE, "Character Record"));
        page.addFocal(content.text(pageIndex, JournalPageSlot.FOCAL, record.name()));
        page.addSubtitle(content.text(pageIndex, JournalPageSlot.SUBTITLE, JournalPageStyleSystem.distinctLine(identitySubtitle(record), record.archetype(), record.name())));
        page.addBody(content.text(pageIndex, JournalPageSlot.BODY, identityProse(record, characterSheet)));
        page.addFooter(content.text(pageIndex, JournalPageSlot.FOOTER, characterSheet.stats().size() + " disciplines recorded"));
        return page.build();
    }

    private static BookPage buildStandingPage(BookContext context, JournalCharacterSheet characterSheet, int firstLedgerPageIndex, int pageIndex) {
        JournalContentStore content = JournalContentStore.instance();
        StandingRecord standing = standingRecord(context, characterSheet);
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.CHARACTER_STANDING, BookPageSide.RIGHT);
        page.addTitle(content.text(pageIndex, JournalPageSlot.TITLE, "Standing"));
        page.addFocal(content.text(pageIndex, JournalPageSlot.FOCAL, "Level " + standing.level()));
        page.addStats(contentLines(content.text(pageIndex, JournalPageSlot.STATS, String.join("\n", standingLines(standing)))), STANDING_BLOCK_LINE_GAP);
        page.addInteraction(
                "standing:view-disciplines",
                content.text(pageIndex, JournalPageSlot.INTERACTION, "View Disciplines"),
                Component.literal("Open the Stat Ledger"),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.jumpToJournalPage(bookContext, firstLedgerPageIndex)
        );
        return page.build();
    }

    private static IdentityRecord parseIdentityRecord(String identitySummary) {
        String[] lines = identitySummary == null ? new String[0] : identitySummary.split("\\R");
        String name = lines.length > 0 ? nonBlankOr(lines[0], "Unknown") : "Unknown";
        String title = lines.length > 1 ? nonBlankOr(lines[1], "Warden of the unwritten path") : "Warden of the unwritten path";
        String archetype = lines.length > 2 ? nonBlankOr(lines[2], "Archetype Unchosen") : "Archetype Unchosen";
        String summary = lines.length > 3 ? nonBlankOr(String.join(" ", List.of(lines).subList(3, lines.length)), title) : title;
        return new IdentityRecord(name, title, archetype, summary);
    }

    private static String identitySubtitle(IdentityRecord record) {
        String subtitle = JournalPageStyleSystem.distinctLine(record.title(), record.archetype(), record.name());
        subtitle = subtitle.replace("Archetype ", "");
        subtitle = subtitle.replace("'s", "s");
        return subtitle;
    }

    private static String identityProse(IdentityRecord record, JournalCharacterSheet characterSheet) {
        String summary = firstNonBlank(record.summary(), characterSheet.allocationStatus());
        String subtitle = identitySubtitle(record);
        String prose = summary.trim();
        if (!subtitle.isBlank()) {
            String lowerSubtitle = subtitle.toLowerCase();
            String lowerProse = prose.toLowerCase();
            if (lowerProse.contains(lowerSubtitle)) {
                prose = characterSheet.allocationStatus();
            }
        }
        prose = prose
                .replace("Skill Levels are chosen by spending the points that mastery grants.", "Mastery earned through practice is later shaped into chosen disciplines.")
                .replace("Skill Levels are chosen with the points that mastery grants.", "Mastery earned through practice is later shaped into chosen disciplines.")
                .replace("Your archetype is not yet sealed.", "Their archetype has not yet been sealed.")
                .replace("Mastery is earned through practice;", "Practice tempers mastery, and")
                .replace("Mastery is earned through practice.", "Practice tempers mastery.")
                .replace("skill points", "unspent potential")
                .trim();
        return JournalPageStyleSystem.polishedSentence(prose);
    }

    private static StandingRecord standingRecord(BookContext context, JournalCharacterSheet characterSheet) {
        int level = context != null && context.player() != null ? context.player().experienceLevel : 0;
        int totalExperience = context != null && context.player() != null ? context.player().totalExperience : 0;
        int nextThreshold = context != null && context.player() != null ? context.player().getXpNeededForNextLevel() : 0;

        String ledgerNote = characterSheet.ledgerNote();
        int masteryLevels = extractCountBetween(ledgerNote, "Mastery:", "levels earned");
        int readyPoints = extractCountBetween(ledgerNote, "Skill Points:", "ready");
        int spentPoints = extractCountBetween(ledgerNote, "|", "spent");
        int earnedPoints = extractTrailingCount(ledgerNote, "earned");

        return new StandingRecord(level, totalExperience, nextThreshold, masteryLevels, readyPoints, spentPoints, earnedPoints);
    }

    private static List<String> standingLines(StandingRecord standing) {
        return List.of(
                "Total Experience  " + standing.totalExperience(),
                "Next Threshold  " + standing.nextThreshold() + " xp",
                "Mastery Points  " + standing.earnedPoints(),
                "Unspent Points  " + standing.readyPoints()
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        String primaryTrimmed = primary == null ? "" : primary.trim();
        return !primaryTrimmed.isEmpty() ? primaryTrimmed : fallback;
    }

    private static String nonBlankOr(String text, String fallback) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
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

    private static int extractCountBetween(String text, String startToken, String endToken) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int start = text.indexOf(startToken);
        if (start < 0) {
            return 0;
        }
        start += startToken.length();
        int end = text.indexOf(endToken, start);
        String slice = end >= 0 ? text.substring(start, end) : text.substring(start);
        return extractFirstInt(slice);
    }

    private static int extractTrailingCount(String text, String endToken) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int end = text.indexOf(endToken);
        if (end < 0) {
            return 0;
        }
        String slice = text.substring(0, end);
        int lastPipe = slice.lastIndexOf('|');
        return extractFirstInt(lastPipe >= 0 ? slice.substring(lastPipe + 1) : slice);
    }

    private static int extractFirstInt(String text) {
        if (text == null) {
            return 0;
        }
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (!digits.isEmpty()) {
                break;
            }
        }
        return digits.isEmpty() ? 0 : Integer.parseInt(digits.toString());
    }

    private static List<String> contentLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(content.split("\\R"));
    }

    private static String defaultLedgerRows(List<JournalCharacterStat> stats) {
        List<String> lines = new ArrayList<>();
        for (JournalCharacterStat stat : stats) {
            lines.add(stat.name() + "|" + stat.value());
        }
        return String.join("\n", lines);
    }

    private static List<JournalCharacterStat> ledgerRowsForPage(String content, List<JournalCharacterStat> defaults) {
        if (content == null || content.isBlank()) {
            return defaults;
        }
        List<JournalCharacterStat> rows = new ArrayList<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("\\|", 2);
            String name = parts[0].trim();
            int value = parts.length > 1 ? extractFirstInt(parts[1]) : 0;
            JournalCharacterStat fallback = defaults.stream().filter(stat -> stat.name().equalsIgnoreCase(name)).findFirst().orElse(null);
            rows.add(new JournalCharacterStat(
                    name,
                    value,
                    fallback != null && fallback.canAllocate(),
                    fallback != null ? fallback.passiveSummary() : ""
            ));
        }
        return rows.isEmpty() ? defaults : rows;
    }

    private record IdentityRecord(String name, String title, String archetype, String summary) {}
    private record StandingRecord(int level, int totalExperience, int nextThreshold, int masteryLevels, int readyPoints, int spentPoints, int earnedPoints) {}
}
