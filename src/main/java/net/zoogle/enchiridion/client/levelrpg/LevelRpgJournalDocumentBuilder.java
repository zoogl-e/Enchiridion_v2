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
        LevelRpgJournalIntroFlowState flow = LevelRpgJournalIntroFlowState.get();
        flow.syncToTruth(context);
        if (flow.isUnbound(context)) {
            return buildUnboundDocument(context);
        }

        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        List<LevelRpgJournalResolvedPage> pages = new ArrayList<>();

        List<NamedSectionLayout> skillSections = new ArrayList<>();

        int characterPageStart = 0;
        int firstLedgerPageIndex = characterPageStart + 2;
        List<BookPage> rawIdentityPages = LevelRpgJournalComposer.buildIdentityPages(context, snapshot.characterSheet(), firstLedgerPageIndex);
        List<LevelRpgJournalResolvedPage> identityPages = List.of(
                LevelRpgJournalResolvedPage.of(JournalPageIds.characterIdentity(), JournalPagePurpose.CHARACTER_IDENTITY, rawIdentityPages.get(0)),
                LevelRpgJournalResolvedPage.of(JournalPageIds.characterStanding(), JournalPagePurpose.CHARACTER_STANDING, rawIdentityPages.get(1))
        );
        Map<String, Integer> skillStartPages = new LinkedHashMap<>();
        int nextPageStart = characterPageStart
                + identityPages.size()
                + LevelRpgJournalComposer.ledgerPageCount(snapshot.characterSheet().stats());
        List<JournalPageId> ledgerPageIds = new ArrayList<>();
        for (int ledgerIndex = 0; ledgerIndex < LevelRpgJournalComposer.ledgerPageCount(snapshot.characterSheet().stats()); ledgerIndex++) {
            ledgerPageIds.add(JournalPageIds.ledger(ledgerIndex + 1));
        }
        for (JournalSkillEntry skill : snapshot.skills()) {
            skillStartPages.put(skill.name(), nextPageStart);
            JournalPageId pageId = JournalPageIds.skill(skill.skillKey());
            List<LevelRpgJournalResolvedPage> pagesForSkill = List.of(LevelRpgJournalResolvedPage.of(
                    pageId,
                    JournalPagePurpose.SKILL_DETAIL,
                    LevelRpgJournalComposer.buildSkillPages(skill, nextPageStart, pageId).getFirst()
            ));
            skillSections.add(new NamedSectionLayout(skill.name(), pagesForSkill));
            nextPageStart += pagesForSkill.size();
        }

        List<LevelRpgJournalResolvedPage> ledgerPages = resolvedLedgerPages(
                snapshot.characterSheet().stats(),
                skillStartPages,
                characterPageStart + identityPages.size(),
                ledgerPageIds
        );

        pages.addAll(identityPages);
        pages.addAll(ledgerPages);
        for (NamedSectionLayout skillSection : skillSections) {
            pages.addAll(skillSection.pages());
        }
        // Ensure the back cover pages start on an even page index so they always
        // form their own spread rather than splitting across two spreads.
        if (pages.size() % 2 != 0) {
            pages.add(LevelRpgJournalResolvedPage.of(
                    JournalPageIds.syntheticEmpty(pages.size()),
                    JournalPagePurpose.SYNTHETIC_EMPTY,
                    BookPage.empty()
            ));
        }
        pages.addAll(boundBackCoverPages());

        return pairPagesIntoSpreads(pages, List.of(), Map.of(), Map.of());
    }

    private static LevelRpgJournalDocument buildUnboundDocument(BookContext context) {
        List<BookPage> rawPages = new ArrayList<>(LevelRpgJournalComposer.buildUnboundPages(context));
        List<LevelRpgJournalResolvedPage> pages = new ArrayList<>();
        pages.addAll(List.of(
                LevelRpgJournalResolvedPage.of(
                        JournalPageIds.unboundInitiation(),
                        JournalPagePurpose.FRONT_MATTER,
                        rawPages.get(0)
                ),
                LevelRpgJournalResolvedPage.of(
                        JournalPageIds.unboundReading(),
                        JournalPagePurpose.FRONT_MATTER,
                        rawPages.get(1)
                )
        ));
        pages.addAll(unboundBackCoverPages());

        List<JournalArchetypeChoice> choices = LevelRpgArchetypeBindingBridge.availableArchetypes();
        List<String> projectionFocusOrder = choices.stream().map(JournalArchetypeChoice::focusId).toList();
        Map<String, Integer> projectionSpreadByFocus = new LinkedHashMap<>();
        Map<String, Integer> projectionPageByFocus = new LinkedHashMap<>();
        for (JournalArchetypeChoice choice : choices) {
            projectionSpreadByFocus.put(choice.focusId(), 0);
            projectionPageByFocus.put(choice.focusId(), 0);
        }

        return pairPagesIntoSpreads(
                pages,
                projectionFocusOrder,
                projectionSpreadByFocus,
                projectionPageByFocus
        );
    }

    private static LevelRpgJournalDocument pairPagesIntoSpreads(
            List<LevelRpgJournalResolvedPage> pages,
            List<String> projectionFocusOrder,
            Map<String, Integer> projectionSpreadByFocus,
            Map<String, Integer> projectionPageByFocus
    ) {
        List<BookSpread> spreads = new ArrayList<>();
        Map<Integer, List<BookInteractiveRegion>> spreadRegions = new LinkedHashMap<>();
        Map<Integer, String> pagePurposes = new LinkedHashMap<>();
        Map<Integer, String> pageIds = new LinkedHashMap<>();

        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex += 2) {
            LevelRpgJournalResolvedPage leftPage = pages.get(pageIndex);
            LevelRpgJournalResolvedPage rightPage = pageIndex + 1 < pages.size()
                    ? pages.get(pageIndex + 1)
                    : LevelRpgJournalResolvedPage.of(JournalPageIds.syntheticEmpty(pageIndex), JournalPagePurpose.SYNTHETIC_EMPTY, BookPage.empty());
            int spreadIndex = spreads.size();
            spreads.add(BookSpread.of(leftPage.page(), rightPage.page()));
            pagePurposes.put(pageIndex, leftPage.purpose().name());
            pagePurposes.put(pageIndex + 1, rightPage.purpose().name());
            pageIds.put(pageIndex, leftPage.id().value());
            pageIds.put(pageIndex + 1, rightPage.id().value());

            List<BookInteractiveRegion> regions = new ArrayList<>();
            regions.addAll(leftPage.interactiveRegions());
            regions.addAll(rightPage.interactiveRegions());
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
                Map.copyOf(pageIds),
                List.copyOf(projectionFocusOrder),
                Map.copyOf(projectionSpreadByFocus),
                Map.copyOf(projectionPageByFocus)
        );
    }

    private static List<LevelRpgJournalResolvedPage> boundBackCoverPages() {
        return List.of(
                LevelRpgJournalResolvedPage.of(JournalPageIds.backCoverLeft(), JournalPagePurpose.FRONT_MATTER, LevelRpgJournalComposer.buildBoundBackCoverPage()),
                LevelRpgJournalResolvedPage.of(JournalPageIds.backCoverRight(), JournalPagePurpose.FRONT_MATTER, BookPage.empty())
        );
    }

    private static List<LevelRpgJournalResolvedPage> unboundBackCoverPages() {
        return List.of(
                LevelRpgJournalResolvedPage.of(JournalPageIds.backCoverLeft(), JournalPagePurpose.FRONT_MATTER, BookPage.empty()),
                LevelRpgJournalResolvedPage.of(JournalPageIds.backCoverRight(), JournalPagePurpose.FRONT_MATTER, BookPage.empty())
        );
    }

    private static List<LevelRpgJournalResolvedPage> resolvedLedgerPages(
            List<JournalCharacterStat> stats,
            Map<String, Integer> skillStartPages,
            int firstLedgerPageIndex,
            List<JournalPageId> pageIds
    ) {
        List<BookPage> pages = LevelRpgJournalComposer.buildLedgerPages(stats, skillStartPages, firstLedgerPageIndex, pageIds);
        List<LevelRpgJournalResolvedPage> resolved = new ArrayList<>(pages.size());
        for (int index = 0; index < pages.size(); index++) {
            resolved.add(LevelRpgJournalResolvedPage.of(pageIds.get(index), JournalPagePurpose.LEDGER, pages.get(index)));
        }
        return List.copyOf(resolved);
    }

    private record NamedSectionLayout(String label, List<LevelRpgJournalResolvedPage> pages) {}
}
