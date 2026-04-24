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
                                new JournalCharacterStat("Valor", 0, false, "Combat discipline hardens your defense and footing."),
                                new JournalCharacterStat("Vitality", 0, false, "Endurance training increases your health pool."),
                                new JournalCharacterStat("Mining", 0, false, "Mining practice improves your pickaxe work rate."),
                                new JournalCharacterStat("Culinary", 0, false, "Culinary baseline scaling is reserved for a future food-efficiency hook."),
                                new JournalCharacterStat("Forging", 0, false, "Forging practice reinforces the armor you wear."),
                                new JournalCharacterStat("Artificing", 0, false, "Artificing baseline scaling is reserved for a future technical-crafting hook."),
                                new JournalCharacterStat("Magick", 0, false, "Magick baseline scaling is reserved for a future attunement hook."),
                                new JournalCharacterStat("Exploration", 0, false, "Field experience improves your travel pace.")
                        )
                ),
                List.of(
                        new JournalSkillEntry(
                                "valor",
                                "Valor",
                                "The discipline of meeting danger head-on and holding the line when battle turns vicious.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Combat discipline hardens your defense and footing.",
                                "No mastery points have awakened.",
                                "Raise Valor through direct combat to uncover stronger front-line techniques.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Choose your opening combat specialization."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Deepen the path you committed to earlier.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Reinforced Grip", "A basic combat recipe tied to martial progress."),
                                        new JournalUnlockEntry("Lv 6 - Ironbound Pattern", "A sturdier weapon recipe opens with deeper training.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "The first branch of Valor opens."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your chosen combat path sharpens into a specialization.")
                                )
                        ),
                        new JournalSkillEntry(
                                "vitality",
                                "Vitality",
                                "The discipline of endurance, recovery, and surviving the punishment that ends lesser journeys.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Endurance training increases your health pool.",
                                "No mastery points have awakened.",
                                "Raise Vitality through survival and recovery to unlock sturdier defensive options.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Commit to recovery or resilience."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Turn that choice into a defining survival trait.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 4 - Fortified Meal", "A sustaining recipe improves survivability."),
                                        new JournalUnlockEntry("Lv 8 - Hardened Tonic", "A stronger defensive preparation becomes available.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Choose whether your endurance favors recovery or resilience."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your chosen survival doctrine hardens into a lasting trait.")
                                )
                        ),
                        new JournalSkillEntry(
                                "mining",
                                "Mining",
                                "The discipline of gathering from the deep earth and pushing farther into resource-rich ground.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Mining practice improves your pickaxe work rate.",
                                "No mastery points have awakened.",
                                "Raise Mining through excavation to reveal deeper ore routes and extraction perks.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Choose whether your mining favors stone or ore."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Sharpen that focus into a specialist route.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Smelter Notes", "A better ore-processing recipe appears."),
                                        new JournalUnlockEntry("Lv 7 - Survey Kit", "A utility recipe supports longer mining runs.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Set your discipline toward speed, ore, or underground utility."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your chosen route becomes a specialist mining practice.")
                                )
                        ),
                        new JournalSkillEntry(
                                "culinary",
                                "Culinary",
                                "The discipline of preparing stronger meals, steadier sustain, and support for the road ahead.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Culinary baseline scaling is reserved for a future food-efficiency hook.",
                                "No mastery points have awakened.",
                                "Raise Culinary through preparation and provisioning to unlock richer meals and support dishes.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Lean into efficient cooking or stronger nourishment."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Refine your kitchen into a defining support tool.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 2 - Traveler's Supper", "A better field meal becomes available."),
                                        new JournalUnlockEntry("Lv 5 - Hearth Stew", "A heartier recipe rewards steady practice.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Choose between faster preparation and stronger nourishment."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your kitchen develops into a specialized support craft.")
                                )
                        ),
                        new JournalSkillEntry(
                                "forging",
                                "Forging",
                                "The discipline of strengthening gear, improving crafted equipment, and advancing smithing work.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Forging practice reinforces the armor you wear.",
                                "No mastery points have awakened.",
                                "Raise Forging through smithing work to reveal stronger gear patterns and upgrades.",
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
                                "The discipline of utility tools, technical parts, and clever support crafts.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Artificing baseline scaling is reserved for a future technical-crafting hook.",
                                "No mastery points have awakened.",
                                "Raise Artificing through clever construction to expand utility crafts and technical support.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Specialize in technical crafting or improved output."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Push that specialty into a major utility advantage.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Utility Frame", "A foundational support recipe unlocks."),
                                        new JournalUnlockEntry("Lv 7 - Precision Toolkit", "A more advanced technical craft appears.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Choose whether your craft favors output or technical precision."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your workshop gains a defining utility specialty.")
                                )
                        ),
                        new JournalSkillEntry(
                                "magick",
                                "Magick",
                                "The discipline of arcane study, attunement, and recipes shaped through magical knowledge.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Magick baseline scaling is reserved for a future attunement hook.",
                                "No mastery points have awakened.",
                                "Raise Magick through study and attunement to reveal stronger arcane crafts and ritual benefits.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Choose your first arcane specialization."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Deepen that magical path into a true identity.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 4 - Sigil Primer", "A basic magical crafting route opens."),
                                        new JournalUnlockEntry("Lv 9 - Ritual Focus", "A stronger arcane preparation recipe unlocks.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "The first arcane branch opens to you."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your chosen magical discipline becomes a true calling.")
                                )
                        ),
                        new JournalSkillEntry(
                                "exploration",
                                "Exploration",
                                "The discipline of movement, discovery, and lasting longer across the world at large.",
                                0,
                                0,
                                0L,
                                12L,
                                "0 / 12",
                                false,
                                "Field experience improves your travel pace.",
                                "No mastery points have awakened.",
                                "Raise Exploration through travel and discovery to reveal stronger route-support tools.",
                                List.of(
                                        new JournalUnlockEntry("Lv 10 - First Mastery", "Choose how your journeying style evolves."),
                                        new JournalUnlockEntry("Lv 20 - Branch Mastery", "Commit to a full travel specialization.")
                                ),
                                List.of(
                                        new JournalUnlockEntry("Lv 3 - Trail Pack", "A travel support recipe unlocks."),
                                        new JournalUnlockEntry("Lv 6 - Surveyor's Kit", "A better exploration utility recipe appears.")
                                ),
                                List.of(
                                        new JournalMilestoneEntry("Lv 10 - First Mastery", "Your path splits toward speed, safety, or discovery."),
                                        new JournalMilestoneEntry("Lv 20 - Branch Deepening", "Your chosen travel discipline becomes a reliable specialty.")
                                )
                        )
                )
        );
    }
}
