package net.zoogle.enchiridion.client.levelrpg.model;

import java.util.Objects;

public record JournalMilestoneEntry(
        String title,
        String description
) {
    public JournalMilestoneEntry {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
    }
}
