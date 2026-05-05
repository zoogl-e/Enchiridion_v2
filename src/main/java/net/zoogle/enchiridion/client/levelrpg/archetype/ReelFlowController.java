package net.zoogle.enchiridion.client.levelrpg.archetype;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;

public final class ReelFlowController {
    private static final int SYNC_TIMEOUT_TICKS = 80;

    public enum FlowPhase {
        INACTIVE,
        SELECTING,
        REQUESTING_BIND,
        AWAITING_SYNC,
        COMPLETED,
        FAILED
    }

    private FlowPhase phase = FlowPhase.INACTIVE;
    private int awaitingSyncTicks;
    private String selectedFocusId;
    private long lastFeedbackVersionSeen;

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

    public boolean shouldRenderReel() {
        return phase == FlowPhase.SELECTING || phase == FlowPhase.FAILED;
    }

    public boolean confirmFocused(ArchetypeReelState reelState) {
        JournalArchetypeChoice focusedChoice = reelState.focusedChoice();
        if (focusedChoice == null) {
            return false;
        }
        selectedFocusId = focusedChoice.focusId();
        reelState.queueBindRequestFromFocused();
        phase = FlowPhase.REQUESTING_BIND;
        return true;
    }

    public boolean confirmChoice(ArchetypeReelState reelState, JournalArchetypeChoice choice) {
        if (choice == null || choice.focusId() == null || choice.focusId().isBlank()) {
            return false;
        }
        selectedFocusId = choice.focusId();
        reelState.queueBindRequestForChoice(choice);
        phase = FlowPhase.REQUESTING_BIND;
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

        if (phase == FlowPhase.REQUESTING_BIND && reelState.readyToRequestBind()) {
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
                ArchetypeBindFeedback feedback = bindGateway.latestFeedback();
                if (feedback != null) {
                    lastFeedbackVersionSeen = feedback.version();
                }
            } else {
                phase = FlowPhase.FAILED;
                reelState.abortToSelectable();
            }
            return;
        }

        if (phase == FlowPhase.AWAITING_SYNC) {
            ArchetypeBindFeedback feedback = bindGateway.latestFeedback();
            if (feedback != null && feedback.version() > lastFeedbackVersionSeen) {
                lastFeedbackVersionSeen = feedback.version();
                if (feedback.archetypeId() == null || selectedFocusId == null || selectedFocusId.equals(feedback.archetypeId().toString())) {
                    if (feedback.success()) {
                        phase = FlowPhase.COMPLETED;
                        return;
                    }
                    phase = FlowPhase.FAILED;
                    reelState.abortToSelectable();
                    return;
                }
            }
            awaitingSyncTicks++;
            if (awaitingSyncTicks >= SYNC_TIMEOUT_TICKS) {
                phase = FlowPhase.FAILED;
                reelState.abortToSelectable();
            }
        }
    }
}
