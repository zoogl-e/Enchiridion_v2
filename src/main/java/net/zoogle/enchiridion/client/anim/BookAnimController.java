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
        return requestOpen(BookAnimState.OPENING);
    }

    public boolean requestOpen(BookAnimState openState) {
        if (!isOpeningState(openState) || state != BookAnimState.CLOSED) {
            return false;
        }
        state = openState;
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
            case OPENING, OPENING_FRONT, OPENING_BACK -> {
                if (stateTime >= BookAnimationSpec.durationSeconds(state)) {
                    enterIdle();
                }
            }
            case CLOSING, CLOSING_FRONT, CLOSING_BACK -> {
                if (stateTime >= BookAnimationSpec.durationSeconds(state)) {
                    enterClosed();
                }
            }
            case FLIPPING_FRONT, FLIPPING_FRONT_TO_ORIGIN, FLIPPING_BACK, FLIPPING_BACK_TO_ORIGIN, FLIPPING_NEXT, FLIPPING_PREV, RIFFLING_NEXT, RIFFLING_PREV -> {
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

    public boolean requestFrontSpread() {
        if (state != BookAnimState.IDLE_OPEN) {
            return false;
        }
        pendingSpread = currentSpread;
        enterFlip(BookAnimState.FLIPPING_FRONT);
        return true;
    }

    public boolean requestFrontSpreadToOrigin() {
        if (state != BookAnimState.IDLE_OPEN) {
            return false;
        }
        pendingSpread = currentSpread;
        enterFlip(BookAnimState.FLIPPING_FRONT_TO_ORIGIN);
        return true;
    }

    public boolean requestBackSpread() {
        if (state != BookAnimState.IDLE_OPEN) {
            return false;
        }
        pendingSpread = currentSpread;
        enterFlip(BookAnimState.FLIPPING_BACK);
        return true;
    }

    public boolean requestBackSpreadToOrigin() {
        if (state != BookAnimState.IDLE_OPEN) {
            return false;
        }
        pendingSpread = currentSpread;
        enterFlip(BookAnimState.FLIPPING_BACK_TO_ORIGIN);
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

    public boolean requestDirectedRiffleToSpread(int targetSpread, int maxSpreadInclusive, boolean forward) {
        if (state != BookAnimState.IDLE_OPEN) {
            return false;
        }
        int clampedTarget = Math.max(0, Math.min(targetSpread, maxSpreadInclusive));
        if (clampedTarget == currentSpread) {
            return false;
        }

        pendingSpread = clampedTarget;
        enterFlip(forward ? BookAnimState.RIFFLING_NEXT : BookAnimState.RIFFLING_PREV);
        return true;
    }

    public boolean requestClose() {
        if (state == BookAnimState.CLOSED || state == BookAnimState.CLOSING) {
            return false;
        }
        return requestClose(BookAnimState.CLOSING);
    }

    public boolean requestClose(BookAnimState closeState) {
        if (!isCloseState(closeState)) {
            return false;
        }
        if (state == BookAnimState.CLOSED || isCloseState(state)) {
            return false;
        }
        state = closeState;
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

    public int getPendingSpread() {
        return pendingSpread;
    }

    public void setCurrentSpread(int currentSpread) {
        this.currentSpread = Math.max(0, currentSpread);
        this.pendingSpread = this.currentSpread;
        this.swappedThisState = false;
    }

    public float getNormalizedProgress() {
        float duration = switch (state) {
            case ARRIVING, OPENING, OPENING_FRONT, OPENING_BACK, CLOSING, CLOSING_FRONT, CLOSING_BACK,
                    FLIPPING_FRONT, FLIPPING_FRONT_TO_ORIGIN, FLIPPING_BACK, FLIPPING_BACK_TO_ORIGIN, FLIPPING_NEXT, FLIPPING_PREV, RIFFLING_NEXT, RIFFLING_PREV -> BookAnimationSpec.durationSeconds(state);
            default -> 1.0f;
        };
        return Math.clamp(stateTime / duration, 0.0f, 1.0f);
    }

    private static boolean isCloseState(BookAnimState state) {
        return state == BookAnimState.CLOSING
                || state == BookAnimState.CLOSING_FRONT
                || state == BookAnimState.CLOSING_BACK;
    }

    private static boolean isOpeningState(BookAnimState state) {
        return state == BookAnimState.OPENING
                || state == BookAnimState.OPENING_FRONT
                || state == BookAnimState.OPENING_BACK;
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
