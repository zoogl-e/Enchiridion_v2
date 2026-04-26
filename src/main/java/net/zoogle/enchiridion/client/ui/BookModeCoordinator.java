package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.client.levelrpg.ArchetypeReelState;

final class BookModeCoordinator {
    boolean isReelActive(ArchetypeReelState reelState) {
        return reelState != null && reelState.active();
    }

    boolean shouldResolvePageInteraction(BookScreenController controller, ArchetypeReelState reelState) {
        return !isReelActive(reelState) && !controller.isProjectionVisible();
    }

    boolean shouldHandleReelInput(ArchetypeReelState reelState) {
        return isReelActive(reelState);
    }

    boolean canHandleProjectionInput(BookScreenController controller, ArchetypeReelState reelState) {
        return !isReelActive(reelState) && controller.isProjectionVisible();
    }

    boolean canHandleProjectionInput(BookScreenController controller) {
        return controller.isProjectionVisible();
    }

    boolean canHandleJournalInput(BookScreenController controller, ArchetypeReelState reelState) {
        return !isReelActive(reelState) && controller.isJournalReadable();
    }

    boolean canHandleJournalInput(BookScreenController controller) {
        return controller.isJournalReadable();
    }
}
