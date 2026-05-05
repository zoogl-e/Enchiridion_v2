package net.zoogle.enchiridion.client.levelrpg.model;

import java.util.Objects;

public record JournalCharacterStat(
        String name,
        int value,
        int masteryValue,
        /** 0–1: display-normalized mastery level radius (log scale, stroke-only ring). */
        float masteryLevelNorm,
        /**
         * 0–1: display-normalized radius for mastery + 1 (soft-cap guide), drawn as a stroke-only outer ring.
         */
        float masteryNextLevelRingNorm,
        boolean canAllocate,
        String passiveSummary
) {
    public JournalCharacterStat {
        Objects.requireNonNull(name, "name");
        passiveSummary = passiveSummary == null ? "" : passiveSummary;
    }
}
