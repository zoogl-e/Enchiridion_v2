package net.zoogle.enchiridion.client.levelrpg.model;

import java.util.List;
import java.util.Objects;

public record JournalCharacterSheet(
        String title,
        String identitySummary,
        String ledgerNote,
        String allocationStatus,
        List<JournalCharacterStat> stats
) {
    public JournalCharacterSheet {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(identitySummary, "identitySummary");
        Objects.requireNonNull(ledgerNote, "ledgerNote");
        Objects.requireNonNull(allocationStatus, "allocationStatus");
        Objects.requireNonNull(stats, "stats");
        stats = List.copyOf(stats);
    }
}
