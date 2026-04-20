package net.zoogle.enchiridion.client.levelrpg;

import java.util.List;
import java.util.Objects;

public record LevelRpgJournalSnapshot(
        String journalTitle,
        String introText,
        String overviewTitle,
        String overviewText,
        String skillsTitle,
        List<JournalSkillEntry> skills,
        String currentFocusTitle,
        String currentFocusText,
        JournalSkillEntry focusedSkill,
        String milestonesTitle,
        List<JournalMilestoneEntry> focusedSkillMilestones
) {
    public LevelRpgJournalSnapshot {
        Objects.requireNonNull(journalTitle, "journalTitle");
        Objects.requireNonNull(introText, "introText");
        Objects.requireNonNull(overviewTitle, "overviewTitle");
        Objects.requireNonNull(overviewText, "overviewText");
        Objects.requireNonNull(skillsTitle, "skillsTitle");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(currentFocusTitle, "currentFocusTitle");
        Objects.requireNonNull(currentFocusText, "currentFocusText");
        Objects.requireNonNull(focusedSkill, "focusedSkill");
        Objects.requireNonNull(milestonesTitle, "milestonesTitle");
        Objects.requireNonNull(focusedSkillMilestones, "focusedSkillMilestones");
        skills = List.copyOf(skills);
        focusedSkillMilestones = List.copyOf(focusedSkillMilestones);
    }
}
