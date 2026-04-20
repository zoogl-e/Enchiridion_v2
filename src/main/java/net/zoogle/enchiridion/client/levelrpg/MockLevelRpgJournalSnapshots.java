package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookContext;

import java.util.List;

public final class MockLevelRpgJournalSnapshots {
    private MockLevelRpgJournalSnapshots() {}

    public static LevelRpgJournalSnapshot create(BookContext context) {
        return new LevelRpgJournalSnapshot(
                "Level RPG Journal",
                "Bearer\nAdventurer\n\nArchetype\nUnchosen\n\nUse this journal to review\n- role and current standing\n- how each skill improves\n- what each skill is for\n- the next unlock worth chasing",
                "Overview",
                "Journal at a glance\nSkills tracked: 8\n\nEach skill page shows\n- what the skill does\n- how to train it\n- when to rely on it\n- the next unlock or mastery step",
                "Skills",
                List.of(
                        new JournalSkillEntry("Valor", "Builds combat strength, weapon confidence, and front-line pressure."),
                        new JournalSkillEntry("Vitality", "Improves endurance, recovery, and the ability to survive drawn-out fights."),
                        new JournalSkillEntry("Mining", "Rewards time underground with faster gathering and deeper resource access."),
                        new JournalSkillEntry("Culinary", "Turns ingredients into better meals, support, and steady preparation."),
                        new JournalSkillEntry("Forging", "Supports smithing progress and the crafting of stronger equipment."),
                        new JournalSkillEntry("Artificing", "Covers clever devices, technical crafting, and efficiency through invention."),
                        new JournalSkillEntry("Magick", "Advances arcane crafting, attunement, and power shaped through magical study."),
                        new JournalSkillEntry("Exploration", "Favors travel, discovery, and movement-based advantages in the wild.")
                ),
                "Current Focus",
                "Why now\nThis skill takes priority because it matches what you are using most.\n\nTrain it\nKeep performing the actions tied to this discipline.\n\nUse it\nLet it shape the part of your build you rely on most.\n\nNext step\nWatch for the next unlock or mastery point.",
                new JournalSkillEntry(
                        "Valor",
                        "Role\nBuilds combat strength, weapon confidence, and front-line pressure.\n\nCurrent\nLv 0 | XP 0\n\nTrain\nFight with melee weapons. Swords, axes, maces, and tridents are the clearest training route.\n\nUse\nLean on it for front-line combat, stronger weapon play, and direct pressure.\n\nNext\nWatch for the next unlock tied to combat progress."
                ),
                "Milestones",
                List.of(
                        new JournalMilestoneEntry("Current Pace", "Lv 0 | XP 0"),
                        new JournalMilestoneEntry("Next Breakthrough", "Watch for the next mastery point or unlock threshold as your level climbs."),
                        new JournalMilestoneEntry("Long-Term Goal", "Shape this skill into a specialty that supports the rest of your build.")
                )
        );
    }
}
