package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class LevelRpgJournalComposer {
    static final String READY_MARK = "Skill Point Ready";
    static final int UNBOUND_INITIATION_SPREAD_INDEX = 0;
    static final int UNBOUND_INITIAL_SPREAD_INDEX = 0;

    private LevelRpgJournalComposer() {}

    static List<BookPage> buildOpeningPages(BookContext context) {
        return List.of(
                BookPage.empty(),
                BookPage.empty()
        );
    }

    static List<BookPage> buildUnboundPages(BookContext context) {
        return List.of(
                BookPage.empty(),
                BookPage.empty()
        );
    }

    static Map<Integer, List<BookInteractiveRegion>> buildUnboundPageRegions(BookContext context) {
        return Map.of();
    }

    static List<BookPage> buildIdentityPages(BookContext context, JournalCharacterSheet characterSheet, int firstLedgerPageIndex) {
        return List.of(
                buildCharacterIdentityPage(characterSheet, firstLedgerPageIndex - 2, JournalPageIds.characterIdentity()),
                buildStandingPage(context, characterSheet, firstLedgerPageIndex, firstLedgerPageIndex - 1, JournalPageIds.characterStanding())
        );
    }

    static int ledgerPageCount(List<JournalCharacterStat> stats) {
        return Math.max(1, (int) Math.ceil(stats.size() / (double) JournalPageStyleSystem.ledgerRowsPerPage(BookPageSide.LEFT)));
    }

    static List<BookPage> buildLedgerPages(
            List<JournalCharacterStat> stats,
            Map<String, Integer> skillStartPages,
            int firstLedgerPageIndex,
            List<JournalPageId> pageIds
    ) {
        JournalContentStore content = JournalContentStore.instance();
        int rowsPerPage = JournalPageStyleSystem.ledgerRowsPerPage(BookPageSide.LEFT);
        int pageCount = Math.max(1, (int) Math.ceil(stats.size() / (double) rowsPerPage));
        List<BookPage> pages = new ArrayList<>(pageCount);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int absolutePageIndex = firstLedgerPageIndex + pageIndex;
            JournalContentStore.PageContentView pageContent = content.page(pageIds.get(pageIndex), absolutePageIndex, JournalPagePurpose.LEDGER);
            BookPageSide pageSide = Math.floorMod(absolutePageIndex, 2) == 0 ? BookPageSide.LEFT : BookPageSide.RIGHT;
            JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.LEDGER, pageSide);
            page.addTitle(pageContent.text(JournalPageSlot.TITLE, pageIndex == 0 ? "Stat Ledger" : "Ledger"));
            int rowStart = pageIndex * rowsPerPage;
            int rowEnd = Math.min(stats.size(), rowStart + rowsPerPage);
            List<JournalCharacterStat> defaultRows = stats.subList(rowStart, rowEnd);
            // Ledger row values must follow the live snapshot; JournalContentStore ROWS overrides
            // persist old "Skill|0" lines and ignore /levelrpg sync.
            page.addLedgerRows(
                    JournalPageSlot.ROWS,
                    ledgerRowsForPage("", defaultRows),
                    skillStartPages
            );
            if (pageCount > 1) {
                page.addFooter(pageContent.text(JournalPageSlot.FOOTER, "Folio " + (pageIndex + 1) + " of " + pageCount));
            }
            pages.add(page.build());
        }
        return List.copyOf(pages);
    }

    static List<BookPage> buildSkillPages(JournalSkillEntry skill, int pageIndex, JournalPageId pageId) {
        return List.of(SkillPageTemplate.build(skill, pageIndex, pageId));
    }

    static BookPage buildBoundBackCoverPage() {
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.FRONT_MATTER, BookPageSide.LEFT);
        page.addTitle("Finis");
        page.addBody("What practice has built, this record holds.");
        return page.build();
    }

    private static BookPage buildCharacterIdentityPage(JournalCharacterSheet characterSheet, int pageIndex, JournalPageId pageId) {
        JournalContentStore content = JournalContentStore.instance();
        JournalContentStore.PageContentView pageContent = content.page(pageId, pageIndex, JournalPagePurpose.CHARACTER_IDENTITY);
        IdentityRecord record = parseIdentityRecord(characterSheet.identitySummary());
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.CHARACTER_IDENTITY, BookPageSide.LEFT);
        page.addTitle(pageContent.text(JournalPageSlot.TITLE, "Character Record"));
        page.addFocal(pageContent.text(JournalPageSlot.FOCAL, record.name()));
        page.addSubtitle(pageContent.text(JournalPageSlot.SUBTITLE, JournalPageStyleSystem.distinctLine(identitySubtitle(record), record.archetype(), record.name())));
        page.addBody(pageContent.text(JournalPageSlot.BODY, identityProse(record)));
        return page.build();
    }

    private static BookPage buildStandingPage(BookContext context, JournalCharacterSheet characterSheet, int firstLedgerPageIndex, int pageIndex, JournalPageId pageId) {
        JournalContentStore content = JournalContentStore.instance();
        JournalContentStore.PageContentView pageContent = content.page(pageId, pageIndex, JournalPagePurpose.CHARACTER_STANDING);
        StandingRecord standing = standingRecord(characterSheet);
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.CHARACTER_STANDING, BookPageSide.RIGHT);
        page.addTitle(pageContent.text(JournalPageSlot.TITLE, "Standing"));
        page.addBody(pageContent.text(JournalPageSlot.BODY, standingProse(standing)));
        page.addRadarChart(
                JournalPageSlot.RADAR,
                radarValues(characterSheet.stats()),
                radarMasteryLevelValues(characterSheet.stats()),
                radarNextMasteryRingValues(characterSheet.stats()),
                radarLabels(characterSheet.stats()));
        page.addInteraction(
                "standing:view-disciplines",
                pageContent.text(JournalPageSlot.INTERACTION, "View Disciplines"),
                Component.literal("Open the Stat Ledger"),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.jumpToJournalPage(bookContext, firstLedgerPageIndex)
        );
        return page.build();
    }

    private static java.util.List<Float> radarValues(java.util.List<JournalCharacterStat> stats) {
        return stats.stream()
                .map(stat -> JournalLayoutMetrics.radarMainVertexRadius(stat.value()))
                .toList();
    }

    private static java.util.List<Float> radarMasteryLevelValues(java.util.List<JournalCharacterStat> stats) {
        return stats.stream().map(JournalCharacterStat::masteryLevelNorm).toList();
    }

    private static java.util.List<Float> radarNextMasteryRingValues(java.util.List<JournalCharacterStat> stats) {
        return stats.stream().map(JournalCharacterStat::masteryNextLevelRingNorm).toList();
    }

    private static java.util.List<String> radarLabels(java.util.List<JournalCharacterStat> stats) {
        return stats.stream().map(JournalCharacterStat::name).toList();
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

    private static String identityProse(IdentityRecord record) {
        boolean bound = !record.archetype().isBlank() && !record.archetype().contains("Unchosen");
        if (bound) {
            return "Practice tempers mastery; chosen disciplines are the shape it takes.";
        }
        return "An archetype is not yet sealed. Practice will reveal the shape of what you may become.";
    }

    private static String standingProse(StandingRecord standing) {
        if (standing.readyPoints() > 0) {
            return "A point of potential is ready. Turn to the disciplines to commit it.";
        }
        if (standing.masteryLevels() > 0) {
            return "Practice shapes this record. Another point waits in the disciplines ahead.";
        }
        return "Practice in the world will shape what you may claim here.";
    }

    private static StandingRecord standingRecord(JournalCharacterSheet characterSheet) {
        String ledgerNote = characterSheet.ledgerNote();
        int masteryLevels = extractCountBetween(ledgerNote, "Mastery:", "levels earned");
        int readyPoints = extractCountBetween(ledgerNote, "Skill Points:", "ready");
        int spentPoints = extractCountBetween(ledgerNote, "|", "spent");
        return new StandingRecord(masteryLevels, readyPoints, spentPoints);
    }

    private static String nonBlankOr(String text, String fallback) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
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
                    fallback != null ? fallback.masteryValue() : 0,
                    fallback != null ? fallback.masteryLevelNorm() : 0f,
                    fallback != null ? fallback.masteryNextLevelRingNorm() : 0f,
                    fallback != null && fallback.canAllocate(),
                    fallback != null ? fallback.passiveSummary() : ""
            ));
        }
        return rows.isEmpty() ? defaults : rows;
    }

    private record IdentityRecord(String name, String title, String archetype, String summary) {}
    private record StandingRecord(int masteryLevels, int readyPoints, int spentPoints) {}
}
