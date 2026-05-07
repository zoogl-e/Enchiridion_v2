package net.zoogle.enchiridion.client.levelrpg.bounty;

/**
 * Mirrors {@code BountyObjectiveHandlers} claimability for the hardcoded journal catalog.
 */
public final class JournalBountyObjectiveHandlers {
    private JournalBountyObjectiveHandlers() {}

    public static boolean isImplemented(JournalBountyObjectiveType type) {
        if (type == null || type == JournalBountyObjectiveType.NONE) {
            return false;
        }
        return switch (type) {
            case INDEX_INVEST_ONCE, REACH_Y, KILL_HOSTILE_MOB, MINE_ORE -> true;
            case CRAFT_ITEM,
                    PLACE_BLOCKS,
                    REPAIR_ITEM,
                    ENCHANT_ITEM,
                    COOK_ITEMS,
                    NONE -> false;
        };
    }
}
