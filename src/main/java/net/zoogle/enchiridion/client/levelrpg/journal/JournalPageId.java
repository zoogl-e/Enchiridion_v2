package net.zoogle.enchiridion.client.levelrpg.journal;

import java.util.Objects;

public record JournalPageId(String value) {
    public JournalPageId {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Journal page id cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
