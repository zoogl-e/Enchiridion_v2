package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookSpread;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LevelRpgJournalDocumentBuilder {
    private LevelRpgJournalDocumentBuilder() {}

    static LevelRpgJournalDocument build(BookContext context) {
        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        List<BookPage> pages = new ArrayList<>();
        Map<Integer, List<BookInteractiveRegion>> pageRegions = new LinkedHashMap<>();
        Map<Integer, String> pagePurposes = new LinkedHashMap<>();

        List<BookPage> introPages = LevelRpgJournalComposer.buildLegacyOpeningPages(context);
        List<NamedSectionLayout> skillSections = new ArrayList<>();

        int characterPageStart = introPages.size();
        int firstLedgerPageIndex = characterPageStart + 2;
        List<BookPage> identityPages = LevelRpgJournalComposer.buildIdentityPages(context, snapshot.characterSheet(), firstLedgerPageIndex);
        Map<String, Integer> skillStartPages = new LinkedHashMap<>();
        int nextPageStart = characterPageStart
                + identityPages.size()
                + LevelRpgJournalComposer.ledgerPageCount(snapshot.characterSheet().stats());
        for (JournalSkillEntry skill : snapshot.skills()) {
            skillStartPages.put(skill.name(), nextPageStart);
            List<BookPage> pagesForSkill = LevelRpgJournalComposer.buildSkillPages(skill, nextPageStart);
            skillSections.add(new NamedSectionLayout(skill.name(), pagesForSkill));
            nextPageStart += pagesForSkill.size();
        }

        List<BookPage> ledgerPages = LevelRpgJournalComposer.buildLedgerPages(
                snapshot.characterSheet().stats(),
                skillStartPages,
                characterPageStart + identityPages.size()
        );

        pages.addAll(introPages);
        markPurposes(pagePurposes, 0, introPages.size(), JournalPagePurpose.FRONT_MATTER);
        pages.addAll(identityPages);
        pagePurposes.put(characterPageStart, JournalPagePurpose.CHARACTER_IDENTITY.name());
        pagePurposes.put(characterPageStart + 1, JournalPagePurpose.CHARACTER_STANDING.name());
        pages.addAll(ledgerPages);
        markPurposes(pagePurposes, characterPageStart + identityPages.size(), ledgerPages.size(), JournalPagePurpose.LEDGER);
        for (NamedSectionLayout skillSection : skillSections) {
            int startIndex = pages.size();
            pages.addAll(skillSection.pages());
            markPurposes(pagePurposes, startIndex, skillSection.pages().size(), JournalPagePurpose.SKILL_DETAIL);
        }

        List<String> projectionFocusOrder = snapshot.skills().stream().map(JournalSkillEntry::name).toList();
        Map<String, Integer> projectionSpreadByFocus = new LinkedHashMap<>();
        Map<String, Integer> projectionPageByFocus = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : skillStartPages.entrySet()) {
            projectionSpreadByFocus.put(entry.getKey(), entry.getValue() / 2);
            projectionPageByFocus.put(entry.getKey(), entry.getValue());
        }

        return pairPagesIntoSpreads(pages, pageRegions, pagePurposes, projectionFocusOrder, projectionSpreadByFocus, projectionPageByFocus);
    }

    private static LevelRpgJournalDocument pairPagesIntoSpreads(
            List<BookPage> pages,
            Map<Integer, List<BookInteractiveRegion>> pageRegions,
            Map<Integer, String> pagePurposes,
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
                regions.addAll(leftRegions);
            }
            List<BookInteractiveRegion> rightRegions = pageRegions.get(pageIndex + 1);
            if (rightRegions != null) {
                regions.addAll(rightRegions);
            }
            if (!regions.isEmpty()) {
                spreadRegions.put(spreadIndex, List.copyOf(regions));
            }
        }

        if (spreads.isEmpty()) {
            spreads.add(BookSpread.of(BookPage.empty(), BookPage.empty()));
        }
        return new LevelRpgJournalDocument(
                List.copyOf(spreads),
                Map.copyOf(spreadRegions),
                Map.copyOf(pagePurposes),
                List.copyOf(projectionFocusOrder),
                Map.copyOf(projectionSpreadByFocus),
                Map.copyOf(projectionPageByFocus)
        );
    }

    private static void markPurposes(Map<Integer, String> pagePurposes, int startIndex, int count, JournalPagePurpose purpose) {
        for (int index = 0; index < count; index++) {
            pagePurposes.put(startIndex + index, purpose.name());
        }
    }

    private record NamedSectionLayout(String label, List<BookPage> pages) {}
}
