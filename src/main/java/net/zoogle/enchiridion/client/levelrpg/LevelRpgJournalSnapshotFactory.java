package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LevelRpgJournalSnapshotFactory {
    static final String LEVEL_RPG_PROFILE_KEY = "levelrpg:profile";
    static final Map<String, String> CANONICAL_SKILL_PATHS = Map.of(
            "Valor", "valor",
            "Vitality", "vitality",
            "Mining", "mining",
            "Culinary", "culinary",
            "Forging", "forging",
            "Artificing", "artificing",
            "Magick", "magick",
            "Exploration", "exploration"
    );
    static final Map<String, String> IMPLEMENTED_MASTERY_EFFECT_SUMMARIES = Map.ofEntries(
            Map.entry("valor:vanguard_stance", "While using axes or maces, you hit harder."),
            Map.entry("valor:duelist_footwork", "While using swords, you move faster."),
            Map.entry("valor:shieldbreaker_drive", "Your hits punish blocking enemies more effectively."),
            Map.entry("valor:execution_window", "You deal extra damage to enemies already near defeat."),
            Map.entry("valor:warlord_presence", "While using axes or maces, you become harder to knock back."),
            Map.entry("valor:finishing_chain", "While using swords, you attack faster."),
            Map.entry("mining:clean_cuts", "Pickaxes mine stone-like blocks faster."),
            Map.entry("mining:prospectors_eye", "Pickaxes mine ore faster."),
            Map.entry("mining:vein_following", "Breaking ore with a pickaxe can trigger a brief haste burst."),
            Map.entry("mining:quarry_discipline", "Pickaxes mine stone-like blocks even faster."),
            Map.entry("mining:voidward_prospecting", "While using a pickaxe deep underground, you gain night vision."),
            Map.entry("artificing:field_tinkering", "Unlocked technical crafts can occasionally yield an extra output."),
            Map.entry("artificing:precision_fabrication", "Refined machine outputs can occasionally yield an extra output when smelted."),
            Map.entry("artificing:salvage_loop", "Unlocked technical crafts gain an even better chance to yield an extra output."),
            Map.entry("artificing:clockwork_layouts", "Refined machine outputs gain an even better chance to yield an extra output when smelted."),
            Map.entry("artificing:improvised_rigging", "Simpler technical crafts gain an additional chance to yield an extra output."),
            Map.entry("artificing:master_mechanisms", "Refined machine outputs gain a further bonus chance to yield an extra output when smelted."),
            Map.entry("magick:ritual_geometry", "Unlocked magical crafts can occasionally yield an extra output."),
            Map.entry("magick:wild_channeling", "While you have a beneficial effect, you attack faster."),
            Map.entry("magick:sigil_memory", "Unlocked magical crafts gain an even better chance to yield an extra output."),
            Map.entry("magick:surging_current", "While you have a beneficial effect, you move faster."),
            Map.entry("magick:sealed_circuit", "You take less magic damage."),
            Map.entry("magick:stormcasting", "While you have a beneficial effect, you deal extra damage."),
            Map.entry("exploration:trail_marks", "While traveling on solid ground, you move faster."),
            Map.entry("exploration:wanderers_instinct", "In low light, you gain night vision."),
            Map.entry("exploration:measured_stride", "You take less fall damage."),
            Map.entry("exploration:hidden_waypoints", "After earning Exploration XP, you gain a brief speed boost."),
            Map.entry("exploration:cartographers_focus", "While sprinting on solid ground, you move even faster."),
            Map.entry("exploration:frontier_sense", "After earning Exploration XP, you gain night vision and reveal up to 6 nearby hostile mobs."),
            Map.entry("vitality:iron_reserve", "You gain extra maximum health."),
            Map.entry("vitality:restorative_bloom", "If you stay well fed and avoid damage for a while, you begin regenerating health."),
            Map.entry("vitality:last_stand", "When you are low on health, you gain brief resistance and take less damage."),
            Map.entry("vitality:rapid_recovery", "Your well-fed recovery begins sooner after taking damage."),
            Map.entry("vitality:stoneheart", "Heavy hits deal less damage to you."),
            Map.entry("vitality:renewing_core", "Your well-fed recovery starts much sooner and becomes stronger, restoring you up to roughly three-quarters health.")
    );

    private LevelRpgJournalSnapshotFactory() {}

    public static LevelRpgJournalSnapshot create(BookContext context) {
        LevelRpgJournalSnapshot mockSnapshot = MockLevelRpgJournalSnapshots.create(context);
        LocalPlayer player = context.player();
        if (player == null) {
            return mockSnapshot;
        }

        CanonicalProfileView profileView = LevelRpgJournalDataReader.readCanonicalProfile(player);
        if (!profileView.hasCanonicalSkillData()) {
            return mockSnapshot;
        }

        return LevelRpgJournalTextFormatter.assembleSnapshot(player, profileView, mockSnapshot);
    }

    static String journalSkillName(ResourceLocation skillId) {
        String path = skillId.getPath();
        for (Map.Entry<String, String> entry : CANONICAL_SKILL_PATHS.entrySet()) {
            if (Objects.equals(entry.getValue(), path)) {
                return entry.getKey();
            }
        }
        return null;
    }

    static int requiredLevelFor(SkillTreeDefinitionView tree, SkillTreeNodeView node) {
        return Math.max(Math.max(0, tree.minSkillLevel()), node.normalizedRequiredSkillLevel());
    }

    static String humanizePath(String path) {
        if (path == null || path.isBlank()) {
            return "Unknown";
        }
        String[] parts = path.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    static record CanonicalProfileView(
            Map<String, CanonicalSkillSnapshot> skillsByJournalName,
            Map<ResourceLocation, CanonicalSkillSnapshot> skillsById,
            String selectedArchetypeDisplayName,
            String trackedSkillName,
            Map<String, SkillTreeProgressView> treeProgressByJournalName,
            List<RecipeUnlockDefinitionView> recipeUnlockDefinitions,
            Map<String, SkillTreeDefinitionView> skillTreesByJournalName
    ) {
        private boolean hasCanonicalSkillData() {
            return !skillsByJournalName.isEmpty();
        }
    }

    static record CanonicalSkillSnapshot(
            ResourceLocation skillId,
            int level,
            long xp,
            Long xpToNextLevel
    ) {}

    static record RecipeUnlockDefinitionView(
            ResourceLocation recipeId,
            String displayName,
            List<RecipeRequirementView> requirements
    ) {}

    static record RecipeRequirementView(
            ResourceLocation skillId,
            String skillName,
            int minLevel
    ) {}

    static record SkillTreeDefinitionView(
            ResourceLocation skillId,
            int minSkillLevel,
            String title,
            String summary,
            List<ThresholdView> thresholds,
            Map<String, SkillTreeNodeView> nodes
    ) {}

    static record ThresholdView(
            String id,
            int level,
            int points,
            String title,
            String description
    ) {}

    static record SkillTreeNodeView(
            String id,
            int cost,
            List<String> requires,
            int requiredSkillLevel,
            String branch,
            String title,
            String description
    ) {
        int normalizedCost() {
            return Math.max(1, cost);
        }

        int normalizedRequiredSkillLevel() {
            return Math.max(0, requiredSkillLevel);
        }
    }

    static record SkillTreeProgressView(
            String skillName,
            ResourceLocation skillId,
            SkillTreeDefinitionView tree,
            int skillLevel,
            int earnedPoints,
            int spentPoints,
            int availablePoints,
            Set<String> unlockedNodes,
            ThresholdView nextThreshold,
            List<NodeProgressView> nodes,
            NodeProgressView suggestedAvailableNode,
            NodeProgressView suggestedNextNode
    ) {}

    static record NodeProgressView(
            SkillTreeNodeView node,
            NodeStatus status,
            List<String> missingRequirements
    ) {}

    enum NodeStatus {
        UNLOCKED,
        AVAILABLE,
        LOCKED_SKILL_LEVEL,
        LOCKED_PREREQUISITE,
        LOCKED_MASTERY_POINTS
    }

    static record TreeUnlockHint(
            String title,
            String description,
            String shortSummary
    ) {}

    static record RecipeUnlockHint(
            ResourceLocation recipeId,
            String title,
            String description,
            String shortSummary,
            int focusedGap,
            int totalMissingLevels
    ) {}
}
