package net.zoogle.enchiridion.client.levelrpg;

import java.util.Objects;

public record JournalSkillEntry(
        String name,
        String description
) {
    public JournalSkillEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
    }
}
