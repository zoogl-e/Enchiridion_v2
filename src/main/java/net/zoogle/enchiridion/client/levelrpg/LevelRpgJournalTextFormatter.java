package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class LevelRpgJournalTextFormatter {
    private LevelRpgJournalTextFormatter() {}

    static LevelRpgJournalSnapshot assembleSnapshot(
            LocalPlayer player,
            LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView,
            LevelRpgJournalSnapshot mockSnapshot
    ) {
        List<JournalSkillEntry> skills = enrichSkills(mockSnapshot.skills(), profileView);
        JournalSkillEntry focusedBaseSkill = resolveFocusedSkill(player.getMainHandItem(), profileView, mockSnapshot);
        JournalSkillEntry focusedSkill = enrichFocusedSkill(focusedBaseSkill, profileView);
        List<JournalMilestoneEntry> milestones = resolveMilestonesFor(baseSkillName(focusedBaseSkill.name()), profileView, mockSnapshot);

        return new LevelRpgJournalSnapshot(
                mockSnapshot.journalTitle(),
                buildIntroText(player, profileView),
                mockSnapshot.overviewTitle(),
                buildOverviewText(profileView),
                mockSnapshot.skillsTitle(),
                skills,
                mockSnapshot.currentFocusTitle(),
                buildCurrentFocusText(player, focusedBaseSkill, profileView),
                focusedSkill,
                mockSnapshot.milestonesTitle(),
                milestones
        );
    }

    private static String buildIntroText(LocalPlayer player, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        String playerName = player.getName().getString();
        String archetypeText = profileView.selectedArchetypeDisplayName() == null
                ? "Unchosen"
                : profileView.selectedArchetypeDisplayName();
        return "Bearer\n"
                + playerName
                + "\n\nArchetype\n"
                + archetypeText
                + "\n\nUse this journal to review"
                + "\n- role and current standing"
                + "\n- how each skill improves"
                + "\n- what each skill is for"
                + "\n- the next unlock worth chasing";
    }

    private static String buildOverviewText(LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        StringBuilder builder = new StringBuilder();
        builder.append("Journal at a glance")
                .append("\nSkills tracked: ")
                .append(profileView.skillsByJournalName().size());
        if (profileView.selectedArchetypeDisplayName() != null) {
            builder.append("\nArchetype: ")
                    .append(profileView.selectedArchetypeDisplayName());
        }
        if (profileView.trackedSkillName() != null) {
            builder.append("\nRecent focus: ")
                    .append(profileView.trackedSkillName());
        }
        if (!profileView.recipeUnlockDefinitions().isEmpty()) {
            builder.append("\nRecipe leads: ")
                    .append(profileView.recipeUnlockDefinitions().size())
                    .append(" nearby");
        }
        if (!profileView.skillTreesByJournalName().isEmpty()) {
            builder.append("\nMastery trees: ")
                    .append(profileView.skillTreesByJournalName().size())
                    .append(" mapped");
        }
        builder.append("\n\nEach skill page shows")
                .append("\n- what the skill does")
                .append("\n- how to train it")
                .append("\n- when to rely on it")
                .append("\n- the next unlock or mastery step");
        return builder.toString();
    }

    private static String buildCurrentFocusText(LocalPlayer player, JournalSkillEntry focusedSkill, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        String focusedBaseName = baseSkillName(focusedSkill.name());
        LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot progress = profileView.skillsByJournalName().get(focusedBaseName);
        LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress = profileView.treeProgressByJournalName().get(focusedBaseName);
        StringBuilder builder = new StringBuilder();
        builder.append("Why now\n")
                .append(describeFocusReason(player.getMainHandItem(), focusedBaseName, profileView));
        if (progress != null) {
            builder.append("\n\nCurrent")
                    .append("\n")
                    .append(formatProgressSentence(progress));
        }
        builder.append("\n\nTrain it\n")
                .append(trainingGuidance(focusedBaseName))
                .append("\n\nUse it\n")
                .append(usageGuidance(focusedBaseName));
        if (treeProgress != null && treeProgress.tree() != null) {
            builder.append("\n\nMastery")
                    .append("\n")
                    .append(formatMasteryLedgerSentence(treeProgress));
            String thresholdSentence = formatThresholdSentence(treeProgress.nextThreshold());
            if (thresholdSentence != null) {
                builder.append("\n").append(thresholdSentence);
            }
        }
        builder.append("\n\nNext step\n")
                .append(describeNextGuideForSkill(focusedBaseName, profileView));
        return builder.toString().trim();
    }

    private static String describeFocusReason(ItemStack stack, String focusedBaseName, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        if (!stack.isEmpty()) {
            return "Your held " + stack.getHoverName().getString() + " points toward " + focusedBaseName + '.';
        }
        if (Objects.equals(profileView.trackedSkillName(), focusedBaseName)) {
            return focusedBaseName + " is highlighted because it was your most recent progress.";
        }
        return "No matching tool is in hand, so the journal falls back to recent progress and highlights " + focusedBaseName + '.';
    }

    private static JournalSkillEntry resolveFocusedSkill(ItemStack stack, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView, LevelRpgJournalSnapshot snapshot) {
        if (!stack.isEmpty()) {
            if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)) {
                return skillByName(snapshot, "Mining");
            }
            if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT) || stack.is(Items.MACE)) {
                return skillByName(snapshot, "Valor");
            }
            if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
                return skillByName(snapshot, "Exploration");
            }
            if (stack.has(DataComponents.FOOD)) {
                return skillByName(snapshot, "Culinary");
            }
            if (stack.isEnchanted() || stack.is(Items.BLAZE_ROD) || stack.is(Items.ENDER_PEARL) || stack.is(Items.EXPERIENCE_BOTTLE)) {
                return skillByName(snapshot, "Magick");
            }
            if (stack.is(Items.REDSTONE) || stack.is(Items.COMPASS) || stack.is(Items.CLOCK) || stack.is(Items.SPYGLASS)) {
                return skillByName(snapshot, "Artificing");
            }
        }

        if (profileView.trackedSkillName() != null) {
            return skillByName(snapshot, profileView.trackedSkillName());
        }
        return snapshot.focusedSkill();
    }

    private static JournalSkillEntry skillByName(LevelRpgJournalSnapshot snapshot, String name) {
        return snapshot.skills().stream()
                .filter(skill -> baseSkillName(skill.name()).equals(name))
                .findFirst()
                .orElse(snapshot.focusedSkill());
    }

    private static List<JournalSkillEntry> enrichSkills(List<JournalSkillEntry> mockSkills, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        return mockSkills.stream()
                .map(skill -> enrichSkill(skill, profileView.skillsByJournalName().get(skill.name()), profileView))
                .toList();
    }

    private static JournalSkillEntry enrichSkill(
            JournalSkillEntry skill,
            LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot canonicalSkill,
            LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView
    ) {
        if (canonicalSkill == null) {
            return skill;
        }

        String skillName = skill.name();
        LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress = profileView.treeProgressByJournalName().get(skillName);
        StringBuilder description = new StringBuilder();
        if (!skill.description().isBlank()) {
            description.append("Role\n")
                    .append(normalizeSentence(skill.description()));
        }
        description.append("\n\nNow\n")
                .append(formatProgressSentence(canonicalSkill));
        description.append("\n\nTrain\n")
                .append(trainingGuidance(skillName));
        description.append("\n\nUse\n")
                .append(usageGuidance(skillName));
        if (treeProgress != null && treeProgress.tree() != null) {
            description.append("\n\nMastery\n")
                    .append(formatMasteryLedgerSentence(treeProgress));
        }

        LevelRpgJournalSnapshotFactory.RecipeUnlockHint recipeHint = findNearbyRecipeUnlocks(skillName, profileView, 1).stream().findFirst().orElse(null);
        LevelRpgJournalSnapshotFactory.TreeUnlockHint treeHint = findTreeUnlockHint(skillName, profileView);
        description.append("\n\nNext\n");
        if (recipeHint != null) {
            description.append(recipeHint.shortSummary());
        } else if (treeHint != null) {
            description.append(treeHint.shortSummary());
        } else {
            description.append("No nearby unlock noted.");
        }

        return new JournalSkillEntry(formatSkillName(skill.name(), canonicalSkill.level()), description.toString());
    }

    private static JournalSkillEntry enrichFocusedSkill(JournalSkillEntry focusedSkill, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        String skillName = baseSkillName(focusedSkill.name());
        LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot snapshot = profileView.skillsByJournalName().get(skillName);
        if (snapshot == null) {
            return focusedSkill;
        }

        LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress = profileView.treeProgressByJournalName().get(skillName);
        StringBuilder description = new StringBuilder();
        if (!focusedSkill.description().isBlank()) {
            description.append("Role\n")
                    .append(normalizeSentence(focusedSkill.description()));
        }
        description.append("\n\nCurrent\n")
                .append(formatProgressSentence(snapshot));
        description.append("\n\nTrain\n")
                .append(trainingGuidance(skillName));
        description.append("\n\nUse\n")
                .append(usageGuidance(skillName));

        if (treeProgress != null && treeProgress.tree() != null) {
            description.append("\n\nMastery\n")
                    .append(formatMasteryLedgerSentence(treeProgress));
            if (!treeProgress.tree().summary().isBlank()) {
                description.append("\nTree\n")
                        .append(treeProgress.tree().summary());
            }
            String thresholdSentence = formatThresholdSentence(treeProgress.nextThreshold());
            if (thresholdSentence != null) {
                description.append("\nNext point\n")
                        .append(thresholdSentence);
            }
            LevelRpgJournalSnapshotFactory.TreeUnlockHint treeHint = findTreeUnlockHint(skillName, profileView);
            if (treeHint != null) {
                description.append("\nNext mastery\n")
                        .append(treeHint.description());
            }
            String unlockedSummary = formatUnlockedNodesSummary(treeProgress, 2);
            if (unlockedSummary != null) {
                description.append("\nUnlocked\n")
                        .append(unlockedSummary);
            }
        } else {
            LevelRpgJournalSnapshotFactory.TreeUnlockHint treeHint = findTreeUnlockHint(skillName, profileView);
            if (treeHint != null) {
                description.append("\n\nMastery\n")
                        .append(treeHint.description());
            }
        }

        LevelRpgJournalSnapshotFactory.RecipeUnlockHint recipeHint = findNearbyRecipeUnlocks(skillName, profileView, 1).stream().findFirst().orElse(null);
        description.append("\n\nNext unlock\n");
        if (recipeHint != null) {
            description.append(recipeHint.description());
        } else {
            description.append("No nearby recipe unlock noted.");
        }

        return new JournalSkillEntry(formatSkillName(skillName, snapshot.level()), description.toString().trim());
    }

    private static List<JournalMilestoneEntry> resolveMilestonesFor(
            String skillName,
            LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView,
            LevelRpgJournalSnapshot mockSnapshot
    ) {
        List<JournalMilestoneEntry> entries = new ArrayList<>();
        LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot snapshot = profileView.skillsByJournalName().get(skillName);
        LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress = profileView.treeProgressByJournalName().get(skillName);
        if (snapshot != null) {
            entries.add(new JournalMilestoneEntry("Current Pace", formatProgressSentence(snapshot)));
        }

        if (treeProgress != null && treeProgress.tree() != null) {
            entries.add(new JournalMilestoneEntry("Mastery Readiness", formatMasteryLedgerSentence(treeProgress)));
            if (treeProgress.nextThreshold() != null) {
                entries.add(new JournalMilestoneEntry("Next Breakthrough", formatThresholdSentence(treeProgress.nextThreshold())));
            }
            LevelRpgJournalSnapshotFactory.TreeUnlockHint treeHint = findTreeUnlockHint(skillName, profileView);
            if (treeHint != null) {
                entries.add(new JournalMilestoneEntry(treeHint.title(), treeHint.description()));
            }
            String unlockedSummary = formatUnlockedNodesSummary(treeProgress, 3);
            if (unlockedSummary != null) {
                entries.add(new JournalMilestoneEntry("Active Masteries", unlockedSummary));
            }
        } else {
            LevelRpgJournalSnapshotFactory.TreeUnlockHint treeHint = findTreeUnlockHint(skillName, profileView);
            if (treeHint != null) {
                entries.add(new JournalMilestoneEntry(treeHint.title(), treeHint.description()));
            }
        }

        for (LevelRpgJournalSnapshotFactory.RecipeUnlockHint hint : findNearbyRecipeUnlocks(skillName, profileView, 2)) {
            entries.add(new JournalMilestoneEntry(hint.title(), hint.description()));
        }

        if (!entries.isEmpty()) {
            return List.copyOf(entries);
        }
        return mockSnapshot.focusedSkillMilestones();
    }

    private static LevelRpgJournalSnapshotFactory.TreeUnlockHint findTreeUnlockHint(
            String skillName,
            LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView
    ) {
        LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress = profileView.treeProgressByJournalName().get(skillName);
        LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot skillSnapshot = profileView.skillsByJournalName().get(skillName);
        if (treeProgress == null || treeProgress.tree() == null || skillSnapshot == null) {
            return null;
        }
        LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree = treeProgress.tree();

        if (skillSnapshot.level() < tree.minSkillLevel()) {
            return new LevelRpgJournalSnapshotFactory.TreeUnlockHint(
                    "Tree Threshold",
                    "Reach " + skillName + " Lv " + tree.minSkillLevel() + ". Current: Lv " + skillSnapshot.level() + '.',
                    "Tree opens at Lv " + tree.minSkillLevel()
            );
        }

        LevelRpgJournalSnapshotFactory.NodeProgressView suggested = treeProgress.suggestedNextNode();
        if (suggested != null) {
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node = suggested.node();
            String nodeName = nodeDisplayName(node);
            String effectSentence = masteryEffectSentence(tree, node);
            String shortEffect = masteryEffectShortSummary(tree, node);
            return switch (suggested.status()) {
                case AVAILABLE -> new LevelRpgJournalSnapshotFactory.TreeUnlockHint(
                        "Suggested Mastery",
                        nodeName + " is ready now. Cost: " + node.normalizedCost() + " point" + (node.normalizedCost() == 1 ? "" : "s") + ". " + effectSentence,
                        "Next mastery: " + nodeName + shortEffect
                );
                case LOCKED_MASTERY_POINTS -> new LevelRpgJournalSnapshotFactory.TreeUnlockHint(
                        "Mastery Shortfall",
                        nodeName + " is the next strong pick. Need " + node.normalizedCost() + " point" + (node.normalizedCost() == 1 ? "" : "s") + "; have " + treeProgress.availablePoints() + ". " + effectSentence,
                        "Need points for " + nodeName + shortEffect
                );
                case LOCKED_PREREQUISITE -> new LevelRpgJournalSnapshotFactory.TreeUnlockHint(
                        "Mastery Chain",
                        "Unlock " + String.join(", ", suggested.missingRequirements().stream().map(LevelRpgJournalSnapshotFactory::humanizePath).toList()) + " before " + nodeName + ". " + effectSentence,
                        "Unlock path: " + nodeName + shortEffect
                );
                case LOCKED_SKILL_LEVEL -> new LevelRpgJournalSnapshotFactory.TreeUnlockHint(
                        "Skill Threshold",
                        nodeName + " opens at " + skillName + " Lv " + LevelRpgJournalSnapshotFactory.requiredLevelFor(tree, node) + ". Current: Lv " + treeProgress.skillLevel() + ". " + effectSentence,
                        "Opens at Lv " + LevelRpgJournalSnapshotFactory.requiredLevelFor(tree, node) + shortEffect
                );
                case UNLOCKED -> null;
            };
        }

        return new LevelRpgJournalSnapshotFactory.TreeUnlockHint(
                "Tree Cleared",
                "All visible nodes in the " + skillName + " tree are already unlocked.",
                "Current tree cleared"
        );
    }

    private static List<LevelRpgJournalSnapshotFactory.RecipeUnlockHint> findNearbyRecipeUnlocks(
            String skillName,
            LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView,
            int limit
    ) {
        LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot focusedSkill = profileView.skillsByJournalName().get(skillName);
        if (focusedSkill == null) {
            return List.of();
        }

        return profileView.recipeUnlockDefinitions().stream()
                .map(definition -> toRecipeUnlockHint(skillName, focusedSkill, definition, profileView))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(LevelRpgJournalSnapshotFactory.RecipeUnlockHint::focusedGap)
                        .thenComparingInt(LevelRpgJournalSnapshotFactory.RecipeUnlockHint::totalMissingLevels)
                        .thenComparing(hint -> hint.recipeId().toString()))
                .limit(Math.max(0, limit))
                .toList();
    }

    private static LevelRpgJournalSnapshotFactory.RecipeUnlockHint toRecipeUnlockHint(
            String skillName,
            LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot focusedSkill,
            LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView definition,
            LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView
    ) {
        LevelRpgJournalSnapshotFactory.RecipeRequirementView focusedRequirement = definition.requirements().stream()
                .filter(requirement -> Objects.equals(requirement.skillName(), skillName))
                .findFirst()
                .orElse(null);
        if (focusedRequirement == null) {
            return null;
        }

        List<String> requirementLines = new ArrayList<>();
        List<String> missingLines = new ArrayList<>();
        int totalMissingLevels = 0;
        for (LevelRpgJournalSnapshotFactory.RecipeRequirementView requirement : definition.requirements()) {
            requirementLines.add(requirement.skillName() + " Lv " + requirement.minLevel());
            LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot current = profileView.skillsByJournalName().get(requirement.skillName());
            int currentLevel = current != null ? current.level() : 0;
            if (currentLevel < requirement.minLevel()) {
                missingLines.add(requirement.skillName() + " " + currentLevel + '/' + requirement.minLevel());
                totalMissingLevels += requirement.minLevel() - currentLevel;
            }
        }

        if (missingLines.isEmpty()) {
            return null;
        }

        String recipeName = definition.displayName();
        int focusedGap = Math.max(0, focusedRequirement.minLevel() - focusedSkill.level());
        return new LevelRpgJournalSnapshotFactory.RecipeUnlockHint(
                definition.recipeId(),
                "Recipe Unlock",
                recipeName + "\nReq: " + String.join(", ", requirementLines) + "\nMissing: " + String.join(", ", missingLines),
                "Closest recipe: " + recipeName + " (" + String.join(", ", missingLines) + ")",
                focusedGap,
                totalMissingLevels
        );
    }

    private static String describeNextGuideForSkill(String skillName, LevelRpgJournalSnapshotFactory.CanonicalProfileView profileView) {
        LevelRpgJournalSnapshotFactory.TreeUnlockHint treeHint = findTreeUnlockHint(skillName, profileView);
        if (treeHint != null) {
            return treeHint.description();
        }
        LevelRpgJournalSnapshotFactory.RecipeUnlockHint recipeHint = findNearbyRecipeUnlocks(skillName, profileView, 1).stream().findFirst().orElse(null);
        if (recipeHint != null) {
            return recipeHint.description();
        }
        return "No nearby mastery step or recipe unlock is noted for this skill.";
    }

    private static String formatSkillName(String baseName, int level) {
        return baseName + " Lv " + level;
    }

    private static String baseSkillName(String skillName) {
        int levelMarker = skillName.indexOf(" Lv ");
        return levelMarker >= 0 ? skillName.substring(0, levelMarker) : skillName;
    }

    private static String formatProgressSentence(LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot progress) {
        if (progress.xpToNextLevel() != null && progress.xpToNextLevel() > 0L) {
            return "Lv " + progress.level() + " | XP " + progress.xp() + " / " + progress.xpToNextLevel();
        }
        return "Lv " + progress.level() + " | XP " + progress.xp();
    }

    private static String formatMasteryLedgerSentence(LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress) {
        return "Ready: "
                + treeProgress.availablePoints()
                + " | Spent: "
                + treeProgress.spentPoints()
                + " | Earned: "
                + treeProgress.earnedPoints()
                + '.';
    }

    private static String formatThresholdSentence(LevelRpgJournalSnapshotFactory.ThresholdView threshold) {
        if (threshold == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Next point at Lv ")
                .append(threshold.level())
                .append(" (+")
                .append(threshold.points())
                .append(" point");
        if (threshold.points() != 1) {
            builder.append('s');
        }
        builder.append(')');
        if (!threshold.title().isBlank()) {
            builder.append(". ").append(threshold.title());
        }
        if (!threshold.description().isBlank()) {
            builder.append(". ").append(threshold.description());
        }
        return builder.toString().trim();
    }

    private static String formatUnlockedNodesSummary(LevelRpgJournalSnapshotFactory.SkillTreeProgressView treeProgress, int limit) {
        List<LevelRpgJournalSnapshotFactory.NodeProgressView> unlockedNodes = treeProgress.nodes().stream()
                .filter(node -> node.status() == LevelRpgJournalSnapshotFactory.NodeStatus.UNLOCKED)
                .limit(Math.max(0, limit))
                .toList();
        if (unlockedNodes.isEmpty()) {
            return null;
        }

        ArrayList<String> parts = new ArrayList<>();
        for (LevelRpgJournalSnapshotFactory.NodeProgressView unlocked : unlockedNodes) {
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node = unlocked.node();
            parts.add(formatNodeSummary(treeProgress.tree(), node));
        }

        String summary = String.join("; ", parts) + '.';
        long additional = treeProgress.nodes().stream().filter(node -> node.status() == LevelRpgJournalSnapshotFactory.NodeStatus.UNLOCKED).count() - unlockedNodes.size();
        if (additional > 0) {
            summary += " +" + additional + " more.";
        }
        return summary;
    }

    private static String nodeDisplayName(LevelRpgJournalSnapshotFactory.SkillTreeNodeView node) {
        if (node == null) {
            return "Unknown";
        }
        if (!node.title().isBlank()) {
            return node.title();
        }
        return LevelRpgJournalSnapshotFactory.humanizePath(node.id());
    }

    private static String formatNodeSummary(
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree,
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node
    ) {
        StringBuilder builder = new StringBuilder(nodeDisplayName(node));
        String detail = masteryEffectSummary(tree, node);
        if (detail == null) {
            detail = fallbackNodeSummary(node);
        }
        if (detail != null) {
            builder.append(" (").append(detail).append(')');
        }
        return builder.toString();
    }

    private static String masteryEffectSummary(
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree,
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node
    ) {
        if (tree == null || tree.skillId() == null || node == null || node.id().isBlank()) {
            return null;
        }
        return LevelRpgJournalSnapshotFactory.IMPLEMENTED_MASTERY_EFFECT_SUMMARIES.get(tree.skillId().getPath() + ':' + node.id());
    }

    private static String masteryEffectSentence(
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree,
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node
    ) {
        String summary = masteryEffectSummary(tree, node);
        if (summary != null) {
            return "It grants: " + summary + ' ';
        }
        String fallback = fallbackNodeSummary(node);
        if (fallback != null) {
            return "Journal note: " + fallback + ' ';
        }
        return "";
    }

    private static String masteryEffectShortSummary(
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree,
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node
    ) {
        String summary = masteryEffectSummary(tree, node);
        if (summary == null) {
            return "";
        }
        return " - " + summary;
    }

    private static String fallbackNodeSummary(LevelRpgJournalSnapshotFactory.SkillTreeNodeView node) {
        if (node == null) {
            return null;
        }
        if (!node.description().isBlank()) {
            return node.description();
        }
        if (!node.branch().isBlank()) {
            return LevelRpgJournalSnapshotFactory.humanizePath(node.branch()) + " branch.";
        }
        return null;
    }

    private static String normalizeSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.endsWith(".") ? text : text + '.';
    }

    private static String trainingGuidance(String skillName) {
        return switch (skillName) {
            case "Valor" -> "Fight with melee weapons. Swords, axes, maces, and tridents are the clearest training route.";
            case "Vitality" -> "Stay in dangerous situations, survive damage, and keep adventuring without folding.";
            case "Mining" -> "Break stone, ores, and other mining targets with the right tools.";
            case "Culinary" -> "Cook meals, process ingredients, and keep food preparation active.";
            case "Forging" -> "Smith gear, improve equipment, and stay busy with crafting benches tied to metalwork.";
            case "Artificing" -> "Build utility items, technical parts, and crafted tools with mechanical value.";
            case "Magick" -> "Work with enchanted or arcane items and recipes tied to magical materials.";
            case "Exploration" -> "Travel with purpose, scout new ground, and keep moving through points of interest.";
            default -> "Practice the actions tied to this discipline in regular play.";
        };
    }

    private static String usageGuidance(String skillName) {
        return switch (skillName) {
            case "Valor" -> "Lean on it for front-line combat, stronger weapon play, and direct pressure.";
            case "Vitality" -> "Lean on it when you need durability, recovery, and longer survival windows.";
            case "Mining" -> "Lean on it when your route depends on ore gathering and deeper material runs.";
            case "Culinary" -> "Lean on it for better food support, preparation, and sustain between fights.";
            case "Forging" -> "Lean on it when your build depends on crafted armor, weapons, and upgrades.";
            case "Artificing" -> "Lean on it for utility tools, technical crafting, and efficient support items.";
            case "Magick" -> "Lean on it for arcane crafting, magical utility, and effect-focused progression.";
            case "Exploration" -> "Lean on it for travel, scouting, and movement-focused play.";
            default -> "Use it when you want this discipline to shape your build direction.";
        };
    }
}
