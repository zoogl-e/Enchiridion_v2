package net.zoogle.enchiridion.client.ui;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.client.audio.PostBindAmbientScheduler;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.archetype.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.archetype.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.archetype.LevelRpgIntroFlowAdapter;
import net.zoogle.enchiridion.client.levelrpg.archetype.ReelFlowController;
import net.zoogle.enchiridion.client.levelrpg.projection.SkillTreeProjectionData;

import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import org.lwjgl.glfw.GLFW;
import java.util.List;
public final class BookScreen extends Screen {
    private final BookScreenController controller;
    private final BookSceneRenderer sceneRenderer = new BookSceneRenderer();
    private final BookInteractionResolver interactionResolver = new BookInteractionResolver();
    private final BookViewState viewState = new BookViewState();
    private final BookModeCoordinator modeCoordinator = new BookModeCoordinator();
    private final BookInputController inputController = new BookInputController();
    private final BookOverlayRenderer overlayRenderer = new BookOverlayRenderer();
    private final InfoPanelLayoutService infoPanelLayoutService = new InfoPanelLayoutService();
    private final BookEditorCoordinator editorCoordinator = new BookEditorCoordinator();
    private final BookCinematicManager cinematicManager = new BookCinematicManager();
    private final SkillTreeProjectionController projectionController = new SkillTreeProjectionController();
    private final ArchetypeReelCoordinator archetypeCoordinator = new ArchetypeReelCoordinator();
    
    private Component overlayTitle;
    private boolean shouldRenderArchetypeReel() {
        return modeCoordinator.shouldHandleReelInput(archetypeCoordinator.reelState()) && archetypeCoordinator.shouldRender();
    }
    public BookScreen(BookDefinition definition) {
        this(definition, createContext(definition));
    }
    public BookScreen(BookDefinition definition, BookContext context) {
        super(definition.provider().displayTitle(context, definition.title()));
        this.controller = new BookScreenController(definition, context, new LevelRpgIntroFlowAdapter());
        this.overlayTitle = definition.provider().displayTitle(context, definition.title());
    }
    private static BookContext createContext(BookDefinition definition) {
        Minecraft minecraft = Minecraft.getInstance();
        return BookContext.of(minecraft, minecraft.player, definition.id());
    }
    @Override
    protected void init() {
        viewState.initLayout(width, height);
        if (viewState.displayedSpread() == null || viewState.displayedSpreadIndex() != controller.spreadIndex()) {
            viewState.syncDisplayedSpreadFromController(controller);
        }
        sceneRenderer.prewarmPageTextures(viewState.displayedSpread(), viewState.displayedSpreadIndex());
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    public void tick() {
        cinematicManager.tick();
        controller.tick();
        projectionController.tick(controller, viewState, width, height);
        archetypeCoordinator.tick(controller, viewState, cinematicManager, width, height);

        overlayTitle = controller.definition().provider().displayTitle(controller.context(), controller.definition().title());
        viewState.updateTextTransition(controller);
        viewState.updateProjectionFocusOffset(controller);
        if (controller.isClosed() && controller.shouldExitWhenClosed()) {
            super.onClose();
        }
    }
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (viewState.layout() == null) {
            viewState.initLayout(width, height);
        }
        if (viewState.displayedSpread() == null) {
            viewState.syncDisplayedSpreadFromController(controller);
        }
        sceneRenderer.beginPresentationFrame(reelStateForPresentation(), width, height);
        try {
            renderImpl(graphics, mouseX, mouseY, partialTick);
        } finally {
            sceneRenderer.endPresentationFrame();
        }
    }
    private void renderImpl(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        cinematicManager.applyCameraShake(graphics, partialTick);
        try {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        preparePresentation(mouseX, mouseY);
        if (shouldRenderArchetypeReel()) {
            if (archetypeCoordinator.reelState().dragging()) {
                archetypeCoordinator.reelState().setDragMouse(mouseX, mouseY);
            }
            BookSceneRenderer.ScreenRect focusedCardRect = sceneRenderer.focusedArchetypeReelCardRect(viewState.layout(), archetypeCoordinator.reelState());
            float tiltAnchorX = focusedCardRect == null ? (width / 2.0f) : focusedCardRect.x() + (focusedCardRect.width() / 2.0f);
            float tiltAnchorY = focusedCardRect == null ? (height / 2.0f) : focusedCardRect.y() + (focusedCardRect.height() / 2.0f);
            archetypeCoordinator.reelState().updateFocusTilt(tiltAnchorX, tiltAnchorY, mouseX, mouseY);
            archetypeCoordinator.updateHover(mouseX, mouseY, viewState, sceneRenderer);
        }
        if (projectionController.isActive()) {
            projectionController.updateHover(viewState, sceneRenderer, mouseX, mouseY);
        }
        BookInteractionResolver.Resolution interaction = resolveInteraction(mouseX, mouseY);
        renderBook(graphics, interaction, mouseX, mouseY, partialTick);
        if (shouldRenderArchetypeReel() && archetypeCoordinator.reelState().dragging()) {
            archetypeCoordinator.renderDropTargetHint(graphics, archetypeDropTargetRect());
        }
        cinematicManager.renderPulse(graphics, archetypeDropTargetRect());
        if (projectionController.isActive()) {
            projectionController.render(graphics, font, viewState, controller, sceneRenderer, mouseX, mouseY);
        }
        cinematicManager.renderBolt(graphics, width, height, archetypeDropTargetRect());
        super.render(graphics, mouseX, mouseY, partialTick);
        if (shouldRenderArchetypeReel()) {
            archetypeCoordinator.renderTopmost(graphics, font, viewState, staticArchetypeReelPanelRect());
        }
        overlayRenderer.renderTopmostOverlay(
                graphics,
                font,
                width,
                height,
                overlayTitle,
                controller,
                viewState,
                sceneRenderer,
                interactionResolver,
                mouseX,
                mouseY,
                BookDebugSettings.pageLocalInteractionDebug(),
                BookDebugSettings.hoveredInteractiveBoundsDebug()
        );
        cinematicManager.renderFlash(graphics, width, height);
        editorCoordinator.render(graphics, font, controller, viewState, sceneRenderer, height);
        } finally {
            graphics.pose().popPose();
        }
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editorCoordinator.keyPressed(keyCode, modifiers, controller, viewState)) {
            return true;
        }
        if (shouldRenderArchetypeReel()) {
            return archetypeCoordinator.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (projectionController.keyPressed(keyCode)) {
            return true;
        }
        return inputController.keyPressed(
                controller,
                modeCoordinator,
                viewState,
                keyCode,
                () -> super.keyPressed(keyCode, scanCode, modifiers),
                super::onClose
        );
    }
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editorCoordinator.charTyped(codePoint, controller, viewState)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
    @Override
    public void onClose() {
        if (controller.isClosed() || controller.isArriving() || controller.isClosing()) {
            super.onClose();
            return;
        }
        controller.beginUserClosing();
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editorCoordinator.mouseClicked(controller, viewState, sceneRenderer, mouseX, mouseY, button)) {
            return true;
        }
        if (shouldRenderArchetypeReel()) {
            return archetypeCoordinator.mouseClicked(mouseX, mouseY, button, viewState, sceneRenderer);
        }
        if (projectionController.mouseClicked(mouseX, mouseY, button, width, height, viewState, sceneRenderer, controller.context())) {
            return true;
        }
        if (inputController.mouseClicked(controller, modeCoordinator, viewState, viewState.layout(), sceneRenderer, mouseX, mouseY, button)) {
            return true;
        }
        return controller.isJournalReadable() && super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (editorCoordinator.mouseDragged(controller, viewState, sceneRenderer, mouseX, mouseY)) {
            return true;
        }
        if (shouldRenderArchetypeReel()
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean hoveredOverTarget = isPointInScreenRect(mouseX, mouseY, archetypeDropTargetRect());
            return archetypeCoordinator.mouseDragged(mouseX, mouseY, hoveredOverTarget);
        }
        if (projectionController.mouseDragged(mouseX, mouseY, button, dragX, dragY, width, height, viewState)) {
            return true;
        }
        return inputController.mouseDragged(controller, viewState, button, mouseX, mouseY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (projectionController.mouseScrolled(mouseX, mouseY, scrollY, viewState, width, height)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (editorCoordinator.mouseReleased(button)) {
            return true;
        }
        if (shouldRenderArchetypeReel()
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean droppedOnTarget = isPointInScreenRect(mouseX, mouseY, archetypeDropTargetRect());
            return archetypeCoordinator.mouseReleased(mouseX, mouseY, droppedOnTarget);
        }
        archetypeCoordinator.reelState().endDrag();
        return inputController.mouseReleased(viewState, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }
    public boolean jumpToSpread(int spreadIndex) {
        return controller.isJournalReadable() && controller.jumpToSpread(spreadIndex);
    }
    public int currentSpreadIndex() {
        return controller.spreadIndex();
    }
    public boolean openProjection(String focusId) {
        return controller.beginProjection(focusId);
    }
    public boolean openArchetypeReel() {
        closeSkillTreeProjection();
        controller.closeProjection();
        List<JournalArchetypeChoice> choices = LevelRpgJournalInteractionBridge.availableArchetypes();
        BookFrontCoverCardState cardState = controller.frontCoverCardState();
        return archetypeCoordinator.open(choices, cardState.boundArchetypeId(), cardState.selectedArchetypeId());
    }
    public boolean closeProjection() {
        return controller.closeProjection();
    }
    public boolean openSkillTreeProjection(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return false;
        }
        projectionController.close();
        archetypeCoordinator.close();
        controller.closeProjection();
        return projectionController.open(skillName, LevelRpgJournalInteractionBridge.availableJournalSkillNames());
    }
    public boolean beginArchetypeBinding(String focusId) {
        return archetypeCoordinator.beginBinding(controller, focusId, viewState);
    }
    private void preparePresentation(int mouseX, int mouseY) {
        viewState.setClosedBookHovered(controller.isClosed() && sceneRenderer.isClosedBookHovered(viewState.layout(), mouseX, mouseY));
        sceneRenderer.preparePresentation(
                viewState.layout(),
                effectiveVisualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                mouseX,
                mouseY,
                viewState.closedBookHovered(),
                viewState.inspectYaw(),
                viewState.inspectPitch()
        );
        viewState.setHoveredProjectionButton(
                overlayRenderer.resolveProjectionButton(viewState.layout(), width, height, controller, mouseX, mouseY)
        );
    }
    private BookInteractionResolver.Resolution resolveInteraction(int mouseX, int mouseY) {
        if (projectionController.isActive()) {
            BookInteractionResolver.Resolution interaction = BookInteractionResolver.Resolution.empty();
            viewState.setInteraction(interaction);
            return interaction;
        }
        BookInteractionResolver.Resolution interaction = interactionResolver.resolve(
                controller,
                sceneRenderer,
                viewState.layout(),
                viewState.displayedSpread(),
                viewState.displayedSpreadIndex(),
                viewState.currentProjectionFocusOffset(),
                mouseX,
                mouseY,
                BookDebugSettings.pageLocalInteractionDebug()
        );
        if (modeCoordinator.shouldResolvePageInteraction(controller, archetypeCoordinator.reelState(), projectionController.isActive())) {
            viewState.setInteraction(interaction);
            return interaction;
        }
        if (modeCoordinator.shouldHandleReelInput(archetypeCoordinator.reelState())) {
            interaction = BookInteractionResolver.Resolution.empty();
        }
        viewState.setInteraction(interaction);
        return interaction;
    }
    private void renderBook(GuiGraphics graphics, BookInteractionResolver.Resolution interaction, int mouseX, int mouseY, float partialTick) {
        sceneRenderer.renderBook(
                graphics,
                viewState.layout(),
                viewState.displayedSpread(),
                viewState.displayedSpreadIndex(),
                effectiveVisualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                viewState.currentProjectionFocusOffset(),
                viewState.focusedProjectionPageSide(controller),
                interactionResolver.pageNodesFor(interaction, BookPageSide.LEFT),
                interactionResolver.pageNodesFor(interaction, BookPageSide.RIGHT),
                interactionResolver.hoveredInteractiveNode(interaction, BookPageSide.LEFT),
                interactionResolver.hoveredInteractiveNode(interaction, BookPageSide.RIGHT),
                mouseX,
                mouseY,
                viewState.textAlpha(),
                partialTick,
                currentFrontCoverCardState(),
                reelStateForPresentation(),
                viewState.closedBookHovered(),
                viewState.inspectYaw(),
                viewState.inspectPitch()
        );
    }
    private BookFrontCoverCardState currentFrontCoverCardState() {
        BookFrontCoverCardState base = controller.frontCoverCardState();
        if (!shouldRenderArchetypeReel()) {
            return base;
        }
        return new BookFrontCoverCardState(
                base.visible(),
                base.clickable(),
                archetypeCoordinator.reelState().hoveredArchetypeId(),
                archetypeCoordinator.reelState().focusedArchetypeId(),
                base.boundArchetypeId()
        );
    }
    private net.zoogle.enchiridion.client.anim.BookAnimState effectiveVisualState() {
        if (shouldRenderArchetypeReel()) {
            return archetypeCoordinator.reelState().dragFrontBlend() >= 0.5f
                    ? net.zoogle.enchiridion.client.anim.BookAnimState.IDLE_FRONT
                    : net.zoogle.enchiridion.client.anim.BookAnimState.IDLE_FRONT_SKILLTREE;
        }
        if (projectionController.isActive()) {
            return net.zoogle.enchiridion.client.anim.BookAnimState.IDLE_SKILLTREE;
        }
        return controller.visualState();
    }
    public boolean closeSkillTreeProjection() {
        if (!projectionController.isActive()) {
            return false;
        }
        projectionController.close();
        return true;
    }
    private InfoPanelLayoutService.PanelRect staticArchetypeReelPanelRect() {
        return infoPanelLayoutService.resolveStaticReelArchetypePanelRect(width, height);
    }
    private BookSceneRenderer.ScreenRect archetypeDropTargetRect() {
        if (viewState.layout() == null) {
            return null;
        }
        return sceneRenderer.projectTrackedRegionRect(
                viewState.layout(),
                effectiveVisualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                viewState.currentProjectionFocusOffset(),
                BookTrackedRegion.Anchor.FRONT_COVER_CARD
        );
    }
    private boolean isPointInScreenRect(double mouseX, double mouseY, BookSceneRenderer.ScreenRect rect) {
        if (rect == null) {
            return false;
        }
        return mouseX >= rect.x()
                && mouseX <= rect.x() + rect.width()
                && mouseY >= rect.y()
                && mouseY <= rect.y() + rect.height();
    }
    private ArchetypeReelState reelStateForPresentation() {
        return archetypeCoordinator.reelStateForPresentation();
    }
}
