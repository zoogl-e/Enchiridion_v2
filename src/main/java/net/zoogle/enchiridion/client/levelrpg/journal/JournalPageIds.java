package net.zoogle.enchiridion.client.levelrpg.journal;

import java.util.List;

final class JournalPageIds {
    private JournalPageIds() {}

    static JournalPageId openingLeft() {
        return new JournalPageId("opening:left");
    }

    static JournalPageId openingRight() {
        return new JournalPageId("opening:right");
    }

    static JournalPageId backCoverLeft() {
        return new JournalPageId("back-cover:left");
    }

    static JournalPageId backCoverRight() {
        return new JournalPageId("back-cover:right");
    }

    static JournalPageId unboundInitiation() {
        return new JournalPageId("unbound:initiation");
    }

    static JournalPageId unboundReading() {
        return new JournalPageId("unbound:reading");
    }

    static JournalPageId characterIdentity() {
        return new JournalPageId("character:identity");
    }

    static JournalPageId characterStanding() {
        return new JournalPageId("character:standing");
    }

    static JournalPageId ledger(int ordinal) {
        return new JournalPageId("ledger:" + ordinal);
    }

    static JournalPageId skill(String skillKey) {
        return new JournalPageId("skill:" + sanitize(skillKey));
    }

    static JournalPageId syntheticEmpty(int pageIndex) {
        return new JournalPageId("synthetic-empty:" + pageIndex);
    }

    static List<JournalPageId> aliasesFor(JournalPageId pageId) {
        if (pageId == null) {
            return List.of();
        }
        return switch (pageId.value()) {
            case "opening:left" -> List.of(new JournalPageId("legacy-opening:left"));
            case "opening:right" -> List.of(new JournalPageId("legacy-opening:right"));
            default -> List.of();
        };
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
