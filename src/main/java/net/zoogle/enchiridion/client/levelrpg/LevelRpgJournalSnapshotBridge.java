package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.player.LocalPlayer;
import net.zoogle.enchiridion.api.BookContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class LevelRpgJournalSnapshotBridge {
    private static final String CLIENT_FACTORY_CLASS = "net.zoogle.levelrpg.client.journal.ClientJournalSnapshotFactory";

    private LevelRpgJournalSnapshotBridge() {}

    static LevelRpgJournalSnapshot create(BookContext context, LevelRpgJournalSnapshot fallback) {
        if (context == null || context.player() == null) {
            return fallback;
        }
        try {
            Class<?> factoryClass = Class.forName(CLIENT_FACTORY_CLASS);
            Method createMethod = factoryClass.getMethod("create");
            Object snapshot = createMethod.invoke(null);
            if (snapshot == null) {
                return fallback;
            }
            return adaptSnapshot(context.player(), snapshot, fallback);
        } catch (ReflectiveOperationException exception) {
            return fallback;
        }
    }

    private static LevelRpgJournalSnapshot adaptSnapshot(LocalPlayer player, Object snapshot, LevelRpgJournalSnapshot fallback)
            throws ReflectiveOperationException {
        Object characterLedger = call(snapshot, "characterLedger");
        List<?> skills = asList(call(snapshot, "skills"));

        JournalCharacterSheet characterSheet = characterLedger != null
                ? adaptCharacterSheet(player, characterLedger)
                : fallback.characterSheet();

        List<JournalSkillEntry> journalSkills = new ArrayList<>();
        for (Object skill : skills) {
            JournalSkillEntry adapted = adaptSkill(skill);
            if (adapted != null) {
                journalSkills.add(adapted);
            }
        }
        if (journalSkills.isEmpty()) {
            journalSkills = fallback.skills();
        }

        return new LevelRpgJournalSnapshot(
                fallback.journalTitle(),
                "Compendium of Growth",
                buildFrontMatterText(player, characterSheet, journalSkills),
                characterSheet,
                List.copyOf(journalSkills)
        );
    }

    private static JournalCharacterSheet adaptCharacterSheet(LocalPlayer player, Object ledger) throws ReflectiveOperationException {
        String archetypeName = stringValue(call(ledger, "archetypeName"));
        String archetypeDescription = stringValue(call(ledger, "archetypeDescription"));
        boolean archetypeLockedIn = booleanValue(call(ledger, "archetypeLockedIn"));
        int totalSkillLevels = intValue(call(ledger, "totalSkillLevels"));
        int totalMasteryLevels = intValue(call(ledger, "totalMasteryLevels"));
        int earnedSkillPoints = intValue(call(ledger, "earnedSkillPoints"));
        int spentSkillPoints = intValue(call(ledger, "spentSkillPoints"));
        int availableSkillPoints = intValue(call(ledger, "availableSkillPoints"));
        List<?> rows = asList(call(ledger, "rows"));

        List<JournalCharacterStat> stats = new ArrayList<>();
        for (Object row : rows) {
            String name = stringValue(call(row, "label"));
            int level = intValue(call(row, "level"));
            stats.add(new JournalCharacterStat(
                    name,
                    level,
                    false,
                    formatPassiveEffectsCompact(call(row, "passiveEffects"))
            ));
        }

        String identitySummary = player.getName().getString()
                + "\nWarden of the unwritten path\nArchetype " + (archetypeName.isBlank() ? "Unchosen" : archetypeName);
        StringBuilder ledgerNote = new StringBuilder();
        if (!archetypeDescription.isBlank()) {
            ledgerNote.append(archetypeDescription).append('\n');
        }
        ledgerNote.append("Invested: ")
                .append(totalSkillLevels)
                .append(" levels chosen");
        ledgerNote.append('\n')
                .append("Mastery: ")
                .append(totalMasteryLevels)
                .append(" levels earned");
        ledgerNote.append('\n')
                .append("Skill Points: ")
                .append(availableSkillPoints)
                .append(" ready | ")
                .append(spentSkillPoints)
                .append(" spent | ")
                .append(earnedSkillPoints)
                .append(" earned");
        String allocationStatus = archetypeLockedIn
                ? "Mastery is earned through practice. Skill Levels are chosen by spending the points that mastery grants."
                : "Your archetype is not yet sealed. Mastery is earned through practice; Skill Levels are chosen with the points that mastery grants.";

        return new JournalCharacterSheet(
                "Character Record",
                identitySummary,
                ledgerNote.toString(),
                allocationStatus,
                List.copyOf(stats)
        );
    }

    private static JournalSkillEntry adaptSkill(Object skill) throws ReflectiveOperationException {
        if (skill == null) {
            return null;
        }
        String displayName = stringValue(call(skill, "displayName"));
        String roleSummary = stringValue(call(skill, "summary"));
        int investedSkillLevel = firstNonZero(
                intValue(call(skill, "investedSkillLevel")),
                intValue(call(skill, "level"))
        );
        int masteryLevel = intValue(call(skill, "masteryLevel"));
        long masteryProgress = firstNonZero(
                longValue(call(skill, "masteryProgress")),
                longValue(call(skill, "masteryXp"))
        );
        long masteryRequiredForNextLevel = firstNonZero(
                longValue(call(skill, "masteryRequiredForNextLevel")),
                longValue(call(skill, "masteryXpToNextLevel"))
        );
        String masteryProgressText = firstNonBlank(
                stringValue(call(skill, "masteryProgressText")),
                masteryRequiredForNextLevel > 0L
                        ? masteryProgress + " / " + masteryRequiredForNextLevel
                        : String.valueOf(masteryProgress)
        );
        boolean canSpendSkillPoint = booleanValue(call(skill, "canSpendSkillPoint"));

        Object passiveEffects = call(skill, "passiveEffects");
        Object mastery = call(skill, "mastery");

        return new JournalSkillEntry(
                displayName,
                roleSummary,
                investedSkillLevel,
                masteryLevel,
                masteryProgress,
                masteryRequiredForNextLevel,
                masteryProgressText,
                canSpendSkillPoint,
                formatPassiveEffectsDetailed(passiveEffects),
                buildMasteryStanding(mastery),
                buildPathForward(mastery, skill, canSpendSkillPoint),
                buildPerkUnlocks(mastery),
                buildRecipeUnlocks(asList(call(skill, "recipeUnlocks"))),
                buildMilestones(mastery)
        );
    }

    private static String buildFrontMatterText(LocalPlayer player, JournalCharacterSheet characterSheet, List<JournalSkillEntry> skills) {
        return String.join("\n",
                player.getName().getString(),
                characterSheet.identitySummary(),
                "",
                "This compendium gathers your practiced disciplines, your standing gifts, and the choices that define your path.",
                "Turn to the ledger for the shape of your build.",
                "Turn to each discipline to see what practice has opened and what your specialization can still claim.",
                "",
                "Disciplines recorded: " + skills.size()
        );
    }

    private static String buildMasteryStanding(Object mastery) throws ReflectiveOperationException {
        if (mastery == null || !booleanValue(call(mastery, "hasTree"))) {
            return "No specialization path is inscribed for this discipline.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Practice opens ")
                .append(intValue(call(mastery, "unlockedTierCount")))
                .append(" tier");
        if (intValue(call(mastery, "unlockedTierCount")) != 1) {
            builder.append('s');
        }
        builder.append('.');
        builder.append("\nSpecialization: ")
                .append(intValue(call(mastery, "availablePoints")))
                .append(" ready | ")
                .append(intValue(call(mastery, "spentPoints")))
                .append(" spent | ")
                .append(intValue(call(mastery, "earnedPoints")));
        builder.append(" earned");

        List<?> nodes = asList(call(mastery, "nodes"));
        long unlockedCount = nodes.stream()
                .filter(node -> "UNLOCKED".equals(stringValue(callUnchecked(node, "status"))))
                .count();
        if (unlockedCount > 0) {
            builder.append("\nChosen nodes: ").append(unlockedCount);
        }
        String suggestedNextNodeId = stringValue(call(mastery, "suggestedNextNodeId"));
        if (!suggestedNextNodeId.isBlank()) {
            builder.append("\nNext path: ")
                    .append(LevelRpgJournalSnapshotFactory.humanizePath(suggestedNextNodeId));
        }
        return builder.toString();
    }

    private static String buildPathForward(Object mastery, Object skill, boolean canSpendSkillPoint) throws ReflectiveOperationException {
        if (canSpendSkillPoint) {
            return "A Skill Point is ready. Open this discipline to decide whether to deepen your investment.";
        }
        if (mastery != null) {
            int availablePoints = intValue(call(mastery, "availablePoints"));
            String suggestedNextNodeId = stringValue(call(mastery, "suggestedNextNodeId"));
            if (availablePoints > 0 && !suggestedNextNodeId.isBlank()) {
                return "A specialization point can be committed now. The nearest open path is "
                        + LevelRpgJournalSnapshotFactory.humanizePath(suggestedNextNodeId) + ".";
            }
            Object nextThreshold = call(mastery, "nextThreshold");
            if (nextThreshold != null) {
                return "Keep training toward Lv "
                        + intValue(call(nextThreshold, "requiredLevel"))
                        + " to open the next tier.";
            }
        }
        List<JournalUnlockEntry> recipes = buildRecipeUnlocks(asList(call(skill, "recipeUnlocks")));
        if (!recipes.isEmpty()) {
            return "The nearest craft lead is " + recipes.getFirst().title() + ".";
        }
        return "No immediate perk or craft lead is recorded for this discipline.";
    }

    private static List<JournalUnlockEntry> buildPerkUnlocks(Object mastery) throws ReflectiveOperationException {
        if (mastery == null || !booleanValue(call(mastery, "hasTree"))) {
            return List.of();
        }
        List<JournalUnlockEntry> entries = new ArrayList<>();
        Object nextThreshold = call(mastery, "nextThreshold");
        if (nextThreshold != null) {
            int level = intValue(call(nextThreshold, "requiredLevel"));
            entries.add(new JournalUnlockEntry(
                    "Lv " + level + " - " + stringValue(call(nextThreshold, "title")),
                    stringValue(call(nextThreshold, "description"))
            ));
        }

        for (Object node : asList(call(mastery, "nodes"))) {
            String status = stringValue(call(node, "status"));
            if (!"AVAILABLE".equals(status) && !"LOCKED_LEVEL".equals(status)) {
                continue;
            }
            int requiredLevel = intValue(call(node, "requiredLevel"));
            String prefix = "AVAILABLE".equals(status) ? "Available now. " : "Requires Lv " + requiredLevel + ". ";
            entries.add(new JournalUnlockEntry(
                    "Lv " + requiredLevel + " - " + stringValue(call(node, "title")),
                    prefix + firstNonBlank(stringValue(call(node, "description")), "No journal note recorded.")
            ));
            if (entries.size() >= 4) {
                break;
            }
        }
        return List.copyOf(entries);
    }

    private static List<JournalUnlockEntry> buildRecipeUnlocks(List<?> recipeUnlocks) throws ReflectiveOperationException {
        List<JournalUnlockEntry> entries = new ArrayList<>();
        for (Object unlock : recipeUnlocks) {
            int requiredLevel = intValue(call(unlock, "requiredLevel"));
            entries.add(new JournalUnlockEntry(
                    "Lv " + requiredLevel + " - " + stringValue(call(unlock, "title")),
                    stringValue(call(unlock, "description"))
            ));
        }
        return List.copyOf(entries);
    }

    private static List<JournalMilestoneEntry> buildMilestones(Object mastery) throws ReflectiveOperationException {
        if (mastery == null || !booleanValue(call(mastery, "hasTree"))) {
            return List.of();
        }
        List<JournalMilestoneEntry> milestones = new ArrayList<>();
        for (Object milestone : asList(call(mastery, "milestones"))) {
            milestones.add(new JournalMilestoneEntry(
                    "Lv " + intValue(call(milestone, "requiredLevel")) + " - " + stringValue(call(milestone, "title")),
                    stringValue(call(milestone, "description"))
            ));
        }
        return List.copyOf(milestones);
    }

    private static String formatPassiveEffectsCompact(Object passiveEffects) throws ReflectiveOperationException {
        if (passiveEffects == null) {
            return "";
        }
        boolean implemented = booleanValue(call(passiveEffects, "implemented"));
        List<?> entries = asList(call(passiveEffects, "entries"));
        if (!implemented) {
            return "No standing boon recorded";
        }
        if (entries.isEmpty()) {
            return "No standing boon active";
        }
        List<String> parts = new ArrayList<>();
        for (Object entry : entries) {
            String label = stringValue(call(entry, "label"));
            String valueText = stringValue(call(entry, "valueText"));
            if (label.isBlank() || valueText.isBlank()) {
                continue;
            }
            parts.add(valueText + " " + label);
        }
        return parts.isEmpty() ? "No standing boon active" : String.join(", ", parts);
    }

    private static String formatPassiveEffectsDetailed(Object passiveEffects) throws ReflectiveOperationException {
        if (passiveEffects == null) {
            return "";
        }
        boolean implemented = booleanValue(call(passiveEffects, "implemented"));
        String summary = stringValue(call(passiveEffects, "summary"));
        List<?> entries = asList(call(passiveEffects, "entries"));
        if (!implemented && entries.isEmpty()) {
            return firstNonBlank(summary, "No standing boon is inscribed for this discipline yet.");
        }
        StringBuilder builder = new StringBuilder();
        if (!summary.isBlank()) {
            builder.append(summary);
        }
        for (Object entry : entries) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append("* ")
                    .append(stringValue(call(entry, "label")))
                    .append(": ")
                    .append(stringValue(call(entry, "valueText")));
            String description = stringValue(call(entry, "description"));
            if (!description.isBlank()) {
                builder.append("\n").append(description);
            }
        }
        return builder.toString();
    }

    private static Object call(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static Object callUnchecked(Object target, String methodName) {
        try {
            return call(target, methodName);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static String stringValue(Object value) {
        return value instanceof String string ? string : "";
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static int firstNonZero(int preferred, int fallback) {
        return preferred != 0 ? preferred : fallback;
    }

    private static long firstNonZero(long preferred, long fallback) {
        return preferred != 0L ? preferred : fallback;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
