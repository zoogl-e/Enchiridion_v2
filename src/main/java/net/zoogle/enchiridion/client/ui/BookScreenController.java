package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimController;
import net.zoogle.enchiridion.client.anim.BookAnimState;

public final class BookScreenController {
    private final BookDefinition definition;
    private final BookContext context;
    private final BookAnimController animController = new BookAnimController();

    private final int maxSpreadIndex;
    private BookSpread currentSpread;

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
        return animController.requestJumpToSpread(spreadIndex, maxSpreadIndex);
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

    private void reloadSpread() {
        currentSpread = definition.provider().getSpread(context, animController.getCurrentSpread());
    }
}
