package net.zoogle.enchiridion.client.levelrpg;

import java.util.Objects;

public record JournalCharacterStat(
        String name,
        int value,
        boolean canAllocate,
        String passiveSummary
) {
    public JournalCharacterStat {
        Objects.requireNonNull(name, "name");
        passiveSummary = passiveSummary == null ? "" : passiveSummary;
    }
}
