package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;

import java.util.List;
import java.util.Objects;

record LevelRpgJournalResolvedPage(
        JournalPageId id,
        JournalPagePurpose purpose,
        BookPage page,
        List<BookInteractiveRegion> interactiveRegions
) {
    LevelRpgJournalResolvedPage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(interactiveRegions, "interactiveRegions");
        interactiveRegions = List.copyOf(interactiveRegions);
    }

    static LevelRpgJournalResolvedPage of(JournalPageId id, JournalPagePurpose purpose, BookPage page) {
        return new LevelRpgJournalResolvedPage(id, purpose, page, List.of());
    }

    static LevelRpgJournalResolvedPage of(
            JournalPageId id,
            JournalPagePurpose purpose,
            BookPage page,
            List<BookInteractiveRegion> interactiveRegions
    ) {
        return new LevelRpgJournalResolvedPage(id, purpose, page, interactiveRegions);
    }
}
