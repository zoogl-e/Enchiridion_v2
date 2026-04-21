package net.zoogle.enchiridion.client.levelrpg;

import java.util.List;
import java.util.Objects;

public record LevelRpgJournalSnapshot(
        String journalTitle,
        String frontMatterTitle,
        String frontMatterText,
        JournalCharacterSheet characterSheet,
        List<JournalSkillEntry> skills
) {
    public LevelRpgJournalSnapshot {
        Objects.requireNonNull(journalTitle, "journalTitle");
        Objects.requireNonNull(frontMatterTitle, "frontMatterTitle");
        Objects.requireNonNull(frontMatterText, "frontMatterText");
        Objects.requireNonNull(characterSheet, "characterSheet");
        Objects.requireNonNull(skills, "skills");
        skills = List.copyOf(skills);
    }
}
