package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;

public final class ReelFlowController {
    private static final int SYNC_TIMEOUT_TICKS = 80;

    public enum FlowPhase {
        INACTIVE,
        SELECTING,
        COMMITTING,
        REQUESTING_BIND,
        AWAITING_SYNC,
        COMPLETED,
        FAILED
    }

    private FlowPhase phase = FlowPhase.INACTIVE;
    private int awaitingSyncTicks;
    private String selectedFocusId;

    public void onOpened() {
        phase = FlowPhase.SELECTING;
        awaitingSyncTicks = 0;
    }

    public void onClosed() {
        phase = FlowPhase.INACTIVE;
        awaitingSyncTicks = 0;
        selectedFocusId = null;
    }

    public FlowPhase phase() {
        return phase;
    }

    public boolean canCancel() {
        return phase == FlowPhase.SELECTING || phase == FlowPhase.FAILED;
    }

    public boolean confirmFocused(ArchetypeReelState reelState) {
        JournalArchetypeChoice focusedChoice = reelState.focusedChoice();
        if (focusedChoice == null) {
            return false;
        }
        selectedFocusId = focusedChoice.focusId();
        reelState.beginConfirming();
        phase = FlowPhase.COMMITTING;
        return true;
    }

    public void tick(
            ArchetypeReelState reelState,
            ResourceLocation syncedBoundArchetypeId,
            BookContext context,
            ArchetypeBindGateway bindGateway
    ) {
        if (!reelState.active()) {
            onClosed();
            return;
        }

        reelState.tick(syncedBoundArchetypeId);
        if (phase == FlowPhase.INACTIVE) {
            phase = FlowPhase.SELECTING;
        }

        if (reelState.readyToCloseAfterBoundReturn()) {
            phase = FlowPhase.COMPLETED;
            return;
        }

        if ((phase == FlowPhase.COMMITTING || phase == FlowPhase.REQUESTING_BIND) && reelState.readyToRequestBind()) {
            phase = FlowPhase.REQUESTING_BIND;
            if (selectedFocusId == null || selectedFocusId.isBlank()) {
                phase = FlowPhase.FAILED;
                reelState.abortToSelectable();
                return;
            }
            if (bindGateway.requestBind(context, selectedFocusId)) {
                reelState.markBindingRequestIssued();
                phase = FlowPhase.AWAITING_SYNC;
                awaitingSyncTicks = 0;
            } else {
                phase = FlowPhase.FAILED;
                reelState.abortToSelectable();
            }
            return;
        }

        if (phase == FlowPhase.AWAITING_SYNC) {
            awaitingSyncTicks++;
            if (awaitingSyncTicks >= SYNC_TIMEOUT_TICKS) {
                phase = FlowPhase.FAILED;
                reelState.abortToSelectable();
            }
        }
    }
}
