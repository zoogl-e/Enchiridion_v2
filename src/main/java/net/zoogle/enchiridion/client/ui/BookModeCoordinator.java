package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.client.levelrpg.ArchetypeReelState;

final class BookModeCoordinator {
    boolean isReelActive(ArchetypeReelState reelState) {
        return reelState != null && reelState.active();
    }

    boolean isSkillTreeProjectionActive(boolean skillTreeProjectionActive) {
        return skillTreeProjectionActive;
    }

    boolean shouldResolvePageInteraction(BookScreenController controller, ArchetypeReelState reelState, boolean skillTreeProjectionActive) {
        return !isReelActive(reelState) && !controller.isProjectionVisible() && !isSkillTreeProjectionActive(skillTreeProjectionActive);
    }

    boolean shouldHandleReelInput(ArchetypeReelState reelState) {
        return isReelActive(reelState);
    }

    boolean shouldHandleSkillTreeProjectionInput(boolean skillTreeProjectionActive) {
        return isSkillTreeProjectionActive(skillTreeProjectionActive);
    }

    boolean canHandleProjectionInput(BookScreenController controller, ArchetypeReelState reelState) {
        return !isReelActive(reelState) && controller.isProjectionVisible();
    }

    boolean canHandleProjectionInput(BookScreenController controller) {
        return controller.isProjectionVisible();
    }

    boolean canHandleJournalInput(BookScreenController controller, ArchetypeReelState reelState, boolean skillTreeProjectionActive) {
        return !isReelActive(reelState) && !isSkillTreeProjectionActive(skillTreeProjectionActive) && controller.isJournalReadable();
    }

    boolean canHandleJournalInput(BookScreenController controller) {
        return canHandleJournalInput(controller, null, false);
    }
}
