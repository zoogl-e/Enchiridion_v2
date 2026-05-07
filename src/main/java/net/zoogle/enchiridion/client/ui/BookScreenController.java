package net.zoogle.enchiridion.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.zoogle.enchiridion.api.BookContentMode;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSection;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.client.anim.BookAnimController;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgJournalInteractionBridge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BookScreenController implements BookProjectionController.ProjectionSpreadNavigator {
    private final BookDefinition definition;
    private final BookContext context;
    private final IntroFlowPort introFlow;
    private final BookAnimController animController = new BookAnimController();
    private final BookProjectionController projectionController;

    private BookSpread currentSpread;
    private boolean exitWhenClosed;
    private boolean resetSpreadOnNextOpen;
    private int storedReadingSpreadIndex;
    private BookSection activeSection = BookSection.INTERIOR;
    private BookSection pendingSectionAfterFlip;

    public BookScreenController(BookDefinition definition, BookContext context) {
        this(definition, context, NoopIntroFlowPort.INSTANCE);
    }

    public BookScreenController(BookDefinition definition, BookContext context, IntroFlowPort introFlow) {
        this.definition = definition;
        this.context = context;
        this.introFlow = introFlow;
        context.contentSession().setMode(BookContentMode.READING);
        int initialSpreadIndex = Math.clamp(definition.provider().initialSpreadIndex(context), 0, maxSpreadIndex());
        this.storedReadingSpreadIndex = initialSpreadIndex;
        this.animController.setCurrentSpread(initialSpreadIndex);
        this.currentSpread = definition.provider().getSpread(context, initialSpreadIndex);
        this.projectionController = new BookProjectionController(definition, context);
        updateActiveSectionFromCurrentSpread();
        this.animController.beginArrival();
    }

    public void tick() {
        int before = animController.getCurrentSpread();
        animController.tick();
        applyPendingSectionTransitionIfReady();
        if (animController.getCurrentSpread() != before) {
            reloadSpread();
        }
        projectionController.tick();
        if (introFlow.tick(context)) {
            context.contentSession().setMode(BookContentMode.READING);
            int initialSpreadIndex = Math.clamp(definition.provider().initialSpreadIndex(context), 0, maxSpreadIndex());
            if (animController.getCurrentSpread() != initialSpreadIndex) {
                animController.setCurrentSpread(initialSpreadIndex);
            }
            storedReadingSpreadIndex = initialSpreadIndex;
            projectionController.closeProjection();
            reloadSpread();
        } else {
            clampSpreadToProvider();
            if (pendingSectionAfterFlip == null) {
                updateActiveSectionFromCurrentSpread();
            }
        }
    }

    public void nextPage() {
        requestNextSpread(maxSpreadIndex());
    }

    public void previousPage() {
        requestPreviousSpread();
    }

    public void nextSpread() {
        nextPage();
    }

    public void previousSpread() {
        previousPage();
    }

    public boolean jumpToSpread(int spreadIndex) {
        if (isProjectionVisible() || isBountyDocumentMode()) {
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
        if (animController.getState() == BookAnimState.IDLE_OPEN) {
            if (activeSection == BookSection.FRONT_SPECIAL) {
                return animController.requestClose(BookAnimState.CLOSING_FRONT);
            }
            if (activeSection == BookSection.BACK_SPECIAL) {
                return animController.requestClose(BookAnimState.CLOSING_BACK);
            }
        }
        return animController.requestClose(BookAnimState.CLOSING);
    }

    public boolean beginOpening() {
        context.contentSession().setMode(BookContentMode.READING);
        int initialSpreadIndex = Math.clamp(definition.provider().initialSpreadIndex(context), 0, maxSpreadIndex());
        if (resetSpreadOnNextOpen && animController.getCurrentSpread() != initialSpreadIndex) {
            animController.setCurrentSpread(initialSpreadIndex);
            storedReadingSpreadIndex = initialSpreadIndex;
            reloadSpread();
        }
        BookAnimState openingState = BookAnimState.OPENING;
        if (activeSection == BookSection.FRONT_SPECIAL) {
            openingState = BookAnimState.OPENING_FRONT;
        } else if (activeSection == BookSection.BACK_SPECIAL) {
            openingState = BookAnimState.OPENING_BACK;
        }
        exitWhenClosed = false;
        resetSpreadOnNextOpen = false;
        return animController.requestOpen(openingState);
    }

    public BookSpread currentSpread() {
        return currentSpread;
    }

    public BookSpread currentDisplayedSpread() {
        if (activeSection == BookSection.FRONT_SPECIAL
                || pendingSectionAfterFlip == BookSection.FRONT_SPECIAL) {
            return definition.provider().getSpecialSectionSpread(context, BookSection.FRONT_SPECIAL);
        }
        if (activeSection == BookSection.BACK_SPECIAL
                || pendingSectionAfterFlip == BookSection.BACK_SPECIAL) {
            return definition.provider().getSpecialSectionSpread(context, BookSection.BACK_SPECIAL);
        }
        return currentSpread;
    }

    public List<BookInteractiveRegion> currentInteractiveRegions(int displayedSpreadIndex) {
        if (activeSection == BookSection.FRONT_SPECIAL
                || pendingSectionAfterFlip == BookSection.FRONT_SPECIAL) {
            return definition.provider().specialSectionInteractiveRegions(context, BookSection.FRONT_SPECIAL);
        }
        if (activeSection == BookSection.BACK_SPECIAL
                || pendingSectionAfterFlip == BookSection.BACK_SPECIAL) {
            return definition.provider().specialSectionInteractiveRegions(context, BookSection.BACK_SPECIAL);
        }
        return definition.provider().interactiveRegions(context, displayedSpreadIndex);
    }

    public List<BookTrackedRegion> currentTrackedInteractiveRegions(int displayedSpreadIndex) {
        if (activeSection == BookSection.FRONT_SPECIAL
                || pendingSectionAfterFlip == BookSection.FRONT_SPECIAL) {
            return definition.provider().specialSectionTrackedInteractiveRegions(context, BookSection.FRONT_SPECIAL);
        }
        if (activeSection == BookSection.BACK_SPECIAL
                || pendingSectionAfterFlip == BookSection.BACK_SPECIAL) {
            return definition.provider().specialSectionTrackedInteractiveRegions(context, BookSection.BACK_SPECIAL);
        }
        return definition.provider().trackedInteractiveRegions(context, displayedSpreadIndex);
    }

    public BookSection activeSection() {
        return activeSection;
    }

    public void setActiveSection(BookSection section) {
        this.activeSection = section != null ? section : BookSection.INTERIOR;
    }

    public void enterBountyDocumentMode() {
        storedReadingSpreadIndex = animController.getCurrentSpread();
        context.contentSession().setMode(BookContentMode.BOUNTY);
        pendingSectionAfterFlip = null;
        setActiveSection(BookSection.INTERIOR);
        animController.setCurrentSpread(0);
        clampSpreadToProvider();
        reloadSpread();
    }

    public void exitBountyDocumentMode() {
        context.contentSession().setMode(BookContentMode.READING);
        pendingSectionAfterFlip = null;
        setActiveSection(BookSection.INTERIOR);
        animController.setCurrentSpread(storedReadingSpreadIndex);
        clampSpreadToProvider();
        reloadSpread();
    }

    /**
     * Midpoint of profile-sync content refresh while in the bounty document (claim / abandon / completion).
     */
    public void onBountyProfileSyncRefreshMidpoint() {
        pendingSectionAfterFlip = null;
        setActiveSection(BookSection.INTERIOR);
        animController.setCurrentSpread(0);
        clampSpreadToProvider();
        reloadSpread();
    }

    public void resetDocumentModeForBookClose() {
        if (context.contentSession().mode() != BookContentMode.BOUNTY) {
            return;
        }
        context.contentSession().setMode(BookContentMode.READING);
        pendingSectionAfterFlip = null;
        setActiveSection(BookSection.INTERIOR);
        animController.setCurrentSpread(storedReadingSpreadIndex);
        clampSpreadToProvider();
        reloadSpread();
    }

    public boolean isBountyDocumentMode() {
        return context.contentSession().isBountyDocument();
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
        if (activeSection == BookSection.FRONT_SPECIAL
                && isOpenReadable()
                && animController.getState() == BookAnimState.IDLE_OPEN) {
            return BookAnimState.IDLE_FRONT;
        }
        if (activeSection == BookSection.BACK_SPECIAL
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
        return animController.getState() == BookAnimState.OPENING
                || animController.getState() == BookAnimState.OPENING_FRONT
                || animController.getState() == BookAnimState.OPENING_BACK;
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

    public boolean isBookmarkInputAllowed() {
        if (isProjectionVisible() || activeSection != BookSection.INTERIOR || pendingSectionAfterFlip != null) {
            return false;
        }
        return switch (visualState()) {
            case IDLE_OPEN, FLIPPING_NEXT, FLIPPING_PREV -> true;
            default -> false;
        };
    }

    public boolean isClosed() {
        return animController.getState() == BookAnimState.CLOSED;
    }

    public boolean isArriving() {
        return animController.getState() == BookAnimState.ARRIVING;
    }

    public boolean isClosing() {
        return animController.getState() == BookAnimState.CLOSING
                || animController.getState() == BookAnimState.CLOSING_FRONT
                || animController.getState() == BookAnimState.CLOSING_BACK;
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
        if (pendingSectionAfterFlip == null) {
            updateActiveSectionFromCurrentSpread();
        }
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

    private void updateActiveSectionFromCurrentSpread() {
        if (isBountyDocumentMode()) {
            setActiveSection(BookSection.INTERIOR);
            return;
        }
        if (activeSection == BookSection.FRONT_SPECIAL && animController.getCurrentSpread() == 0) {
            return;
        }
        if (activeSection == BookSection.BACK_SPECIAL && animController.getCurrentSpread() == maxSpreadIndex()) {
            return;
        }
        setActiveSection(definition.provider().getSectionForSpread(context, animController.getCurrentSpread()));
    }

    private void applyPendingSectionTransitionIfReady() {
        if (pendingSectionAfterFlip == null) {
            return;
        }
        BookAnimState state = animController.getState();
        if (state != BookAnimState.FLIPPING_FRONT
                && state != BookAnimState.FLIPPING_FRONT_TO_ORIGIN
                && state != BookAnimState.FLIPPING_BACK
                && state != BookAnimState.FLIPPING_BACK_TO_ORIGIN) {
            pendingSectionAfterFlip = null;
            return;
        }
        if (animController.getNormalizedProgress() < net.zoogle.enchiridion.client.anim.BookAnimationSpec.flipPageSwapProgress()) {
            return;
        }
        setActiveSection(pendingSectionAfterFlip);
        reloadSpread();
        pendingSectionAfterFlip = null;
    }

    @Override
    public int currentSpreadIndex() {
        return animController.getCurrentSpread();
    }

    @Override
    public boolean requestNextSpread(int maxSpreadIndex) {
        if (isBountyDocumentMode()) {
            if (LevelRpgJournalInteractionBridge.activeSoloBountyId(context) != null) {
                return false;
            }
            boolean advanced = animController.requestNextSpread(maxSpreadIndex());
            if (advanced) {
                playPageTurnSound();
            }
            return advanced;
        }
        if (activeSection == BookSection.FRONT_SPECIAL) {
            return requestSectionFlip(BookSection.INTERIOR, BookAnimState.FLIPPING_FRONT_TO_ORIGIN);
        }
        if (activeSection == BookSection.BACK_SPECIAL) {
            return false;
        }
        if (activeSection == BookSection.INTERIOR
                && animController.getState() == BookAnimState.IDLE_OPEN
                && animController.getCurrentSpread() == maxSpreadIndex()
                && definition.provider().hasSpecialSection(context, BookSection.BACK_SPECIAL)) {
            return requestSectionFlip(BookSection.BACK_SPECIAL, BookAnimState.FLIPPING_BACK);
        }
        boolean advanced = animController.requestNextSpread(maxSpreadIndex());
        if (advanced) {
            playPageTurnSound();
        }
        return advanced;
    }

    @Override
    public boolean requestPreviousSpread() {
        if (isBountyDocumentMode()) {
            if (LevelRpgJournalInteractionBridge.activeSoloBountyId(context) != null) {
                return false;
            }
            boolean retreated = animController.requestPreviousSpread();
            if (retreated) {
                playPageTurnSound();
            }
            return retreated;
        }
        if (activeSection == BookSection.BACK_SPECIAL) {
            return requestSectionFlip(BookSection.INTERIOR, BookAnimState.FLIPPING_BACK_TO_ORIGIN);
        }
        if (activeSection == BookSection.FRONT_SPECIAL) {
            return false;
        }
        if (activeSection == BookSection.INTERIOR
                && animController.getState() == BookAnimState.IDLE_OPEN
                && animController.getCurrentSpread() == 0
                && definition.provider().hasSpecialSection(context, BookSection.FRONT_SPECIAL)) {
            return requestSectionFlip(BookSection.FRONT_SPECIAL, BookAnimState.FLIPPING_FRONT);
        }
        boolean retreated = animController.requestPreviousSpread();
        if (retreated) {
            playPageTurnSound();
        }
        return retreated;
    }

    @Override
    public boolean requestDirectedRiffleToSpread(int targetSpread, int maxSpreadIndex, boolean reverseDirection) {
        if (isBountyDocumentMode()) {
            return false;
        }
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

    private boolean requestSectionFlip(BookSection targetSection, BookAnimState flipState) {
        if (animController.getState() != BookAnimState.IDLE_OPEN) {
            return false;
        }
        pendingSectionAfterFlip = targetSection;
        boolean flipped = switch (flipState) {
            case FLIPPING_FRONT -> animController.requestFrontSpread();
            case FLIPPING_FRONT_TO_ORIGIN -> animController.requestFrontSpreadToOrigin();
            case FLIPPING_BACK -> {
                animController.setCurrentSpread(maxSpreadIndex());
                yield animController.requestBackSpread();
            }
            case FLIPPING_BACK_TO_ORIGIN -> {
                animController.setCurrentSpread(maxSpreadIndex());
                yield animController.requestBackSpreadToOrigin();
            }
            default -> false;
        };
        if (flipped) {
            playPageTurnSound();
        } else {
            pendingSectionAfterFlip = null;
        }
        return flipped;
    }
}
