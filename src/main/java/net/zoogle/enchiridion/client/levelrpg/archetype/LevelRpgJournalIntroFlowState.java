package net.zoogle.enchiridion.client.levelrpg.archetype;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgArchetypeBindingBridge;

import java.util.List;

public final class LevelRpgJournalIntroFlowState {
    private static final int BINDING_DURATION_TICKS = 20;
    private static final LevelRpgJournalIntroFlowState INSTANCE = new LevelRpgJournalIntroFlowState();

    private Phase phase = Phase.SEEKING_INITIATION;
    private String selectedFocusId;
    private int bindingTicksRemaining;
    private boolean bindRequestSent;

    public static LevelRpgJournalIntroFlowState get() {
        return INSTANCE;
    }

    public boolean isUnbound(BookContext context) {
        return !LevelRpgArchetypeBindingBridge.isBookBound(context);
    }

    public void syncToTruth(BookContext context) {
        if (!isUnbound(context)) {
            reset();
        } else if (phase == null) {
            phase = Phase.SEEKING_INITIATION;
        }
    }

    public Phase phase() {
        return phase;
    }

    public String selectedFocusId() {
        return selectedFocusId;
    }

    public boolean beginSelection(BookContext context) {
        syncToTruth(context);
        List<JournalArchetypeChoice> choices = LevelRpgArchetypeBindingBridge.availableArchetypes();
        if (choices.isEmpty()) {
            return false;
        }
        phase = Phase.SELECTING_ARCHETYPE;
        if (selectedFocusId == null || selectedFocusId.isBlank()) {
            selectedFocusId = choices.getFirst().focusId();
        }
        return true;
    }

    public void setSelectedFocus(String focusId) {
        if (focusId != null && !focusId.isBlank()) {
            selectedFocusId = focusId;
        }
    }

    public void beginBinding(BookContext context, String focusId) {
        syncToTruth(context);
        selectedFocusId = focusId;
        phase = Phase.BINDING_ARCHETYPE;
        bindingTicksRemaining = BINDING_DURATION_TICKS;
        bindRequestSent = false;
    }

    public boolean tick(BookContext context) {
        boolean unbound = isUnbound(context);
        if (!unbound) {
            boolean changed = phase != Phase.SEEKING_INITIATION || selectedFocusId != null || bindRequestSent || bindingTicksRemaining > 0;
            reset();
            return changed;
        }
        syncToTruth(context);
        if (phase == Phase.BINDING_ARCHETYPE) {
            if (bindingTicksRemaining > 0) {
                bindingTicksRemaining--;
                return false;
            }
            if (!bindRequestSent && selectedFocusId != null && LevelRpgArchetypeBindingBridge.requestBindArchetype(context, selectedFocusId)) {
                bindRequestSent = true;
                phase = Phase.WAITING_FOR_BIND_SYNC;
                return true;
            }
            phase = Phase.SELECTING_ARCHETYPE;
            return true;
        }
        return false;
    }

    public void reset() {
        phase = Phase.SEEKING_INITIATION;
        selectedFocusId = null;
        bindingTicksRemaining = 0;
        bindRequestSent = false;
    }

    public boolean shouldResetOnClose(BookContext context) {
        return isUnbound(context) && phase != Phase.SEEKING_INITIATION;
    }

    public boolean interactionsLocked() {
        return phase == Phase.BINDING_ARCHETYPE || phase == Phase.WAITING_FOR_BIND_SYNC;
    }

    public JournalArchetypeChoice selectedChoice() {
        if (selectedFocusId == null || selectedFocusId.isBlank()) {
            return null;
        }
        for (JournalArchetypeChoice choice : LevelRpgArchetypeBindingBridge.availableArchetypes()) {
            if (selectedFocusId.equals(choice.focusId())) {
                return choice;
            }
        }
        return null;
    }

    public ResourceLocation selectedArchetypeId(BookContext context) {
        ResourceLocation id = LevelRpgArchetypeBindingBridge.selectedArchetypeId(context);
        if (id != null) {
            return id;
        }
        if (selectedFocusId == null || selectedFocusId.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(selectedFocusId);
        } catch (Exception exception) {
            return null;
        }
    }

    enum Phase {
        SEEKING_INITIATION,
        SELECTING_ARCHETYPE,
        BINDING_ARCHETYPE,
        WAITING_FOR_BIND_SYNC
    }
}
