package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class LevelRpgJournalDataReader {
    private LevelRpgJournalDataReader() {}

    static LevelRpgJournalSnapshotFactory.CanonicalProfileView readCanonicalProfile(LocalPlayer player) {
        Map<String, LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot> loadedSkills = readCanonicalSkillsFromClientCache();
        LinkedHashMap<String, LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot> skillsByJournalName = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot> skillsById = new LinkedHashMap<>();
        loadedSkills.forEach((skillName, snapshot) -> {
            if (skillName != null && snapshot != null) {
                skillsByJournalName.put(skillName, snapshot);
                skillsById.put(snapshot.skillId(), snapshot);
            }
        });
        Map<String, LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView> skillTrees = readSkillTreeDefinitions();
        Map<String, LevelRpgJournalSnapshotFactory.SkillTreeProgressView> treeProgressByJournalName = readTreeProgressViews(skillsByJournalName, skillTrees);

        return new LevelRpgJournalSnapshotFactory.CanonicalProfileView(
                Map.copyOf(skillsByJournalName),
                Map.copyOf(skillsById),
                readSelectedArchetypeDisplayName(player).orElse(null),
                readTrackedSkillNameFromClientCache(),
                treeProgressByJournalName,
                readRecipeUnlockDefinitions(player),
                skillTrees
        );
    }

    private static Map<String, LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot> readCanonicalSkillsFromClientCache() {
        try {
            Class<?> cacheClass = Class.forName("net.zoogle.levelrpg.client.data.ClientProfileCache");
            Method isReadyMethod = cacheClass.getMethod("isReady");
            Object ready = isReadyMethod.invoke(null);
            if (!Boolean.TRUE.equals(ready)) {
                return Map.of();
            }

            Method getSkillsViewMethod = cacheClass.getMethod("getSkillsView");
            Object skillsView = getSkillsViewMethod.invoke(null);
            if (!(skillsView instanceof Map<?, ?> skillMap)) {
                return Map.of();
            }

            LinkedHashMap<String, LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot> snapshots = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : skillMap.entrySet()) {
                if (!(entry.getKey() instanceof ResourceLocation skillId)) {
                    continue;
                }
                String journalName = LevelRpgJournalSnapshotFactory.journalSkillName(skillId);
                if (journalName == null) {
                    continue;
                }
                LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot snapshot = toCanonicalSkillSnapshot(skillId, entry.getValue());
                if (snapshot != null) {
                    snapshots.put(journalName, snapshot);
                }
            }
            return Map.copyOf(snapshots);
        } catch (ReflectiveOperationException ignored) {
            return Map.of();
        }
    }

    private static LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot toCanonicalSkillSnapshot(ResourceLocation skillId, Object state) {
        if (state == null) {
            return null;
        }
        Integer level = readIntField(state, "level");
        Long xp = readLongField(state, "xp");
        if (level == null || xp == null) {
            return null;
        }
        return new LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot(skillId, level, xp, readXpToNextLevel(skillId, level));
    }

    private static Long readXpToNextLevel(ResourceLocation skillId, int level) {
        try {
            Class<?> levelingClass = Class.forName("net.zoogle.levelrpg.profile.SkillLeveling");
            Method method = levelingClass.getMethod("xpToNextLevel", ResourceLocation.class, int.class);
            Object value = method.invoke(null, skillId, level);
            return value instanceof Number number ? Math.max(0L, number.longValue()) : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String readTrackedSkillNameFromClientCache() {
        try {
            Class<?> cacheClass = Class.forName("net.zoogle.levelrpg.client.data.ClientProfileCache");
            Method method = cacheClass.getMethod("getLastSkillId");
            Object value = method.invoke(null);
            return value instanceof ResourceLocation skillId ? LevelRpgJournalSnapshotFactory.journalSkillName(skillId) : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Optional<String> readSelectedArchetypeDisplayName(LocalPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(LevelRpgJournalSnapshotFactory.LEVEL_RPG_PROFILE_KEY)) {
            return Optional.empty();
        }

        CompoundTag profileTag = root.getCompound(LevelRpgJournalSnapshotFactory.LEVEL_RPG_PROFILE_KEY);
        if (!profileTag.contains("archetype")) {
            return Optional.empty();
        }

        CompoundTag archetypeTag = profileTag.getCompound("archetype");
        if (!archetypeTag.contains("id")) {
            return Optional.empty();
        }

        try {
            ResourceLocation archetypeId = ResourceLocation.parse(archetypeTag.getString("id"));
            return resolveArchetypeDisplayName(archetypeId);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> resolveArchetypeDisplayName(ResourceLocation archetypeId) {
        try {
            Class<?> registryClass = Class.forName("net.zoogle.levelrpg.profile.ArchetypeRegistry");
            Method getMethod = registryClass.getMethod("get", ResourceLocation.class);
            Object definition = getMethod.invoke(null, archetypeId);
            if (definition != null) {
                Method displayNameMethod = definition.getClass().getMethod("displayName");
                Object displayName = displayNameMethod.invoke(definition);
                if (displayName instanceof String string && !string.isBlank()) {
                    return Optional.of(string);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Optional.of(LevelRpgJournalSnapshotFactory.humanizePath(archetypeId.getPath()));
    }

    private static Map<String, LevelRpgJournalSnapshotFactory.SkillTreeProgressView> readTreeProgressViews(
            Map<String, LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot> skillsByJournalName,
            Map<String, LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView> skillTreesByJournalName
    ) {
        LinkedHashMap<String, LevelRpgJournalSnapshotFactory.SkillTreeProgressView> result = new LinkedHashMap<>();
        for (Map.Entry<String, LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView> entry : skillTreesByJournalName.entrySet()) {
            String skillName = entry.getKey();
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree = entry.getValue();
            LevelRpgJournalSnapshotFactory.CanonicalSkillSnapshot skillSnapshot = skillsByJournalName.get(skillName);
            if (tree == null || skillSnapshot == null) {
                continue;
            }

            int spentPoints = readTreePointsSpentFromClientCache(skillSnapshot.skillId());
            Set<String> unlockedNodes = readUnlockedTreeNodesFromClientCache(skillSnapshot.skillId());
            int earnedPoints = masteryPointsForLevel(tree, skillSnapshot.level());
            int availablePoints = Math.max(0, earnedPoints - spentPoints);
            LevelRpgJournalSnapshotFactory.ThresholdView nextThreshold = nextThreshold(tree, skillSnapshot.level());
            List<LevelRpgJournalSnapshotFactory.NodeProgressView> nodeProgressViews = buildNodeProgressViews(tree, skillSnapshot.level(), availablePoints, unlockedNodes);
            LevelRpgJournalSnapshotFactory.NodeProgressView suggestedAvailableNode = nodeProgressViews.stream()
                    .filter(node -> node.status() == LevelRpgJournalSnapshotFactory.NodeStatus.AVAILABLE)
                    .findFirst()
                    .orElse(null);
            LevelRpgJournalSnapshotFactory.NodeProgressView suggestedLockedNode = nodeProgressViews.stream()
                    .filter(node -> node.status() != LevelRpgJournalSnapshotFactory.NodeStatus.UNLOCKED)
                    .min(Comparator
                            .comparingInt((LevelRpgJournalSnapshotFactory.NodeProgressView node) -> LevelRpgJournalSnapshotFactory.requiredLevelFor(tree, node.node()))
                            .thenComparing(node -> node.node().id()))
                    .orElse(null);
            LevelRpgJournalSnapshotFactory.NodeProgressView suggestedNextNode = suggestedAvailableNode != null ? suggestedAvailableNode : suggestedLockedNode;

            result.put(skillName, new LevelRpgJournalSnapshotFactory.SkillTreeProgressView(
                    skillName,
                    skillSnapshot.skillId(),
                    tree,
                    skillSnapshot.level(),
                    earnedPoints,
                    spentPoints,
                    availablePoints,
                    unlockedNodes,
                    nextThreshold,
                    List.copyOf(nodeProgressViews),
                    suggestedAvailableNode,
                    suggestedNextNode
            ));
        }
        return Map.copyOf(result);
    }

    private static int readTreePointsSpentFromClientCache(ResourceLocation skillId) {
        try {
            Class<?> cacheClass = Class.forName("net.zoogle.levelrpg.client.data.ClientProfileCache");
            Method method = cacheClass.getMethod("getTreePointsSpent", ResourceLocation.class);
            Object value = method.invoke(null, skillId);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private static Set<String> readUnlockedTreeNodesFromClientCache(ResourceLocation skillId) {
        try {
            Class<?> cacheClass = Class.forName("net.zoogle.levelrpg.client.data.ClientProfileCache");
            Method method = cacheClass.getMethod("getTreeUnlockedNodes", ResourceLocation.class);
            Object value = method.invoke(null, skillId);
            if (value instanceof Set<?> set) {
                return set.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Set.of();
    }

    private static List<LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView> readRecipeUnlockDefinitions(LocalPlayer player) {
        try {
            Class<?> registryClass = Class.forName("net.zoogle.levelrpg.data.RecipeUnlockRegistry");
            Method entriesMethod = registryClass.getMethod("entries");
            Object value = entriesMethod.invoke(null);
            if (!(value instanceof Iterable<?> entries)) {
                return List.of();
            }

            ArrayList<LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView> definitions = new ArrayList<>();
            for (Object entry : entries) {
                if (!(entry instanceof Map.Entry<?, ?> mapEntry) || !(mapEntry.getKey() instanceof ResourceLocation recipeId)) {
                    continue;
                }
                LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView definition = toRecipeUnlockDefinition(player, recipeId, mapEntry.getValue());
                if (definition != null && !definition.requirements().isEmpty()) {
                    definitions.add(definition);
                }
            }
            return List.copyOf(definitions);
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
    }

    private static LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView toRecipeUnlockDefinition(LocalPlayer player, ResourceLocation recipeId, Object definition) {
        if (definition == null) {
            return null;
        }
        try {
            Method requirementsMethod = definition.getClass().getMethod("requirements");
            Object requirementsValue = requirementsMethod.invoke(definition);
            if (!(requirementsValue instanceof List<?> requirements)) {
                return new LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView(recipeId, resolveRecipeDisplayName(player, recipeId), List.of());
            }

            ArrayList<LevelRpgJournalSnapshotFactory.RecipeRequirementView> requirementViews = new ArrayList<>();
            for (Object requirement : requirements) {
                if (requirement == null) {
                    continue;
                }
                Method skillIdMethod = requirement.getClass().getMethod("skillId");
                Method minLevelMethod = requirement.getClass().getMethod("minLevel");
                Object skillIdValue = skillIdMethod.invoke(requirement);
                Object minLevelValue = minLevelMethod.invoke(requirement);
                if (skillIdValue instanceof ResourceLocation skillId && minLevelValue instanceof Number minLevel) {
                    String skillName = LevelRpgJournalSnapshotFactory.journalSkillName(skillId);
                    if (skillName != null) {
                        requirementViews.add(new LevelRpgJournalSnapshotFactory.RecipeRequirementView(skillId, skillName, minLevel.intValue()));
                    }
                }
            }
            return new LevelRpgJournalSnapshotFactory.RecipeUnlockDefinitionView(recipeId, resolveRecipeDisplayName(player, recipeId), List.copyOf(requirementViews));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Map<String, LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView> readSkillTreeDefinitions() {
        try {
            Class<?> registryClass = Class.forName("net.zoogle.levelrpg.data.SkillTreeRegistry");
            Method entriesMethod = registryClass.getMethod("entries");
            Object value = entriesMethod.invoke(null);
            if (!(value instanceof Iterable<?> entries)) {
                return Map.of();
            }

            LinkedHashMap<String, LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView> result = new LinkedHashMap<>();
            for (Object entry : entries) {
                if (!(entry instanceof Map.Entry<?, ?> mapEntry) || !(mapEntry.getKey() instanceof ResourceLocation skillId)) {
                    continue;
                }
                String skillName = LevelRpgJournalSnapshotFactory.journalSkillName(skillId);
                if (skillName == null) {
                    continue;
                }
                LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView definition = toSkillTreeDefinition(skillId, mapEntry.getValue());
                if (definition != null) {
                    result.put(skillName, definition);
                }
            }
            return Map.copyOf(result);
        } catch (ReflectiveOperationException ignored) {
            return Map.of();
        }
    }

    private static LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView toSkillTreeDefinition(ResourceLocation skillId, Object definition) {
        if (definition == null) {
            return null;
        }
        try {
            Method minSkillLevelMethod = definition.getClass().getMethod("minSkillLevel");
            Method titleMethod = definition.getClass().getMethod("title");
            Method summaryMethod = definition.getClass().getMethod("summary");
            Method thresholdsMethod = definition.getClass().getMethod("thresholds");
            Method nodesMethod = definition.getClass().getMethod("nodes");
            Object minSkillLevelValue = minSkillLevelMethod.invoke(definition);
            Object titleValue = titleMethod.invoke(definition);
            Object summaryValue = summaryMethod.invoke(definition);
            Object thresholdsValue = thresholdsMethod.invoke(definition);
            Object nodesValue = nodesMethod.invoke(definition);

            int minSkillLevel = minSkillLevelValue instanceof Number number ? number.intValue() : 0;
            ArrayList<LevelRpgJournalSnapshotFactory.ThresholdView> thresholds = new ArrayList<>();
            if (thresholdsValue instanceof List<?> thresholdList) {
                for (Object threshold : thresholdList) {
                    LevelRpgJournalSnapshotFactory.ThresholdView thresholdView = toThresholdView(threshold);
                    if (thresholdView != null) {
                        thresholds.add(thresholdView);
                    }
                }
            }
            LinkedHashMap<String, LevelRpgJournalSnapshotFactory.SkillTreeNodeView> nodes = new LinkedHashMap<>();
            if (nodesValue instanceof Map<?, ?> nodeMap) {
                for (Map.Entry<?, ?> nodeEntry : nodeMap.entrySet()) {
                    if (!(nodeEntry.getKey() instanceof String nodeId)) {
                        continue;
                    }
                    LevelRpgJournalSnapshotFactory.SkillTreeNodeView node = toSkillTreeNode(nodeId, nodeEntry.getValue());
                    if (node != null) {
                        nodes.put(nodeId, node);
                    }
                }
            }
            return new LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView(
                    skillId,
                    minSkillLevel,
                    titleValue instanceof String string ? string : "",
                    summaryValue instanceof String string ? string : "",
                    List.copyOf(thresholds),
                    Map.copyOf(nodes)
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static LevelRpgJournalSnapshotFactory.ThresholdView toThresholdView(Object threshold) {
        if (threshold == null) {
            return null;
        }
        try {
            Method idMethod = threshold.getClass().getMethod("id");
            Method levelMethod = threshold.getClass().getMethod("level");
            Method pointsMethod = threshold.getClass().getMethod("points");
            Method titleMethod = threshold.getClass().getMethod("title");
            Method descriptionMethod = threshold.getClass().getMethod("description");

            Object idValue = idMethod.invoke(threshold);
            Object levelValue = levelMethod.invoke(threshold);
            Object pointsValue = pointsMethod.invoke(threshold);
            Object titleValue = titleMethod.invoke(threshold);
            Object descriptionValue = descriptionMethod.invoke(threshold);

            return new LevelRpgJournalSnapshotFactory.ThresholdView(
                    idValue instanceof String string ? string : "",
                    levelValue instanceof Number number ? number.intValue() : 0,
                    pointsValue instanceof Number number ? number.intValue() : 0,
                    titleValue instanceof String string ? string : "",
                    descriptionValue instanceof String string ? string : ""
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static LevelRpgJournalSnapshotFactory.SkillTreeNodeView toSkillTreeNode(String fallbackId, Object node) {
        if (node == null) {
            return null;
        }
        try {
            Method idMethod = node.getClass().getMethod("id");
            Method costMethod = node.getClass().getMethod("cost");
            Method requiresMethod = node.getClass().getMethod("requires");
            Method requiredSkillLevelMethod = node.getClass().getMethod("requiredSkillLevel");
            Method branchMethod = node.getClass().getMethod("branch");
            Method titleMethod = node.getClass().getMethod("title");
            Method descriptionMethod = node.getClass().getMethod("description");

            Object idValue = idMethod.invoke(node);
            Object costValue = costMethod.invoke(node);
            Object requiresValue = requiresMethod.invoke(node);
            Object requiredSkillLevelValue = requiredSkillLevelMethod.invoke(node);
            Object branchValue = branchMethod.invoke(node);
            Object titleValue = titleMethod.invoke(node);
            Object descriptionValue = descriptionMethod.invoke(node);

            String nodeId = idValue instanceof String string && !string.isBlank() ? string : fallbackId;
            int cost = costValue instanceof Number number ? number.intValue() : 0;
            List<String> requires = requiresValue instanceof List<?> list
                    ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                    : List.of();
            int requiredSkillLevel = requiredSkillLevelValue instanceof Number number ? number.intValue() : 0;
            return new LevelRpgJournalSnapshotFactory.SkillTreeNodeView(
                    nodeId,
                    cost,
                    List.copyOf(requires),
                    requiredSkillLevel,
                    branchValue instanceof String string ? string : "",
                    titleValue instanceof String string ? string : "",
                    descriptionValue instanceof String string ? string : ""
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static List<LevelRpgJournalSnapshotFactory.NodeProgressView> buildNodeProgressViews(
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree,
            int skillLevel,
            int availablePoints,
            Set<String> unlockedNodes
    ) {
        ArrayList<LevelRpgJournalSnapshotFactory.NodeProgressView> nodes = new ArrayList<>();
        for (LevelRpgJournalSnapshotFactory.SkillTreeNodeView node : tree.nodes().values()) {
            List<String> missingRequirements = node.requires().stream()
                    .filter(required -> !unlockedNodes.contains(required))
                    .toList();
            LevelRpgJournalSnapshotFactory.NodeStatus status = resolveNodeStatus(tree, node, skillLevel, availablePoints, unlockedNodes, missingRequirements);
            nodes.add(new LevelRpgJournalSnapshotFactory.NodeProgressView(node, status, missingRequirements));
        }
        return List.copyOf(nodes);
    }

    private static LevelRpgJournalSnapshotFactory.NodeStatus resolveNodeStatus(
            LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree,
            LevelRpgJournalSnapshotFactory.SkillTreeNodeView node,
            int skillLevel,
            int availablePoints,
            Set<String> unlockedNodes,
            List<String> missingRequirements
    ) {
        if (unlockedNodes.contains(node.id())) {
            return LevelRpgJournalSnapshotFactory.NodeStatus.UNLOCKED;
        }
        if (skillLevel < LevelRpgJournalSnapshotFactory.requiredLevelFor(tree, node)) {
            return LevelRpgJournalSnapshotFactory.NodeStatus.LOCKED_SKILL_LEVEL;
        }
        if (!missingRequirements.isEmpty()) {
            return LevelRpgJournalSnapshotFactory.NodeStatus.LOCKED_PREREQUISITE;
        }
        if (availablePoints < node.normalizedCost()) {
            return LevelRpgJournalSnapshotFactory.NodeStatus.LOCKED_MASTERY_POINTS;
        }
        return LevelRpgJournalSnapshotFactory.NodeStatus.AVAILABLE;
    }

    private static int masteryPointsForLevel(LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree, int skillLevel) {
        if (skillLevel < Math.max(0, tree.minSkillLevel())) {
            return 0;
        }
        int total = 0;
        for (LevelRpgJournalSnapshotFactory.ThresholdView threshold : tree.thresholds()) {
            if (skillLevel >= Math.max(0, threshold.level())) {
                total += Math.max(0, threshold.points());
            }
        }
        return Math.max(0, total);
    }

    private static LevelRpgJournalSnapshotFactory.ThresholdView nextThreshold(LevelRpgJournalSnapshotFactory.SkillTreeDefinitionView tree, int skillLevel) {
        for (LevelRpgJournalSnapshotFactory.ThresholdView threshold : tree.thresholds()) {
            if (skillLevel < Math.max(0, threshold.level())) {
                return threshold;
            }
        }
        return null;
    }

    private static Integer readIntField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getField(fieldName);
            return field.getInt(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Long readLongField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getField(fieldName);
            return field.getLong(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String resolveRecipeDisplayName(LocalPlayer player, ResourceLocation recipeId) {
        if (player == null || player.level() == null || recipeId == null) {
            return LevelRpgJournalSnapshotFactory.humanizePath(recipeId == null ? null : recipeId.getPath());
        }

        try {
            Object recipeManager = player.level().getRecipeManager();
            Method byKeyMethod = recipeManager.getClass().getMethod("byKey", ResourceLocation.class);
            Object holderValue = byKeyMethod.invoke(recipeManager, recipeId);
            if (holderValue instanceof Optional<?> optional && optional.isPresent()) {
                Object holder = optional.get();
                Object recipe = invokeNoArg(holder, "value");
                if (recipe != null) {
                    ItemStack result = resolveRecipeResultStack(player, recipe);
                    if (!result.isEmpty()) {
                        String displayName = result.getHoverName().getString();
                        if (displayName != null && !displayName.isBlank()) {
                            return displayName;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return LevelRpgJournalSnapshotFactory.humanizePath(recipeId.getPath());
    }

    private static ItemStack resolveRecipeResultStack(LocalPlayer player, Object recipe) {
        try {
            for (Method method : recipe.getClass().getMethods()) {
                if (!method.getName().equals("getResultItem")) {
                    continue;
                }
                Object value;
                if (method.getParameterCount() == 0) {
                    value = method.invoke(recipe);
                } else if (method.getParameterCount() == 1) {
                    value = method.invoke(recipe, player.level().registryAccess());
                } else {
                    continue;
                }
                if (value instanceof ItemStack stack) {
                    return stack;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }
}
