package net.zoogle.enchiridion.client.anim;

import java.util.EnumMap;
import java.util.Map;

public final class BookAnimationSpec {
    private static final float FLIP_PAGE_SWAP_PROGRESS = 0.5f;
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

    private static Map<BookAnimState, Clip> createClips() {
        EnumMap<BookAnimState, Clip> clips = new EnumMap<>(BookAnimState.class);
        clips.put(BookAnimState.ARRIVING, new Clip("animation.model.slam", 0.5f, PlaybackMode.HOLD_ON_LAST_FRAME));
        clips.put(BookAnimState.CLOSED, new Clip("animation.model.idle_closed", 4.0f, PlaybackMode.LOOP));
        clips.put(BookAnimState.CLOSING, new Clip("animation.model.idle_closed", 0.55f, PlaybackMode.LOOP));
        clips.put(BookAnimState.OPENING, new Clip("animation.model.open", 1.5f, PlaybackMode.HOLD_ON_LAST_FRAME));
        clips.put(BookAnimState.IDLE_OPEN, new Clip("animation.model.idle_open", 4.0f, PlaybackMode.LOOP));
        clips.put(BookAnimState.FLIPPING_NEXT, new Clip("animation.model.flip_right", 0.5f, PlaybackMode.HOLD_ON_LAST_FRAME));
        clips.put(BookAnimState.FLIPPING_PREV, new Clip("animation.model.flip_left", 0.5f, PlaybackMode.HOLD_ON_LAST_FRAME));
        clips.put(BookAnimState.RIFFLING_NEXT, new Clip("animation.model.riffle_right", 0.7f, PlaybackMode.HOLD_ON_LAST_FRAME));
        clips.put(BookAnimState.RIFFLING_PREV, new Clip("animation.model.riffle_left", 0.7f, PlaybackMode.HOLD_ON_LAST_FRAME));
        return clips;
    }

    public record Clip(String geckoName, float durationSeconds, PlaybackMode playbackMode) {}

    public enum PlaybackMode {
        LOOP,
        HOLD_ON_LAST_FRAME
    }
}
