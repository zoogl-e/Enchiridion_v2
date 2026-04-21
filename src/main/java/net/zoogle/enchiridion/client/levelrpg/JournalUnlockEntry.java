package net.zoogle.enchiridion.client.levelrpg;

import java.util.Objects;

public record JournalUnlockEntry(
        String title,
        String description
) {
    public JournalUnlockEntry {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
    }
}
