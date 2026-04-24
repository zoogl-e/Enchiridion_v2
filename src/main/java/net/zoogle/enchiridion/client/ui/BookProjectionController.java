package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookProjectionView;
import org.jetbrains.annotations.Nullable;

final class BookProjectionController {
    private static final int PROJECTION_TRANSITION_TICKS = 6;

    private final BookDefinition definition;
    private final BookContext context;

    private ProjectionMode projectionMode = ProjectionMode.JOURNAL_READING;
    private String selectedProjectionId;
    private int projectionTransitionTicks;

    BookProjectionController(BookDefinition definition, BookContext context) {
        this.definition = definition;
        this.context = context;
    }

    void tick() {
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

    boolean beginProjection(boolean bookReadable, String focusId) {
        if (!bookReadable || focusId == null || focusId.isBlank()) {
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

    boolean closeProjection() {
        if (!isProjectionVisible()) {
            return false;
        }
        projectionMode = ProjectionMode.SKILL_PROJECTION_EXITING;
        projectionTransitionTicks = Math.min(PROJECTION_TRANSITION_TICKS, Math.max(1, projectionTransitionTicks));
        return true;
    }

    boolean nextProjectionFocus(ProjectionSpreadNavigator navigator) {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return false;
        }
        return switchProjectionFocus(definition.provider().nextProjectionFocus(context, selectedProjectionId), true, navigator);
    }

    boolean previousProjectionFocus(ProjectionSpreadNavigator navigator) {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return false;
        }
        return switchProjectionFocus(definition.provider().previousProjectionFocus(context, selectedProjectionId), false, navigator);
    }

    boolean isProjectionVisible() {
        return projectionMode != ProjectionMode.JOURNAL_READING;
    }

    boolean isProjectionInteractive() {
        return projectionMode == ProjectionMode.SKILL_PROJECTION_ACTIVE;
    }

    ProjectionMode projectionMode() {
        return projectionMode;
    }

    float projectionProgress() {
        return switch (projectionMode) {
            case JOURNAL_READING -> 0.0f;
            case SKILL_PROJECTION_TRANSITION, SKILL_PROJECTION_EXITING -> projectionTransitionTicks / (float) PROJECTION_TRANSITION_TICKS;
            case SKILL_PROJECTION_ACTIVE -> 1.0f;
        };
    }

    @Nullable String selectedProjectionId() {
        return selectedProjectionId;
    }

    @Nullable BookProjectionView projectionView() {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return null;
        }
        return definition.provider().projection(context, selectedProjectionId);
    }

    int selectedProjectionPageIndex() {
        if (!isProjectionVisible() || selectedProjectionId == null || selectedProjectionId.isBlank()) {
            return -1;
        }
        return definition.provider().pageIndexForProjectionFocus(context, selectedProjectionId);
    }

    private boolean switchProjectionFocus(@Nullable String nextFocusId, boolean forward, ProjectionSpreadNavigator navigator) {
        if (nextFocusId == null || nextFocusId.isBlank() || nextFocusId.equals(selectedProjectionId)) {
            return false;
        }
        int currentSpread = navigator.currentSpreadIndex();
        int targetSpread = definition.provider().spreadForProjectionFocus(context, nextFocusId);
        int maxSpreadIndex = Math.max(0, definition.provider().spreadCount(context) - 1);
        if (targetSpread >= 0 && targetSpread != currentSpread) {
            boolean adjacentSpread = Math.abs(targetSpread - currentSpread) == 1;
            boolean changed = adjacentSpread
                    ? (forward ? navigator.requestNextSpread(maxSpreadIndex) : navigator.requestPreviousSpread())
                    : navigator.requestDirectedRiffleToSpread(targetSpread, maxSpreadIndex, !forward);
            if (!changed) {
                return false;
            }
        }
        selectedProjectionId = nextFocusId;
        projectionMode = ProjectionMode.SKILL_PROJECTION_ACTIVE;
        projectionTransitionTicks = PROJECTION_TRANSITION_TICKS;
        if (targetSpread == currentSpread || targetSpread < 0) {
            navigator.reloadCurrentSpread();
        }
        return true;
    }

    interface ProjectionSpreadNavigator {
        int currentSpreadIndex();
        boolean requestNextSpread(int maxSpreadIndex);
        boolean requestPreviousSpread();
        boolean requestDirectedRiffleToSpread(int targetSpread, int maxSpreadIndex, boolean reverseDirection);
        void reloadCurrentSpread();
    }

    enum ProjectionMode {
        JOURNAL_READING,
        SKILL_PROJECTION_TRANSITION,
        SKILL_PROJECTION_ACTIVE,
        SKILL_PROJECTION_EXITING
    }
}
