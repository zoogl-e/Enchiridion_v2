package net.zoogle.enchiridion.client.anim;

import java.util.EnumMap;
import java.util.Map;

public final class BookAnimationSpec {
    private static final float ARRIVAL_DURATION_SECONDS = 0.58f;
    private static final float OPEN_DURATION_SECONDS = 1.35f;
    private static final float CLOSE_DURATION_SECONDS = 1.0f;
    private static final float FLIP_DURATION_SECONDS = 0.56f;
    private static final float RIFFLE_DURATION_SECONDS = 0.76f;
    private static final float IDLE_LOOP_DURATION_SECONDS = 4.0f;
    private static final float FLIP_PAGE_SWAP_PROGRESS = 0.5f;
    private static final float HOVER_SMOOTHING = 0.18f;
    private static final float TILT_SMOOTHING = 0.12f;
    private static final float INSPECT_SMOOTHING = 0.16f;
    private static final Map<BookAnimState, Clip> CLIPS = createClips();

    private BookAnimationSpec() {}

    public static Clip clipFor(BookAnimState state) {
        return CLIPS.getOrDefault(state, CLIPS.get(BookAnimState.CLOSED));
    }

    public static float durationSeconds(BookAnimState state) {
        return clipFor(state).durationSeconds();
    }

    public static float flipPageSwapProgress() {
        return FLIP_PAGE_SWAP_PROGRESS;
    }

    public static float hoverSmoothing() {
        return HOVER_SMOOTHING;
    }

    public static float tiltSmoothing() {
        return TILT_SMOOTHING;
    }

    public static float inspectSmoothing() {
        return INSPECT_SMOOTHING;
    }

    public static boolean hasDedicatedVisualClip(BookAnimState state) {
        return clipFor(state).dedicatedVisualClip();
    }

    public static boolean usesClosedIdleFallback(BookAnimState state) {
        return state == BookAnimState.CLOSING && !clipFor(state).dedicatedVisualClip();
    }

    public static float presentationProgress(BookAnimState state, float rawProgress) {
        float progress = Math.clamp(rawProgress, 0.0f, 1.0f);
        return switch (state) {
            case OPENING -> easeOutCubic(progress);
            case CLOSING -> smoothStep(progress);
            default -> progress;
        };
    }

    private static Map<BookAnimState, Clip> createClips() {
        EnumMap<BookAnimState, Clip> clips = new EnumMap<>(BookAnimState.class);
        clips.put(BookAnimState.ARRIVING, new Clip("animation.model.slam", ARRIVAL_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.CLOSED, new Clip("animation.model.idle_closed", IDLE_LOOP_DURATION_SECONDS, PlaybackMode.LOOP, true));
        clips.put(BookAnimState.CLOSING, new Clip("animation.model.close", CLOSE_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.OPENING, new Clip("animation.model.open", OPEN_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.IDLE_OPEN, new Clip("animation.model.idle_open", IDLE_LOOP_DURATION_SECONDS, PlaybackMode.LOOP, true));
        clips.put(BookAnimState.IDLE_SKILLTREE, new Clip("animation.model.idle_skilltree", IDLE_LOOP_DURATION_SECONDS, PlaybackMode.LOOP, true));
        clips.put(BookAnimState.FLIPPING_NEXT, new Clip("animation.model.flip_right", FLIP_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.FLIPPING_PREV, new Clip("animation.model.flip_left", FLIP_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.FLIPPING_NEXT_SKILLTREE, new Clip("animation.model.flip_right_skilltree", FLIP_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.FLIPPING_PREV_SKILLTREE, new Clip("animation.model.flip_left_skilltree", FLIP_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.RIFFLING_NEXT, new Clip("animation.model.riffle_right", RIFFLE_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.RIFFLING_PREV, new Clip("animation.model.riffle_left", RIFFLE_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.RIFFLING_NEXT_SKILLTREE, new Clip("animation.model.riffle_right_skilltree", RIFFLE_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        clips.put(BookAnimState.RIFFLING_PREV_SKILLTREE, new Clip("animation.model.riffle_left_skilltree", RIFFLE_DURATION_SECONDS, PlaybackMode.HOLD_ON_LAST_FRAME, true));
        return clips;
    }

    private static float smoothStep(float progress) {
        return progress * progress * (3.0f - (2.0f * progress));
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0f - progress;
        return 1.0f - (inverse * inverse * inverse);
    }

    public record Clip(String geckoName, float durationSeconds, PlaybackMode playbackMode, boolean dedicatedVisualClip) {}

    public enum PlaybackMode {
        LOOP,
        HOLD_ON_LAST_FRAME
    }
}
