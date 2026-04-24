package net.zoogle.enchiridion.client.levelrpg;

import java.util.Objects;

record JournalPageId(String value) {
    JournalPageId {
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
