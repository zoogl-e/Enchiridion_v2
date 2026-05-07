package net.zoogle.enchiridion.client.levelrpg.bounty;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class JournalBountyCatalog {
    private static final List<JournalBountyOffer> OFFERS = List.of(
            new JournalBountyOffer(
                    ResourceLocation.fromNamespaceAndPath("levelrpg", "first_steps"),
                    "First Inscription",
                    "The Bookmark urges your first true commitment.",
                    "Commit Essence at The Index.",
                    1,
                    1,
                    JournalBountyObjectiveType.INDEX_INVEST_ONCE,
                    1,
                    0
            ),
            new JournalBountyOffer(
                    ResourceLocation.fromNamespaceAndPath("levelrpg", "deep_breath"),
                    "A Deep Breath",
                    "Descend, endure, and return to the light.",
                    "Venture below the surface and return.",
                    1,
                    1,
                    JournalBountyObjectiveType.REACH_Y,
                    0,
                    50
            ),
            new JournalBountyOffer(
                    ResourceLocation.fromNamespaceAndPath("levelrpg", "embers_of_valor"),
                    "Embers of Valor",
                    "Courage glows brightest after the clash.",
                    "Slay a hostile creature.",
                    1,
                    1,
                    JournalBountyObjectiveType.KILL_HOSTILE_MOB,
                    1,
                    0
            ),
            new JournalBountyOffer(
                    ResourceLocation.fromNamespaceAndPath("levelrpg", "steady_hands"),
                    "Steady Hands",
                    "Stillness sharpens the path before commitment.",
                    "Practice precision before committing your next path.",
                    1,
                    1,
                    JournalBountyObjectiveType.NONE,
                    0,
                    0
            ),
            new JournalBountyOffer(
                    ResourceLocation.fromNamespaceAndPath("levelrpg", "stone_memory"),
                    "Stone Memory",
                    "The earth keeps counsel for patient listeners.",
                    "Mine 3 ore blocks at Y 32 or below.",
                    1,
                    1,
                    JournalBountyObjectiveType.MINE_ORE,
                    3,
                    32
            ),
            new JournalBountyOffer(
                    ResourceLocation.fromNamespaceAndPath("levelrpg", "warm_hearth"),
                    "Warm Hearth",
                    "Preparation is its own quiet vow.",
                    "Prepare yourself or another before the next journey.",
                    1,
                    1,
                    JournalBountyObjectiveType.NONE,
                    0,
                    0
            )
    );

    private JournalBountyCatalog() {}

    public static JournalBountyOfferSnapshot offerSnapshot(int tier, int spreadIndex) {
        int clampedTier = Math.clamp(tier, 1, 3);
        int offerCount = clampedTier * 2;
        int totalSpreads = offerCount / 2;
        int clampedSpread = Math.clamp(spreadIndex, 0, Math.max(0, totalSpreads - 1));
        int leftIndex = clampedSpread * 2;
        return new JournalBountyOfferSnapshot(
                OFFERS.get(leftIndex),
                OFFERS.get(leftIndex + 1),
                clampedSpread,
                totalSpreads
        );
    }

    public static int spreadCountForTier(int tier) {
        return Math.clamp(tier, 1, 3);
    }

    public static JournalBountyOffer resolveOffer(ResourceLocation bountyId) {
        if (bountyId == null) {
            return null;
        }
        for (JournalBountyOffer offer : OFFERS) {
            if (bountyId.equals(offer.id())) {
                return offer;
            }
        }
        return null;
    }
}
