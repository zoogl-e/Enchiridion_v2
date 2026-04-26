package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ArchetypeReelState {
    private static final float MOVE_SMOOTHING = 0.22f;
    private static final float MOVE_SNAP_DISTANCE = 0.025f;
    private static final float FOCUS_TILT_SMOOTHING = 0.18f;
    private static final float MAX_FOCUS_TILT_YAW = 6.0f;
    private static final float MAX_FOCUS_TILT_PITCH = 4.0f;
    private static final int CONFIRMING_TICKS = 8;
    private static final int BURNING_TICKS = 18;
    private static final int RETURN_TICKS = 12;
    private static final int ENTRANCE_TICKS = 10;
    /** Min ticks between keyboard reel steps (reduces GLFW key-repeat skipping multiple cards). */
    private static final int KEYBOARD_NAV_COOLDOWN_TICKS = 4;

    private boolean active;
    private List<JournalArchetypeChoice> entries = List.of();
    private int focusedIndex;
    private int settledFocusedIndex;
    private int previousFocusedIndex;
    private int hoveredIndex = -1;
    private float animatedFocusPosition;
    private float transitionProgress = 1.0f;
    private float transitionDistance = 1.0f;
    private float focusedTiltYaw;
    private float focusedTiltPitch;
    private int phaseTick;
    private boolean bindRequestIssued;
    private Phase phase = Phase.IDLE;
    private ResourceLocation boundArchetypeId;
    private ResourceLocation selectedArchetypeId;
    private int entranceTick;
    private int keyboardNavigateCooldownTicks;

    public enum Phase {
        IDLE,
        MOVING,
        CONFIRMING,
        BURNING,
        BOUND_RETURN
    }

    public boolean open(List<JournalArchetypeChoice> entries, @Nullable ResourceLocation boundArchetypeId, @Nullable ResourceLocation selectedArchetypeId) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        this.active = true;
        this.entries = List.copyOf(new ArrayList<>(entries));
        this.boundArchetypeId = boundArchetypeId;
        this.selectedArchetypeId = selectedArchetypeId;
        this.focusedIndex = preferredIndex(boundArchetypeId, selectedArchetypeId);
        this.settledFocusedIndex = this.focusedIndex;
        this.previousFocusedIndex = this.focusedIndex;
        this.animatedFocusPosition = this.focusedIndex;
        this.hoveredIndex = this.focusedIndex;
        this.transitionProgress = 1.0f;
        this.transitionDistance = 1.0f;
        this.focusedTiltYaw = 0.0f;
        this.focusedTiltPitch = 0.0f;
        this.phaseTick = 0;
        this.bindRequestIssued = false;
        this.phase = Phase.IDLE;
        this.entranceTick = 0;
        this.keyboardNavigateCooldownTicks = 0;
        return true;
    }

    public void close() {
        active = false;
        hoveredIndex = -1;
        phase = Phase.IDLE;
        phaseTick = 0;
        bindRequestIssued = false;
        focusedTiltYaw = 0.0f;
        focusedTiltPitch = 0.0f;
        entranceTick = 0;
        keyboardNavigateCooldownTicks = 0;
    }

    public void tick(@Nullable ResourceLocation syncedBoundArchetypeId) {
        if (!active || entries.isEmpty()) {
            return;
        }

        if (keyboardNavigateCooldownTicks > 0) {
            keyboardNavigateCooldownTicks--;
        }

        if (entranceTick < ENTRANCE_TICKS) {
            entranceTick++;
        }

        updateMovement();
        if (syncedBoundArchetypeId != null) {
            boundArchetypeId = syncedBoundArchetypeId;
            if (selectedArchetypeId != null && selectedArchetypeId.equals(syncedBoundArchetypeId)
                    && (phase == Phase.BURNING || phase == Phase.CONFIRMING)) {
                phase = Phase.BOUND_RETURN;
                phaseTick = 0;
            }
        }

        switch (phase) {
            case CONFIRMING -> {
                phaseTick++;
                transitionProgress = Math.min(1.0f, phaseTick / (float) CONFIRMING_TICKS);
                if (phaseTick >= CONFIRMING_TICKS) {
                    phase = Phase.BURNING;
                    phaseTick = 0;
                }
            }
            case BURNING -> {
                phaseTick++;
                transitionProgress = Math.min(1.0f, phaseTick / (float) BURNING_TICKS);
            }
            case BOUND_RETURN -> {
                phaseTick++;
                transitionProgress = Math.min(1.0f, phaseTick / (float) RETURN_TICKS);
            }
            default -> {
            }
        }
    }

    public void updateFocusTilt(float anchorX, float anchorY, double mouseX, double mouseY) {
        if (!active) {
            focusedTiltYaw = approach(focusedTiltYaw, 0.0f, FOCUS_TILT_SMOOTHING);
            focusedTiltPitch = approach(focusedTiltPitch, 0.0f, FOCUS_TILT_SMOOTHING);
            return;
        }
        float offsetX = clamp((float) ((mouseX - anchorX) / 180.0f), -1.0f, 1.0f);
        float offsetY = clamp((float) ((mouseY - anchorY) / 180.0f), -1.0f, 1.0f);
        focusedTiltYaw = approach(focusedTiltYaw, offsetX * MAX_FOCUS_TILT_YAW, FOCUS_TILT_SMOOTHING);
        focusedTiltPitch = approach(focusedTiltPitch, -offsetY * MAX_FOCUS_TILT_PITCH, FOCUS_TILT_SMOOTHING);
    }

    public boolean active() {
        return active;
    }

    public List<JournalArchetypeChoice> entries() {
        return entries;
    }

    public List<JournalArchetypeChoice> choices() {
        return entries();
    }

    public int focusedIndex() {
        return focusedIndex;
    }

    public int settledFocusedIndex() {
        return settledFocusedIndex;
    }

    public int previousFocusedIndex() {
        return previousFocusedIndex;
    }

    public float animatedFocusPosition() {
        return animatedFocusPosition;
    }

    public float transitionProgress() {
        return transitionProgress;
    }

    /**
     * Multiplier for reel card and overlay opacity (entrance ease-in, fade during burn/return).
     */
    public float globalPresentationAlpha() {
        float entrance = Math.min(1.0f, entranceTick / (float) ENTRANCE_TICKS);
        return switch (phase) {
            case BURNING -> entrance * (1.0f - 0.78f * transitionProgress);
            case BOUND_RETURN -> entrance * Math.max(0.0f, 0.22f * (1.0f - transitionProgress));
            default -> entrance;
        };
    }

    /**
     * How much to lower the book while the reel is shown; eases in on open and out during bound return.
     * Pass the render {@code partialTick} for smooth sub-tick interpolation; use 0 for non-render contexts.
     */
    public float reelBookLowerBlend(float partialTick) {
        float entrance = Math.min(1.0f, (entranceTick + partialTick) / (float) ENTRANCE_TICKS);
        float exit = phase == Phase.BOUND_RETURN ? (1.0f - transitionProgress) : 1.0f;
        return entrance * exit;
    }

    public Phase phase() {
        return phase;
    }

    public float focusedTiltYaw() {
        return focusedTiltYaw;
    }

    public float focusedTiltPitch() {
        return focusedTiltPitch;
    }

    public boolean bindRequestIssued() {
        return bindRequestIssued;
    }

    public void markBindingRequestIssued() {
        bindRequestIssued = true;
    }

    public boolean readyToRequestBind() {
        return phase == Phase.BURNING && !bindRequestIssued && transitionProgress >= 0.6f;
    }

    public boolean readyToCloseAfterBoundReturn() {
        return phase == Phase.BOUND_RETURN && transitionProgress >= 1.0f;
    }

    public void beginConfirming() {
        if (!active || focusedChoice() == null) {
            return;
        }
        selectedArchetypeId = focusedArchetypeId();
        phase = Phase.CONFIRMING;
        phaseTick = 0;
        bindRequestIssued = false;
        transitionProgress = 0.0f;
    }

    public void abortToSelectable() {
        if (!active) {
            return;
        }
        phase = Phase.IDLE;
        phaseTick = 0;
        bindRequestIssued = false;
        transitionProgress = 1.0f;
    }

    public void move(int direction) {
        if (!active || entries.isEmpty() || phase != Phase.IDLE) {
            return;
        }
        if (keyboardNavigateCooldownTicks > 0) {
            return;
        }
        int next = Math.floorMod(focusedIndex + direction, entries.size());
        if (next == focusedIndex) {
            return;
        }
        keyboardNavigateCooldownTicks = KEYBOARD_NAV_COOLDOWN_TICKS;
        setFocusIndex(next, false);
    }

    public void setHoveredIndex(int hoveredIndex) {
        this.hoveredIndex = hoveredIndex;
    }

    public int hoveredIndex() {
        return hoveredIndex;
    }

    public void focusIndex(int index) {
        if (!active || index < 0 || index >= entries.size() || phase != Phase.IDLE) {
            return;
        }
        setFocusIndex(index, false);
    }

    public JournalArchetypeChoice focusedChoice() {
        if (!active || entries.isEmpty()) {
            return null;
        }
        return entries.get(settledFocusedIndex);
    }

    public @Nullable ResourceLocation hoveredArchetypeId() {
        if (!active || hoveredIndex < 0 || hoveredIndex >= entries.size()) {
            return null;
        }
        return entries.get(hoveredIndex).archetypeId();
    }

    public @Nullable ResourceLocation focusedArchetypeId() {
        JournalArchetypeChoice focusedChoice = focusedChoice();
        return focusedChoice == null ? null : focusedChoice.archetypeId();
    }

    public @Nullable ResourceLocation selectedArchetypeId() {
        return selectedArchetypeId;
    }

    public @Nullable ResourceLocation boundArchetypeId() {
        return boundArchetypeId;
    }

    public void markSelected(@Nullable ResourceLocation archetypeId) {
        this.selectedArchetypeId = archetypeId;
    }

    private void setFocusIndex(int index, boolean snapCarousel) {
        if (index == focusedIndex) {
            return;
        }
        previousFocusedIndex = focusedIndex;
        focusedIndex = index;
        hoveredIndex = index;
        if (snapCarousel) {
            animatedFocusPosition = index;
            settledFocusedIndex = index;
            transitionDistance = 1.0f;
            transitionProgress = 1.0f;
            phase = Phase.IDLE;
            phaseTick = 0;
        } else {
            transitionDistance = Math.max(1.0f, Math.abs(wrappedDelta(animatedFocusPosition, focusedIndex, entries.size())));
            transitionProgress = 0.0f;
            phase = Phase.MOVING;
            phaseTick = 0;
        }
    }

    private void updateMovement() {
        float delta = wrappedDelta(animatedFocusPosition, focusedIndex, entries.size());
        animatedFocusPosition += delta * MOVE_SMOOTHING;
        float remaining = Math.abs(wrappedDelta(animatedFocusPosition, focusedIndex, entries.size()));
        if (remaining < MOVE_SNAP_DISTANCE) {
            animatedFocusPosition = focusedIndex;
            if (phase == Phase.MOVING) {
                settledFocusedIndex = focusedIndex;
                phase = Phase.IDLE;
                transitionProgress = 1.0f;
            }
        } else if (phase == Phase.MOVING) {
            transitionProgress = clamp(1.0f - (remaining / Math.max(0.001f, transitionDistance)), 0.0f, 1.0f);
        }
    }

    private int preferredIndex(@Nullable ResourceLocation boundArchetypeId, @Nullable ResourceLocation selectedArchetypeId) {
        ResourceLocation preferred = selectedArchetypeId != null ? selectedArchetypeId : boundArchetypeId;
        if (preferred == null) {
            return 0;
        }
        for (int index = 0; index < entries.size(); index++) {
            if (preferred.equals(entries.get(index).archetypeId())) {
                return index;
            }
        }
        return 0;
    }

    private static float wrappedDelta(float current, float target, int size) {
        if (size <= 0) {
            return 0.0f;
        }
        float direct = target - current;
        float wrappedPositive = direct + size;
        float wrappedNegative = direct - size;
        float best = direct;
        if (Math.abs(wrappedPositive) < Math.abs(best)) {
            best = wrappedPositive;
        }
        if (Math.abs(wrappedNegative) < Math.abs(best)) {
            best = wrappedNegative;
        }
        return best;
    }

    private static float approach(float current, float target, float smoothing) {
        return current + ((target - current) * smoothing);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
