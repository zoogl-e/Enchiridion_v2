package net.zoogle.enchiridion.client.levelrpg;

import java.util.List;
import java.util.Objects;

public record JournalSkillEntry(
        String name,
        String roleSummary,
        int investedSkillLevel,
        int masteryLevel,
        long masteryProgress,
        long masteryRequiredForNextLevel,
        String masteryProgressText,
        boolean canSpendSkillPoint,
        String passiveSummary,
        String masteryStanding,
        String pathForward,
        List<JournalUnlockEntry> perkUnlocks,
        List<JournalUnlockEntry> recipeUnlocks,
        List<JournalMilestoneEntry> milestones
) {
    public JournalSkillEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(roleSummary, "roleSummary");
        masteryProgressText = masteryProgressText == null ? "" : masteryProgressText;
        Objects.requireNonNull(passiveSummary, "passiveSummary");
        Objects.requireNonNull(masteryStanding, "masteryStanding");
        Objects.requireNonNull(pathForward, "pathForward");
        Objects.requireNonNull(perkUnlocks, "perkUnlocks");
        Objects.requireNonNull(recipeUnlocks, "recipeUnlocks");
        Objects.requireNonNull(milestones, "milestones");
        perkUnlocks = List.copyOf(perkUnlocks);
        recipeUnlocks = List.copyOf(recipeUnlocks);
        milestones = List.copyOf(milestones);
    }
}
