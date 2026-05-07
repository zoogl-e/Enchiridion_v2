package net.zoogle.enchiridion.client.levelrpg.bounty;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Client-side bounty offer row; mirrors LevelRPG {@code BountyDefinition} fields needed for UI.
 */
public record JournalBountyOffer(
        ResourceLocation id,
        String title,
        String summary,
        String objective,
        int rewardEssence,
        int firstCompletionBonusEssence,
        JournalBountyObjectiveType objectiveType,
        /** Required count for countable objectives; 0 when unused (e.g. REACH_Y uses only {@link #objectiveTargetY()}). */
        int objectiveCount,
        /** REACH_Y: surface threshold Y. MINE_ORE: max ore block Y inclusive (0 = no cap). */
        int objectiveTargetY
) {
    public JournalBountyOffer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(objective, "objective");
        Objects.requireNonNull(objectiveType, "objectiveType");
    }

    /** Matches server {@code BountyDefinition#objectiveImplemented()}. */
    public boolean objectiveImplemented() {
        return JournalBountyObjectiveHandlers.isImplemented(objectiveType);
    }
}
