package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookContext;

import java.util.List;

public final class MockLevelRpgJournalSnapshots {
    private MockLevelRpgJournalSnapshots() {}

    public static LevelRpgJournalSnapshot create(BookContext context) {
        return new LevelRpgJournalSnapshot(
                "Level RPG Journal",
                "Growth Ledger",
                String.join("\n",
                        "This journal now separates your build from each skill's rewards.",
                        "",
                        "Character pages keep your current spread together in one place.",
                        "Skill pages focus on what each discipline unlocks: perks, recipes, and mastery paths."
                ),
                new JournalCharacterSheet(
                        "Character Record",
                        "Adventurer\nWarden of the unwritten path\nArchetype Unchosen",
                        "Invested: 0 levels chosen\nMastery: 0 levels earned\nSkill Points: 0 ready | 0 spent | 0 earned",
                        "Mastery is earned through practice. Skill Levels are chosen with the points that mastery grants.",
                        List.of(
                                new JournalCharacterStat("Valor", 0, 0f, 0f, false, "Combat mastery sharpens your offense and hardens your body."),
                                new JournalCharacterStat("Finesse", 0, 0f, 0f, false, "Deliberate movement sharpens your footwork and positioning."),
                                new JournalCharacterStat("Arcana", 0, 0f, 0f, false, "Scholarly understanding multiplies what other skills can do."),
                                new JournalCharacterStat("Delving", 0, 0f, 0f, false, "Mastery of the hidden and submerged improves your extraction pace."),
                                new JournalCharacterStat("Forging", 0, 0f, 0f, false, "Forging practice reinforces the arms and armor you craft."),
                                new JournalCharacterStat("Artificing", 0, 0f, 0f, false, "Making things that persist — architecture and contraptions alike."),
                                new JournalCharacterStat("Hearth", 0, 0f, 0f, false, "Sustenance, alchemy, and the caretaker's craft.")
                        )
                ),
                List.of(
                        new JournalSkillEntry(
                                "valor",
                                "Valor",
                                "Combat through power and aggression. Two branches: Offense for damage, Grit for resilience.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Combat mastery sharpens your offense and hardens your body.",
                                "No mastery points have awakened.",
                                "Raise Valor through direct combat and surviving danger to uncover the Offense and Grit branches.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - Combat Awakening", "Choose your opening approach: vanguard aggression or duelist precision."),
                                        new JournalUnlockEntry("Lv 20 - Veteran Edge", "Your chosen doctrine deepens into a battlefield identity.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Reinforced Grip", "A basic combat recipe tied to martial progress."),
                                        new JournalUnlockEntry("Lv 6 - Ironbound Pattern", "A sturdier weapon recipe opens with deeper training.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - Combat Awakening", "The first branch of Valor opens."),
                                        new JournalMilestoneEntry("Lv 20 - Veteran Edge", "Your chosen combat path sharpens into a specialization.")
                                )
                        ),
                        new JournalSkillEntry(
                                "finesse",
                                "Finesse",
                                "Combat through mobility and positioning. Wins fights by dictating distance and terms, not raw output.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Deliberate movement sharpens your footwork and positioning.",
                                "No mastery points have awakened.",
                                "Raise Finesse through deliberate movement choices to unlock speed, evasion, and positional techniques.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Step", "Choose between pathfinding reliability and evasion depth."),
                                        new JournalUnlockEntry("Lv 20 - Shadowstep", "Your movement identity cements into a full positioning doctrine.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Fleet Wrap", "A lightweight travel recipe opens."),
                                        new JournalUnlockEntry("Lv 6 - Quickstep Kit", "A more advanced mobility recipe appears.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Step", "The first Finesse branch opens."),
                                        new JournalMilestoneEntry("Lv 20 - Shadowstep", "Your chosen movement path becomes a true identity.")
                                )
                        ),
                        new JournalSkillEntry(
                                "arcana",
                                "Arcana",
                                "Scholarly understanding of Minecraft's magical forces. A multiplier across other skills — not a damage class.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Scholarly understanding multiplies what other skills can do.",
                                "No mastery points have awakened.",
                                "Raise Arcana through enchanting, brewing, and magical craft to reveal cross-skill synergies.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - Arcane Awareness", "Choose between ritual control and raw channeling."),
                                        new JournalUnlockEntry("Lv 20 - Archmage Path", "Your chosen arcane school deepens into a true scholarly identity.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 4 - Sigil Primer", "A basic magical crafting route opens."),
                                        new JournalUnlockEntry("Lv 9 - Ritual Focus", "A stronger arcane preparation recipe unlocks.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - Arcane Awareness", "The first Arcana branch opens."),
                                        new JournalMilestoneEntry("Lv 20 - Archmage Path", "Your chosen magical discipline becomes a true calling.")
                                )
                        ),
                        new JournalSkillEntry(
                                "delving",
                                "Delving",
                                "Mastery of the hidden and submerged. Underground and underwater — anywhere the world hides things.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Mastery of the hidden and submerged improves your extraction pace.",
                                "No mastery points have awakened.",
                                "Raise Delving through mining ore and exploring dangerous depths to reveal underground and underwater paths.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - Ore Sense", "Choose between extraction efficiency and deep prospecting."),
                                        new JournalUnlockEntry("Lv 20 - Deep Delver", "Your chosen branch becomes a specialist Delving identity.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Smelter Notes", "A better ore-processing recipe appears."),
                                        new JournalUnlockEntry("Lv 7 - Survey Kit", "A utility recipe supports longer delving runs.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - Ore Sense", "The first Delving branch opens."),
                                        new JournalMilestoneEntry("Lv 20 - Deep Delver", "Your chosen route becomes a specialist Delving practice.")
                                )
                        ),
                        new JournalSkillEntry(
                                "forging",
                                "Forging",
                                "Blacksmith identity with social weight. Weapons and armor made with intentionality and craft.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Forging practice reinforces the arms and armor you craft.",
                                "No mastery points have awakened.",
                                "Raise Forging through smithing work to reveal stronger gear patterns, signatures, and craft upgrades.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Commit to a smithing focus."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Transform that focus into an expert craft path.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 4 - Tempered Edge", "A stronger weapon improvement unlocks."),
                                        new JournalUnlockEntry("Lv 8 - Reinforced Plate", "An improved armor pattern appears.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Choose whether your forge favors efficiency, durability, or power."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your forge practice matures into an expert tradition.")
                                )
                        ),
                        new JournalSkillEntry(
                                "artificing",
                                "Artificing",
                                "Making things that persist. Two branches: Architect for beauty, Mechanist for function.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Making things that persist — architecture and contraptions alike.",
                                "No mastery points have awakened.",
                                "Raise Artificing through building and clever construction to unlock Architect and Mechanist paths.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Specialize in aesthetic building or functional contraptions."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Push that specialty into a major utility or structural advantage.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Utility Frame", "A foundational support recipe unlocks."),
                                        new JournalUnlockEntry("Lv 7 - Precision Toolkit", "A more advanced technical craft appears.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Choose between the Architect and Mechanist paths."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your workshop gains a defining specialty.")
                                )
                        ),
                        new JournalSkillEntry(
                                "hearth",
                                "Hearth",
                                "Food, sustenance, alchemy, brewing. The caretaker skill — the most powerful players depend on you.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Sustenance, alchemy, and the caretaker's craft.",
                                "No mastery points have awakened.",
                                "Raise Hearth through preparing food and brewing to reveal provisioning and gourmet paths.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - Kitchen Foundation", "Choose between efficient provisioning and refined cuisine."),
                                        new JournalUnlockEntry("Lv 20 - Feastwright", "Your kitchen becomes a community asset.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 2 - Traveler's Supper", "A better field meal becomes available."),
                                        new JournalUnlockEntry("Lv 5 - Hearthfire Stew", "A heartier recipe rewards steady practice.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - Kitchen Foundation", "Choose between faster preparation and stronger nourishment."),
                                        new JournalMilestoneEntry("Lv 20 - Feastwright", "Your Hearth craft develops into a specialized support identity.")
                                )
                        )
                )
        );
    }
}
