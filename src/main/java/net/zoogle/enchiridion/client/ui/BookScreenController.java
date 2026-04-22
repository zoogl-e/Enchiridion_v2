package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimController;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import org.jetbrains.annotations.Nullable;

public final class BookScreenController implements BookProjectionController.ProjectionSpreadNavigator {
    private final BookDefinition definition;
    private final BookContext context;
    private final BookAnimController animController = new BookAnimController();
    private final BookProjectionController projectionController;

    private final int maxSpreadIndex;
    private BookSpread currentSpread;

    public BookScreenController(BookDefinition definition, BookContext context) {
        this.definition = definition;
        this.context = context;
        this.maxSpreadIndex = Math.max(0, definition.provider().spreadCount(context) - 1);
        this.currentSpread = definition.provider().getSpread(context, 0);
        this.projectionController = new BookProjectionController(definition, context, maxSpreadIndex);
        this.animController.beginArrival();
    }

    public void tick() {
        int before = animController.getCurrentSpread();
        animController.tick();
        if (animController.getCurrentSpread() != before) {
            reloadSpread();
        }
        projectionController.tick();
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
        return projectionController.beginProjection(isOpenReadable(), focusId);
    }

    public boolean closeProjection() {
        return projectionController.closeProjection();
    }

    public boolean nextProjectionFocus() {
        return projectionController.nextProjectionFocus(this);
    }

    public boolean previousProjectionFocus() {
        return projectionController.previousProjectionFocus(this);
    }

    public boolean isProjectionVisible() {
        return projectionController.isProjectionVisible();
    }

    public boolean isProjectionInteractive() {
        return projectionController.isProjectionInteractive();
    }

    public BookProjectionController.ProjectionMode projectionMode() {
        return projectionController.projectionMode();
    }

    public float projectionProgress() {
        return projectionController.projectionProgress();
    }

    public @Nullable String selectedProjectionId() {
        return projectionController.selectedProjectionId();
    }

    public @Nullable BookProjectionView projectionView() {
        return projectionController.projectionView();
    }

    public int selectedProjectionPageIndex() {
        return projectionController.selectedProjectionPageIndex();
    }

    void reloadSpread() {
        currentSpread = definition.provider().getSpread(context, animController.getCurrentSpread());
    }

    @Override
    public int currentSpreadIndex() {
        return animController.getCurrentSpread();
    }

    @Override
    public boolean requestNextSpread(int maxSpreadIndex) {
        return animController.requestNextSpread(maxSpreadIndex);
    }

    @Override
    public boolean requestPreviousSpread() {
        return animController.requestPreviousSpread();
    }

    @Override
    public boolean requestDirectedRiffleToSpread(int targetSpread, int maxSpreadIndex, boolean reverseDirection) {
        return animController.requestDirectedRiffleToSpread(targetSpread, maxSpreadIndex, reverseDirection);
    }

    @Override
    public void reloadCurrentSpread() {
        reloadSpread();
    }
}
