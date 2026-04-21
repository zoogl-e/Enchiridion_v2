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

        List<BookPage> introPages = LevelRpgJournalBookProvider.buildLegacyOpeningPages(context);
        List<NamedSectionLayout> skillSections = new ArrayList<>();
        for (JournalSkillEntry skill : snapshot.skills()) {
            skillSections.add(new NamedSectionLayout(skill.name(), LevelRpgJournalBookProvider.buildSkillPages(skill)));
        }

        List<BookPage> identityPages = LevelRpgJournalBookProvider.buildIdentityPages(snapshot.characterSheet());
        int characterPageStart = introPages.size();
        Map<String, Integer> skillStartPages = new LinkedHashMap<>();
        int nextPageStart = characterPageStart
                + identityPages.size()
                + LevelRpgJournalBookProvider.ledgerPageCount(snapshot.characterSheet().stats());
        for (NamedSectionLayout skillSection : skillSections) {
            skillStartPages.put(skillSection.label(), nextPageStart);
            nextPageStart += skillSection.pages().size();
        }

        List<BookPage> ledgerPages = LevelRpgJournalBookProvider.buildLedgerPages(snapshot.characterSheet().stats(), skillStartPages);

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

    private static LevelRpgJournalDocument pairPagesIntoSpreads(
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
                List.copyOf(projectionFocusOrder),
                Map.copyOf(projectionSpreadByFocus),
                Map.copyOf(projectionPageByFocus)
        );
    }

    private record NamedSectionLayout(String label, List<BookPage> pages) {}
}
