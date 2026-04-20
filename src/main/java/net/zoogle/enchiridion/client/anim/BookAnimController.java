package net.zoogle.enchiridion.client.anim;

public final class BookAnimController {
    private BookAnimState state = BookAnimState.ARRIVING;
    private float stateTime = 0.0f;
    private int currentSpread = 0;
    private int pendingSpread = 0;
    private boolean swappedThisState = false;

    public void beginArrival() {
        state = BookAnimState.ARRIVING;
        stateTime = 0.0f;
        swappedThisState = false;
    }

    public boolean requestOpen() {
        if (state != BookAnimState.CLOSED) {
            return false;
        }
        state = BookAnimState.OPENING;
        stateTime = 0.0f;
        swappedThisState = false;
        return true;
    }

    public void tick() {
        tick(1.0f / 20.0f);
    }

    public void tick(float deltaSeconds) {
        stateTime += deltaSeconds;

        switch (state) {
            case ARRIVING -> {
                if (stateTime >= BookAnimationSpec.durationSeconds(BookAnimState.ARRIVING)) {
                    enterClosed();
                }
            }
            case OPENING -> {
                if (stateTime >= BookAnimationSpec.durationSeconds(BookAnimState.OPENING)) {
                    enterIdle();
                }
            }
            case CLOSING -> {
                if (stateTime >= BookAnimationSpec.durationSeconds(BookAnimState.CLOSING)) {
                    enterClosed();
                }
            }
            case FLIPPING_NEXT, FLIPPING_PREV, RIFFLING_NEXT, RIFFLING_PREV -> {
                float progress = getNormalizedProgress();
                if (!swappedThisState && progress >= BookAnimationSpec.flipPageSwapProgress()) {
                    currentSpread = pendingSpread;
                    swappedThisState = true;
                }
                if (stateTime >= BookAnimationSpec.durationSeconds(state)) {
                    enterIdle();
                }
            }
            default -> {
            }
        }
    }

    public boolean requestNextSpread(int maxSpreadInclusive) {
        if (state != BookAnimState.IDLE_OPEN || currentSpread >= maxSpreadInclusive) {
            return false;
        }
        pendingSpread = currentSpread + 1;
        enterFlip(BookAnimState.FLIPPING_NEXT);
        return true;
    }

    public boolean requestPreviousSpread() {
        if (state != BookAnimState.IDLE_OPEN || currentSpread <= 0) {
            return false;
        }
        pendingSpread = currentSpread - 1;
        enterFlip(BookAnimState.FLIPPING_PREV);
        return true;
    }

    public boolean requestJumpToSpread(int targetSpread, int maxSpreadInclusive) {
        if (state != BookAnimState.IDLE_OPEN) {
            return false;
        }
        int clampedTarget = Math.max(0, Math.min(targetSpread, maxSpreadInclusive));
        if (clampedTarget == currentSpread) {
            return false;
        }

        pendingSpread = clampedTarget;
        enterFlip(clampedTarget > currentSpread ? BookAnimState.RIFFLING_NEXT : BookAnimState.RIFFLING_PREV);
        return true;
    }

    public boolean requestClose() {
        if (state == BookAnimState.CLOSED || state == BookAnimState.CLOSING) {
            return false;
        }
        state = BookAnimState.CLOSING;
        stateTime = 0.0f;
        swappedThisState = false;
        pendingSpread = currentSpread;
        return true;
    }

    public BookAnimState getState() {
        return state;
    }

    public int getCurrentSpread() {
        return currentSpread;
    }

    public float getNormalizedProgress() {
        float duration = switch (state) {
            case ARRIVING, OPENING, CLOSING, FLIPPING_NEXT, FLIPPING_PREV, RIFFLING_NEXT, RIFFLING_PREV -> BookAnimationSpec.durationSeconds(state);
            default -> 1.0f;
        };
        return Math.clamp(stateTime / duration, 0.0f, 1.0f);
    }

    private void enterFlip(BookAnimState nextState) {
        state = nextState;
        stateTime = 0.0f;
        swappedThisState = false;
    }

    private void enterIdle() {
        state = BookAnimState.IDLE_OPEN;
        stateTime = 0.0f;
        swappedThisState = false;
    }

    private void enterClosed() {
        state = BookAnimState.CLOSED;
        stateTime = 0.0f;
        swappedThisState = false;
    }
}
