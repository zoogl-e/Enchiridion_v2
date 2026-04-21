package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimController;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import org.jetbrains.annotations.Nullable;

public final class BookScreenController {
    private static final int PROJECTION_TRANSITION_TICKS = 6;

    private final BookDefinition definition;
    private final BookContext context;
    private final BookAnimController animController = new BookAnimController();

    private final int maxSpreadIndex;
    private BookSpread currentSpread;
    private ProjectionMode projectionMode = ProjectionMode.JOURNAL_READING;
    private String selectedProjectionId;
    private int projectionTransitionTicks;

    public BookScreenController(BookDefinition definition, BookContext context) {
        this.definition = definition;
        this.context = context;
        this.maxSpreadIndex = Math.max(0, definition.provider().spreadCount(context) - 1);
        this.currentSpread = definition.provider().getSpread(context, 0);
        this.animController.beginArrival();
    }

    public void tick() {
        int before = animController.getCurrentSpread();
        animController.tick();
        if (animController.getCurrentSpread() != before) {
            reloadSpread();
        }
        if (projectionMode == ProjectionMode.SKILL_PROJECTION_TRANSITION) {
            projectionTransitionTicks = Math.min(PROJECTION_TRANSITION_TICKS, projectionTransitionTicks + 1);
            if (projectionTransitionTicks >= PROJECTION_TRANSITION_TICKS) {
                projectionMode = ProjectionMode.SKILL_PROJECTION_ACTIVE;
            }
        } else if (projectionMode == ProjectionMode.SKILL_PROJECTION_EXITING) {
            projectionTransitionTicks = Math.max(0, projectionTransitionTicks - 1);
            if (projectionTransitionTicks <= 0) {
                projectionMode = ProjectionMode.JOURNAL_READING;
                selectedProjectionId = null;
            }
        }
    }

    public void nextPage() {
        if (animController.requestNextSpread(maxSpreadIndex)) {
            // texture swap happens when animation crosses its midpoint
        }
    }

    public void previousPage() {
        if (animController.requestPreviousSpread()) {
            // texture swap happens when animation crosses its midpoint
        }
    }

    public void nextSpread() {
        nextPage();
    }

    public void previousSpread() {
        previousPage();
    }

    public boolean jumpToSpread(int spreadIndex) {
        if (isProjectionVisible()) {
            return false;
        }
        return animController.requestJumpToSpread(spreadIndex, maxSpreadIndex);
    }

    public boolean jumpToFirstSpread() {
        return jumpToSpread(0);
    }

    public boolean jumpToLastSpread() {
        return jumpToSpread(maxSpreadIndex);
    }

    public boolean beginClosing() {
        return animController.requestClose();
    }

    public boolean beginOpening() {
        return animController.requestOpen();
    }

    public BookSpread currentSpread() {
        return currentSpread;
    }

    public BookAnimState state() {
        return animController.getState();
    }

    public BookAnimState visualState() {
        if (isProjectionVisible() && isOpenReadable()) {
            return switch (animController.getState()) {
                case FLIPPING_NEXT -> BookAnimState.FLIPPING_NEXT;
                case FLIPPING_PREV -> BookAnimState.FLIPPING_PREV;
                case RIFFLING_NEXT -> BookAnimState.RIFFLING_NEXT;
                case RIFFLING_PREV -> BookAnimState.RIFFLING_PREV;
                default -> BookAnimState.IDLE_OPEN;
            };
        }
        return animController.getState();
    }

    public boolean isOpening() {
        return animController.getState() == BookAnimState.OPENING;
    }

    public boolean isIdleOpen() {
        return animController.getState() == BookAnimState.IDLE_OPEN;
    }

    public boolean isOpenReadable() {
        return animController.getState() == BookAnimState.IDLE_OPEN
                || animController.getState() == BookAnimState.FLIPPING_NEXT
                || animController.getState() == BookAnimState.FLIPPING_PREV
                || animController.getState() == BookAnimState.RIFFLING_NEXT
                || animController.getState() == BookAnimState.RIFFLING_PREV;
    }

    public boolean isJournalReadable() {
        return isOpenReadable() && !isProjectionVisible();
    }

    public boolean isClosed() {
        return animController.getState() == BookAnimState.CLOSED;
    }

    public boolean isArriving() {
        return animController.getState() == BookAnimState.ARRIVING;
    }

    public boolean isClosing() {
        return animController.getState() == BookAnimState.CLOSING;
    }

    public int spreadIndex() {
        return animController.getCurrentSpread();
    }

    public float animationProgress() {
        return animController.getNormalizedProgress();
    }

    public BookDefinition definition() {
        return definition;
    }

    public BookContext context() {
        return context;
    }

    public int spreadCount() {
        return maxSpreadIndex + 1;
    }

    public boolean beginProjection(String focusId) {
        if (!isOpenReadable() || focusId == null || focusId.isBlank()) {
            return false;
        }
        BookProjectionView projection = definition.provider().projection(context, focusId);
        if (projection == null) {
            return false;
        }
        selectedProjectionId = focusId;
        projectionMode = ProjectionMode.SKILL_PROJECTION_TRANSITION;
        projectionTransitionTicks = 0;
        return true;
    }

    public boolean closeProjection() {
        if (!isProjectionVisible()) {
            return false;
        }
        projectionMode = ProjectionMode.SKILL_PROJECTION_EXITING;
        projectionTransitionTicks = Math.min(PROJECTION_TRANSITION_TICKS, Math.max(1, projectionTransitionTicks));
        return true;
    }

    public boolean nextProjectionFocus() {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return false;
        }
        String nextFocus = definition.provider().nextProjectionFocus(context, selectedProjectionId);
        return switchProjectionFocus(nextFocus, true);
    }

    public boolean previousProjectionFocus() {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return false;
        }
        String previousFocus = definition.provider().previousProjectionFocus(context, selectedProjectionId);
        return switchProjectionFocus(previousFocus, false);
    }

    public boolean isProjectionVisible() {
        return projectionMode != ProjectionMode.JOURNAL_READING;
    }

    public boolean isProjectionInteractive() {
        return projectionMode == ProjectionMode.SKILL_PROJECTION_ACTIVE;
    }

    public ProjectionMode projectionMode() {
        return projectionMode;
    }

    public float projectionProgress() {
        return switch (projectionMode) {
            case JOURNAL_READING -> 0.0f;
            case SKILL_PROJECTION_TRANSITION -> projectionTransitionTicks / (float) PROJECTION_TRANSITION_TICKS;
            case SKILL_PROJECTION_EXITING -> projectionTransitionTicks / (float) PROJECTION_TRANSITION_TICKS;
            case SKILL_PROJECTION_ACTIVE -> 1.0f;
        };
    }

    public @Nullable String selectedProjectionId() {
        return selectedProjectionId;
    }

    public @Nullable BookProjectionView projectionView() {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return null;
        }
        return definition.provider().projection(context, selectedProjectionId);
    }

    public int selectedProjectionPageIndex() {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return -1;
        }
        return definition.provider().pageIndexForProjectionFocus(context, selectedProjectionId);
    }

    private boolean switchProjectionFocus(@Nullable String nextFocusId, boolean forward) {
        if (nextFocusId == null || nextFocusId.isBlank() || nextFocusId.equals(selectedProjectionId)) {
            return false;
        }
        int currentSpread = animController.getCurrentSpread();
        int targetSpread = definition.provider().spreadForProjectionFocus(context, nextFocusId);
        if (targetSpread >= 0 && targetSpread != currentSpread) {
            boolean adjacentSpread = Math.abs(targetSpread - currentSpread) == 1;
            boolean changed = adjacentSpread
                    ? (forward ? animController.requestNextSpread(maxSpreadIndex) : animController.requestPreviousSpread())
                    : animController.requestDirectedRiffleToSpread(targetSpread, maxSpreadIndex, !forward);
            if (!changed) {
                return false;
            }
        }
        selectedProjectionId = nextFocusId;
        projectionMode = ProjectionMode.SKILL_PROJECTION_ACTIVE;
        projectionTransitionTicks = PROJECTION_TRANSITION_TICKS;
        if (targetSpread == currentSpread || targetSpread < 0) {
            reloadSpread();
        }
        return true;
    }

    private void reloadSpread() {
        currentSpread = definition.provider().getSpread(context, animController.getCurrentSpread());
    }

    public enum ProjectionMode {
        JOURNAL_READING,
        SKILL_PROJECTION_TRANSITION,
        SKILL_PROJECTION_ACTIVE,
        SKILL_PROJECTION_EXITING
    }
}
