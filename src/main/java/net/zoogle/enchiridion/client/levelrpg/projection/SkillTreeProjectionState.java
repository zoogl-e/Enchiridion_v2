package net.zoogle.enchiridion.client.levelrpg.projection;

import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgJournalInteractionBridge;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Player-facing Enchiridion projection – interaction / animation state.</b>
 *
 * <p>Mutable, per-session state machine for the 3-D skill-tree overlay rendered inside the
 * Enchiridion book screen. It tracks the currently focused skill, camera rotation (yaw/pitch),
 * zoom level, drag inertia, hold-to-unlock progress, and visual fade alpha.
 *
 * <p>Lifecycle: created once and owned by {@code BookModeCoordinator}; {@link #open} is called
 * when the projection overlay is activated (e.g. the player opens a skill spread in the journal),
 * and {@link #close} is called when the overlay is dismissed. {@link #tick} must be called every
 * game tick to advance animations and cooldowns.
 *
 * <p>This class is completely internal to Enchiridion and does not depend on LevelRPG at compile
 * time. Skill data is fed in via {@link SkillTreeProjectionData}.
 *
 * @see SkillTreeProjectionData
 * @see LevelRpgJournalInteractionBridge
 */
public final class SkillTreeProjectionState {
    private static final float FADE_SPEED = 0.12f;
    private static final int NAVIGATION_COOLDOWN_TICKS = 4;
    private static final int HOLD_UNLOCK_TICKS = 24;
    private static final float ZOOM_MIN = 1.0f;
    private static final float ZOOM_MAX = 5.0f;
    private static final float CAMERA_OFFSET_LIMIT = 920.0f;
    private static final float CAMERA_EASE = 0.24f;

    private boolean active;
    private List<String> skills = List.of();
    private int focusedIndex;
    private int navigationCooldownTicks;
    private float visualAlpha;
    private int ticksActive;
    private int hoveredNodeIndex = -1;
    private int selectedNodeIndex = -1;
    private String statusMessage = "";
    private int statusMessageTicks;
    private boolean statusMessageSuccess;
    private boolean holdingUnlock;
    private int heldNodeIndex = -1;
    private int holdTicks;
    private int celebrationTicks;
    private float projectionYawDeg;
    private float projectionPitchDeg;
    private float zoomScale = 1.0f;
    private float cameraOffsetX;
    private float cameraOffsetY;
    private float cameraTargetOffsetX;
    private float cameraTargetOffsetY;
    private boolean rotatingByDrag;
    private float yawVelocityDegPerTick;
    private float pitchVelocityDegPerTick;

    public boolean open(List<String> availableSkills, String initialSkill) {
        if (availableSkills == null || availableSkills.isEmpty()) {
            return false;
        }
        List<String> normalized = new ArrayList<>();
        for (String skill : availableSkills) {
            if (skill != null && !skill.isBlank()) {
                normalized.add(skill);
            }
        }
        if (normalized.isEmpty()) {
            return false;
        }
        skills = List.copyOf(normalized);
        focusedIndex = initialIndex(initialSkill);
        navigationCooldownTicks = 0;
        active = true;
        visualAlpha = 0.0f;
        ticksActive = 0;
        hoveredNodeIndex = -1;
        selectedNodeIndex = -1;
        statusMessage = "";
        statusMessageTicks = 0;
        statusMessageSuccess = false;
        holdingUnlock = false;
        heldNodeIndex = -1;
        holdTicks = 0;
        celebrationTicks = 0;
        projectionYawDeg = 0.0f;
        projectionPitchDeg = -8.0f;
        zoomScale = 1.0f;
        cameraOffsetX = 0.0f;
        cameraOffsetY = 0.0f;
        cameraTargetOffsetX = 0.0f;
        cameraTargetOffsetY = 0.0f;
        rotatingByDrag = false;
        yawVelocityDegPerTick = 0.0f;
        pitchVelocityDegPerTick = 0.0f;
        return true;
    }

    public void close() {
        active = false;
        skills = List.of();
        focusedIndex = 0;
        navigationCooldownTicks = 0;
        visualAlpha = 0.0f;
        ticksActive = 0;
        hoveredNodeIndex = -1;
        selectedNodeIndex = -1;
        statusMessage = "";
        statusMessageTicks = 0;
        statusMessageSuccess = false;
        holdingUnlock = false;
        heldNodeIndex = -1;
        holdTicks = 0;
        celebrationTicks = 0;
        projectionYawDeg = 0.0f;
        projectionPitchDeg = -8.0f;
        zoomScale = 1.0f;
        cameraOffsetX = 0.0f;
        cameraOffsetY = 0.0f;
        cameraTargetOffsetX = 0.0f;
        cameraTargetOffsetY = 0.0f;
        rotatingByDrag = false;
        yawVelocityDegPerTick = 0.0f;
        pitchVelocityDegPerTick = 0.0f;
    }

    public void tick() {
        if (!active) {
            visualAlpha = Math.max(0.0f, visualAlpha - FADE_SPEED);
            return;
        }
        ticksActive++;
        visualAlpha = Math.min(1.0f, visualAlpha + FADE_SPEED);
        if (navigationCooldownTicks > 0) {
            navigationCooldownTicks--;
        }
        if (statusMessageTicks > 0) {
            statusMessageTicks--;
        }
        if (celebrationTicks > 0) {
            celebrationTicks--;
        }
        if (holdingUnlock && heldNodeIndex >= 0) {
            holdTicks++;
        }
        if (!rotatingByDrag) {
            projectionYawDeg += yawVelocityDegPerTick;
            projectionPitchDeg += pitchVelocityDegPerTick;
            yawVelocityDegPerTick *= 0.88f;
            pitchVelocityDegPerTick *= 0.88f;
            if (Math.abs(yawVelocityDegPerTick) < 0.01f) {
                yawVelocityDegPerTick = 0.0f;
            }
            if (Math.abs(pitchVelocityDegPerTick) < 0.01f) {
                pitchVelocityDegPerTick = 0.0f;
            }
        }
        cameraOffsetX += (cameraTargetOffsetX - cameraOffsetX) * CAMERA_EASE;
        cameraOffsetY += (cameraTargetOffsetY - cameraOffsetY) * CAMERA_EASE;
    }

    public boolean move(int direction) {
        if (!active || skills.isEmpty() || navigationCooldownTicks > 0 || direction == 0) {
            return false;
        }
        focusedIndex = Math.floorMod(focusedIndex + direction, skills.size());
        navigationCooldownTicks = NAVIGATION_COOLDOWN_TICKS;
        hoveredNodeIndex = -1;
        selectedNodeIndex = -1;
        cancelUnlockHold();
        projectionYawDeg = 0.0f;
        projectionPitchDeg = -8.0f;
        zoomScale = 1.0f;
        cameraOffsetX = 0.0f;
        cameraOffsetY = 0.0f;
        cameraTargetOffsetX = 0.0f;
        cameraTargetOffsetY = 0.0f;
        rotatingByDrag = false;
        yawVelocityDegPerTick = 0.0f;
        pitchVelocityDegPerTick = 0.0f;
        return true;
    }

    public boolean active() {
        return active;
    }

    public String focusedSkill() {
        if (skills.isEmpty()) {
            return "";
        }
        return skills.get(Math.max(0, Math.min(focusedIndex, skills.size() - 1)));
    }

    public float visualAlpha() {
        return visualAlpha;
    }

    public int ticksActive() {
        return ticksActive;
    }

    public int hoveredNodeIndex() {
        return hoveredNodeIndex;
    }

    public void setHoveredNodeIndex(int hoveredNodeIndex) {
        this.hoveredNodeIndex = hoveredNodeIndex;
    }

    public int selectedNodeIndex() {
        return selectedNodeIndex;
    }

    public void selectNodeIndex(int selectedNodeIndex) {
        this.selectedNodeIndex = selectedNodeIndex;
    }

    public void setStatusMessage(String statusMessage, boolean success) {
        this.statusMessage = statusMessage == null ? "" : statusMessage;
        this.statusMessageTicks = this.statusMessage.isBlank() ? 0 : 60;
        this.statusMessageSuccess = success;
    }

    public String statusMessage() {
        return statusMessageTicks > 0 ? statusMessage : "";
    }

    public boolean statusMessageSuccess() {
        return statusMessageSuccess;
    }

    public void beginUnlockHold(int nodeIndex) {
        if (!active || nodeIndex < 0) {
            return;
        }
        holdingUnlock = true;
        heldNodeIndex = nodeIndex;
        holdTicks = 0;
    }

    public void cancelUnlockHold() {
        holdingUnlock = false;
        heldNodeIndex = -1;
        holdTicks = 0;
    }

    public void onHoverChanged(int hoveredIndex) {
        if (holdingUnlock && hoveredIndex != heldNodeIndex) {
            cancelUnlockHold();
        }
    }

    public boolean holdingUnlock() {
        return holdingUnlock;
    }

    public int heldNodeIndex() {
        return heldNodeIndex;
    }

    public float holdProgress() {
        if (!holdingUnlock || heldNodeIndex < 0) {
            return 0.0f;
        }
        return Math.min(1.0f, holdTicks / (float) HOLD_UNLOCK_TICKS);
    }

    public boolean consumeUnlockReady() {
        if (!holdingUnlock || heldNodeIndex < 0 || holdTicks < HOLD_UNLOCK_TICKS) {
            return false;
        }
        cancelUnlockHold();
        return true;
    }

    public void triggerCelebration() {
        celebrationTicks = 24;
    }

    public float celebrationStrength() {
        if (celebrationTicks <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, celebrationTicks / 24.0f);
    }

    public void rotateByDrag(float deltaX, float deltaY) {
        rotatingByDrag = true;
        float speedScale = 1.0f / (0.90f + (zoomScale * 0.35f));
        float yawDelta = deltaX * 0.42f * speedScale;
        projectionYawDeg += yawDelta;
        float pitchDelta = -deltaY * 0.33f * speedScale;
        projectionPitchDeg += pitchDelta;
        yawVelocityDegPerTick = (yawVelocityDegPerTick * 0.42f) + (yawDelta * 0.58f);
        pitchVelocityDegPerTick = (pitchVelocityDegPerTick * 0.42f) + (pitchDelta * 0.58f);
    }

    public void endDragRotation() {
        rotatingByDrag = false;
    }

    public float projectionYawDeg() {
        return projectionYawDeg;
    }

    public float projectionPitchDeg() {
        return projectionPitchDeg;
    }

    public boolean adjustZoom(float scrollDelta) {
        if (!active || scrollDelta == 0.0f) {
            return false;
        }
        float next = clamp((float) (zoomScale * Math.pow(1.18f, scrollDelta)), ZOOM_MIN, ZOOM_MAX);
        boolean changed = next != zoomScale;
        zoomScale = next;
        return changed;
    }

    public float zoomScale() {
        return zoomScale;
    }

    public void setZoomScale(float zoomScale) {
        this.zoomScale = clamp(zoomScale, ZOOM_MIN, ZOOM_MAX);
    }

    public void addCameraOffset(float deltaX, float deltaY) {
        setCameraOffset(cameraTargetOffsetX + deltaX, cameraTargetOffsetY + deltaY);
    }

    public void addCameraOffsetImmediate(float deltaX, float deltaY) {
        setCameraOffset(cameraTargetOffsetX + deltaX, cameraTargetOffsetY + deltaY);
        cameraOffsetX = cameraTargetOffsetX;
        cameraOffsetY = cameraTargetOffsetY;
    }

    public void setCameraOffset(float x, float y) {
        cameraTargetOffsetX = clamp(x, -CAMERA_OFFSET_LIMIT, CAMERA_OFFSET_LIMIT);
        cameraTargetOffsetY = clamp(y, -CAMERA_OFFSET_LIMIT, CAMERA_OFFSET_LIMIT);
    }

    public float cameraOffsetX() {
        return cameraOffsetX;
    }

    public float cameraOffsetY() {
        return cameraOffsetY;
    }

    public float cameraTargetOffsetX() {
        return cameraTargetOffsetX;
    }

    public float cameraTargetOffsetY() {
        return cameraTargetOffsetY;
    }

    public void focusToward(float targetScreenX, float targetScreenY, float centerX, float centerY) {
        addCameraOffset(centerX - targetScreenX, centerY - targetScreenY);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private int initialIndex(String initialSkill) {
        if (initialSkill == null || initialSkill.isBlank()) {
            return 0;
        }
        for (int index = 0; index < skills.size(); index++) {
            if (initialSkill.equalsIgnoreCase(skills.get(index))) {
                return index;
            }
        }
        return 0;
    }
}
