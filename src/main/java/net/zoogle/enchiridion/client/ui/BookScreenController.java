package net.zoogle.enchiridion.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimController;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import org.jetbrains.annotations.Nullable;

public final class BookScreenController implements BookProjectionController.ProjectionSpreadNavigator {
    private final BookDefinition definition;
    private final BookContext context;
    private final IntroFlowPort introFlow;
    private final BookAnimController animController = new BookAnimController();
    private final BookProjectionController projectionController;

    private BookSpread currentSpread;
    private boolean exitWhenClosed;
    private boolean resetSpreadOnNextOpen;

    public BookScreenController(BookDefinition definition, BookContext context) {
        this(definition, context, NoopIntroFlowPort.INSTANCE);
    }

    public BookScreenController(BookDefinition definition, BookContext context, IntroFlowPort introFlow) {
        this.definition = definition;
        this.context = context;
        this.introFlow = introFlow;
        int initialSpreadIndex = Math.clamp(definition.provider().initialSpreadIndex(context), 0, maxSpreadIndex());
        this.animController.setCurrentSpread(initialSpreadIndex);
        this.currentSpread = definition.provider().getSpread(context, initialSpreadIndex);
        this.projectionController = new BookProjectionController(definition, context);
        this.animController.beginArrival();
    }

    public void tick() {
        int before = animController.getCurrentSpread();
        animController.tick();
        if (animController.getCurrentSpread() != before) {
            reloadSpread();
        }
        projectionController.tick();
        if (introFlow.tick(context)) {
            int initialSpreadIndex = Math.clamp(definition.provider().initialSpreadIndex(context), 0, maxSpreadIndex());
            if (animController.getCurrentSpread() != initialSpreadIndex) {
                animController.setCurrentSpread(initialSpreadIndex);
            }
            projectionController.closeProjection();
            reloadSpread();
        } else {
            clampSpreadToProvider();
        }
    }

    public void nextPage() {
        boolean moved = shouldUseFrontFlipToOrigin()
                ? animController.requestFrontSpreadToOrigin()
                : shouldUseBackFlip()
                ? animController.requestBackSpread()
                : animController.requestNextSpread(maxSpreadIndex());
        if (moved) {
            playPageTurnSound();
            // texture swap happens when animation crosses its midpoint
        }
    }

    public void previousPage() {
        boolean moved = shouldUseFrontFlip()
                ? animController.requestFrontSpread()
                : shouldUseBackFlipToOrigin()
                ? animController.requestBackSpreadToOrigin()
                : animController.requestPreviousSpread();
        if (moved) {
            playPageTurnSound();
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
        boolean jumped = animController.requestJumpToSpread(spreadIndex, maxSpreadIndex());
        if (jumped) {
            playPageTurnSound();
        }
        return jumped;
    }

    public boolean jumpToFirstSpread() {
        return jumpToSpread(0);
    }

    public boolean jumpToLastSpread() {
        return jumpToSpread(maxSpreadIndex());
    }

    public boolean beginClosing() {
        return animController.requestClose();
    }

    public boolean beginOpening() {
        int initialSpreadIndex = Math.clamp(definition.provider().initialSpreadIndex(context), 0, maxSpreadIndex());
        if (resetSpreadOnNextOpen && animController.getCurrentSpread() != initialSpreadIndex) {
            animController.setCurrentSpread(initialSpreadIndex);
            reloadSpread();
        }
        exitWhenClosed = false;
        resetSpreadOnNextOpen = false;
        return animController.requestOpen();
    }

    public BookSpread currentSpread() {
        return currentSpread;
    }

    public BookAnimState state() {
        return animController.getState();
    }

    public BookAnimState visualState() {
        if (animController.getState() == BookAnimState.FLIPPING_FRONT
                || animController.getState() == BookAnimState.FLIPPING_FRONT_TO_ORIGIN
                || animController.getState() == BookAnimState.FLIPPING_BACK
                || animController.getState() == BookAnimState.FLIPPING_BACK_TO_ORIGIN) {
            return animController.getState();
        }
        if (definition.provider().isFrontSpread(context, animController.getCurrentSpread())
                && isOpenReadable()
                && animController.getState() == BookAnimState.IDLE_OPEN) {
            return BookAnimState.IDLE_FRONT;
        }
        if (definition.provider().isBackSpread(context, animController.getCurrentSpread())
                && isOpenReadable()
                && animController.getState() == BookAnimState.IDLE_OPEN) {
            return BookAnimState.IDLE_BACK;
        }
        if (isProjectionVisible() && isOpenReadable()) {
            return switch (animController.getState()) {
                case FLIPPING_FRONT -> BookAnimState.FLIPPING_FRONT;
                case FLIPPING_FRONT_TO_ORIGIN -> BookAnimState.FLIPPING_FRONT_TO_ORIGIN;
                case FLIPPING_BACK -> BookAnimState.FLIPPING_BACK;
                case FLIPPING_BACK_TO_ORIGIN -> BookAnimState.FLIPPING_BACK_TO_ORIGIN;
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
                || animController.getState() == BookAnimState.FLIPPING_FRONT
                || animController.getState() == BookAnimState.FLIPPING_FRONT_TO_ORIGIN
                || animController.getState() == BookAnimState.FLIPPING_BACK
                || animController.getState() == BookAnimState.FLIPPING_BACK_TO_ORIGIN
                || animController.getState() == BookAnimState.FLIPPING_NEXT
                || animController.getState() == BookAnimState.FLIPPING_PREV
                || animController.getState() == BookAnimState.RIFFLING_NEXT
                || animController.getState() == BookAnimState.RIFFLING_PREV;
    }

    public boolean isJournalReadable() {
        return isOpenReadable() && !isProjectionVisible();
    }

    public boolean isInteractionStableReadable() {
        return animController.getState() == BookAnimState.IDLE_OPEN && !isProjectionVisible();
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
        return maxSpreadIndex() + 1;
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

    public void refreshCurrentSpread() {
        reloadSpread();
    }

    public boolean beginUserClosing() {
        if (hasPendingIntroFlow()) {
            resetPendingIntroFlow();
            exitWhenClosed = true;
            resetSpreadOnNextOpen = true;
        }
        return beginClosing();
    }

    public boolean shouldExitWhenClosed() {
        return exitWhenClosed && isClosed();
    }

    public boolean showFrontCoverCard() {
        return frontCoverCardState().visible();
    }

    public BookFrontCoverCardState frontCoverCardState() {
        return definition.provider().frontCoverCardState(context);
    }

    public void resetPendingIntroFlow() {
        if (hasPendingIntroFlow()) {
            introFlow.reset();
            projectionController.closeProjection();
            reloadSpread();
        }
    }

    public boolean beginArchetypeBinding(String focusId) {
        if (focusId == null || focusId.isBlank()) {
            return false;
        }
        introFlow.beginBinding(context, focusId);
        projectionController.closeProjection();
        reloadSpread();
        return true;
    }

    private boolean hasPendingIntroFlow() {
        return introFlow.shouldResetOnClose(context);
    }

    private enum NoopIntroFlowPort implements IntroFlowPort {
        INSTANCE;

        @Override
        public boolean tick(BookContext context) {
            return false;
        }

        @Override
        public void beginBinding(BookContext context, String focusId) {
        }

        @Override
        public boolean shouldResetOnClose(BookContext context) {
            return false;
        }

        @Override
        public void reset() {
        }
    }

    private int maxSpreadIndex() {
        return Math.max(0, definition.provider().spreadCount(context) - 1);
    }

    private void clampSpreadToProvider() {
        int maxSpreadIndex = maxSpreadIndex();
        if (animController.getCurrentSpread() > maxSpreadIndex) {
            animController.setCurrentSpread(maxSpreadIndex);
            projectionController.closeProjection();
            reloadSpread();
        }
    }

    private boolean shouldUseFrontFlip() {
        if (animController.getState() != BookAnimState.IDLE_OPEN || animController.getCurrentSpread() <= 0) {
            return false;
        }
        int targetSpread = animController.getCurrentSpread() - 1;
        return definition.provider().isFrontSpread(context, targetSpread)
                && !definition.provider().isFrontSpread(context, animController.getCurrentSpread());
    }

    private boolean shouldUseFrontFlipToOrigin() {
        return animController.getState() == BookAnimState.IDLE_OPEN
                && definition.provider().isFrontSpread(context, animController.getCurrentSpread())
                && animController.getCurrentSpread() < maxSpreadIndex();
    }

    private boolean shouldUseBackFlip() {
        if (animController.getState() != BookAnimState.IDLE_OPEN || animController.getCurrentSpread() >= maxSpreadIndex()) {
            return false;
        }
        int targetSpread = animController.getCurrentSpread() + 1;
        return definition.provider().isBackSpread(context, targetSpread)
                && !definition.provider().isBackSpread(context, animController.getCurrentSpread());
    }

    private boolean shouldUseBackFlipToOrigin() {
        return animController.getState() == BookAnimState.IDLE_OPEN
                && definition.provider().isBackSpread(context, animController.getCurrentSpread())
                && animController.getCurrentSpread() > 0;
    }

    @Override
    public int currentSpreadIndex() {
        return animController.getCurrentSpread();
    }

    @Override
    public boolean requestNextSpread(int maxSpreadIndex) {
        boolean advanced = shouldUseFrontFlipToOrigin()
                ? animController.requestFrontSpreadToOrigin()
                : shouldUseBackFlip()
                ? animController.requestBackSpread()
                : animController.requestNextSpread(maxSpreadIndex());
        if (advanced) {
            playPageTurnSound();
        }
        return advanced;
    }

    @Override
    public boolean requestPreviousSpread() {
        boolean retreated = shouldUseFrontFlip()
                ? animController.requestFrontSpread()
                : shouldUseBackFlipToOrigin()
                ? animController.requestBackSpreadToOrigin()
                : animController.requestPreviousSpread();
        if (retreated) {
            playPageTurnSound();
        }
        return retreated;
    }

    @Override
    public boolean requestDirectedRiffleToSpread(int targetSpread, int maxSpreadIndex, boolean reverseDirection) {
        boolean riffled = animController.requestDirectedRiffleToSpread(targetSpread, maxSpreadIndex(), reverseDirection);
        if (riffled) {
            playPageTurnSound();
        }
        return riffled;
    }

    @Override
    public void reloadCurrentSpread() {
        reloadSpread();
    }

    private static void playPageTurnSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }
}
