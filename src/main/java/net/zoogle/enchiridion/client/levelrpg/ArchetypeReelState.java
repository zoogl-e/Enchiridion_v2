package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ArchetypeReelState {
    private static final float MOVE_SMOOTHING = 0.36f;
    private static final float MOVE_SNAP_DISTANCE = 0.025f;
    private static final float INERTIA_DAMPING = 0.84f;
    private static final float INERTIA_SNAP_FORCE = 0.18f;
    private static final float INERTIA_STOP_SPEED = 0.003f;
    private static final float INERTIA_STOP_OFFSET = 0.03f;
    private static final float DRAG_PIXELS_PER_SLOT = 84.0f;
    private static final float FOCUS_TILT_SMOOTHING = 0.18f;
    private static final float MAX_FOCUS_TILT_YAW = 6.0f;
    private static final float MAX_FOCUS_TILT_PITCH = 4.0f;
    private static final int RETURN_TICKS = 12;
    private static final int ENTRANCE_TICKS = 10;
    /** Min ticks between keyboard reel steps (reduces GLFW key-repeat skipping multiple cards). */
    private static final int KEYBOARD_NAV_COOLDOWN_TICKS = 1;

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
    private boolean dragging;
    private int draggedIndex = -1;
    private boolean dropTargetHovered;
    private boolean bindRequestQueued;
    private float dragMouseX;
    private float dragMouseY;
    private float dragFrontBlend;
    private float dragLastMouseX;
    private boolean dragMouseInitialized;
    private float inertialVelocity;

    public enum Phase {
        IDLE,
        MOVING,
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
        this.bindRequestQueued = false;
        this.dragFrontBlend = 0.0f;
        this.dragLastMouseX = 0.0f;
        this.dragMouseInitialized = false;
        this.inertialVelocity = 0.0f;
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
        dragging = false;
        draggedIndex = -1;
        dropTargetHovered = false;
        bindRequestQueued = false;
        dragFrontBlend = 0.0f;
        dragLastMouseX = 0.0f;
        dragMouseInitialized = false;
        inertialVelocity = 0.0f;
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
        float targetDragBlend = dragging ? 1.0f : 0.0f;
        dragFrontBlend = approach(dragFrontBlend, targetDragBlend, 0.24f);

        updateMovement();
        if (syncedBoundArchetypeId != null) {
            boundArchetypeId = syncedBoundArchetypeId;
            if (selectedArchetypeId != null && selectedArchetypeId.equals(syncedBoundArchetypeId)
                    && bindRequestIssued) {
                phase = Phase.BOUND_RETURN;
                phaseTick = 0;
            }
        }

        switch (phase) {
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
        bindRequestQueued = false;
    }

    public boolean readyToRequestBind() {
        return phase == Phase.IDLE && bindRequestQueued && !bindRequestIssued;
    }

    public boolean readyToCloseAfterBoundReturn() {
        return phase == Phase.BOUND_RETURN && transitionProgress >= 1.0f;
    }

    public void queueBindRequestFromFocused() {
        if (!active || focusedChoice() == null) {
            return;
        }
        selectedArchetypeId = focusedArchetypeId();
        phase = Phase.IDLE;
        bindRequestIssued = false;
        bindRequestQueued = true;
        transitionProgress = 1.0f;
    }

    public void queueBindRequestForChoice(@Nullable JournalArchetypeChoice choice) {
        if (!active || choice == null || choice.archetypeId() == null) {
            return;
        }
        for (int index = 0; index < entries.size(); index++) {
            JournalArchetypeChoice entry = entries.get(index);
            if (choice.archetypeId().equals(entry.archetypeId())) {
                focusedIndex = index;
                settledFocusedIndex = index;
                hoveredIndex = index;
                animatedFocusPosition = index;
                break;
            }
        }
        selectedArchetypeId = choice.archetypeId();
        phase = Phase.IDLE;
        bindRequestIssued = false;
        bindRequestQueued = true;
        transitionProgress = 1.0f;
    }

    public void abortToSelectable() {
        if (!active) {
            return;
        }
        phase = Phase.IDLE;
        phaseTick = 0;
        bindRequestIssued = false;
        bindRequestQueued = false;
        transitionProgress = 1.0f;
        endDrag();
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

    public boolean beginDrag(int index) {
        if (!active || phase != Phase.IDLE || index < 0 || index >= entries.size()) {
            return false;
        }
        setFocusIndex(index, false);
        dragging = true;
        draggedIndex = index;
        dropTargetHovered = false;
        dragMouseX = 0.0f;
        dragMouseY = 0.0f;
        dragLastMouseX = 0.0f;
        dragMouseInitialized = false;
        inertialVelocity = 0.0f;
        return true;
    }

    public boolean dragging() {
        return dragging;
    }

    public int draggedIndex() {
        return draggedIndex;
    }

    public void setDropTargetHovered(boolean hovered) {
        dropTargetHovered = hovered;
    }

    public boolean dropTargetHovered() {
        return dropTargetHovered;
    }

    public void setDragMouse(float mouseX, float mouseY) {
        if (dragging) {
            if (!dragMouseInitialized) {
                dragLastMouseX = mouseX;
                dragMouseInitialized = true;
            } else {
                float deltaX = mouseX - dragLastMouseX;
                dragLastMouseX = mouseX;
                float slotDelta = -deltaX / DRAG_PIXELS_PER_SLOT;
                if (Math.abs(slotDelta) > 0.0001f) {
                    animatedFocusPosition += slotDelta;
                    inertialVelocity = (inertialVelocity * 0.55f) + (slotDelta * 0.45f);
                    updateFocusFromAnimatedPosition();
                    transitionProgress = 0.0f;
                    phase = Phase.MOVING;
                }
            }
        }
        dragMouseX = mouseX;
        dragMouseY = mouseY;
    }

    public float dragMouseX() {
        return dragMouseX;
    }

    public float dragMouseY() {
        return dragMouseY;
    }

    public JournalArchetypeChoice draggedChoice() {
        if (!dragging || draggedIndex < 0 || draggedIndex >= entries.size()) {
            return null;
        }
        return entries.get(draggedIndex);
    }

    public float dragFrontBlend() {
        return dragFrontBlend;
    }

    public void endDrag() {
        dragging = false;
        draggedIndex = -1;
        dropTargetHovered = false;
        dragMouseX = 0.0f;
        dragMouseY = 0.0f;
        dragLastMouseX = 0.0f;
        dragMouseInitialized = false;
    }

    public void snapToNearestFocus() {
        if (entries.isEmpty()) {
            return;
        }
        float nearest = Math.round(animatedFocusPosition);
        animatedFocusPosition = nearest;
        inertialVelocity = 0.0f;
        updateFocusFromAnimatedPosition();
        settledFocusedIndex = focusedIndex;
        phase = Phase.IDLE;
        phaseTick = 0;
        transitionProgress = 1.0f;
        transitionDistance = 1.0f;
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
        if (entries.isEmpty()) {
            return;
        }
        if (dragging) {
            return;
        }
        if (Math.abs(inertialVelocity) > 0.0001f) {
            inertialVelocity *= INERTIA_DAMPING;
            animatedFocusPosition += inertialVelocity;
            float nearest = Math.round(animatedFocusPosition);
            float snapDelta = wrappedDelta(animatedFocusPosition, nearest, entries.size());
            animatedFocusPosition += snapDelta * INERTIA_SNAP_FORCE;
            updateFocusFromAnimatedPosition();
            transitionProgress = 0.0f;
            phase = Phase.MOVING;
            if (Math.abs(inertialVelocity) < INERTIA_STOP_SPEED && Math.abs(snapDelta) < INERTIA_STOP_OFFSET) {
                snapToNearestFocus();
            }
            return;
        }
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

    private void updateFocusFromAnimatedPosition() {
        if (entries.isEmpty()) {
            return;
        }
        int nearestIndex = Math.floorMod(Math.round(animatedFocusPosition), entries.size());
        focusedIndex = nearestIndex;
        if (hoveredIndex < 0) {
            hoveredIndex = nearestIndex;
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
