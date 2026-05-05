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
import net.zoogle.enchiridion.client.levelrpg.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgArchetypeBindGateway;
import net.zoogle.enchiridion.client.levelrpg.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgIntroFlowAdapter;
import net.zoogle.enchiridion.client.levelrpg.ReelFlowController;

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
    private final ArchetypeReelState archetypeReelState = new ArchetypeReelState();
    private final ReelFlowController reelFlowController = new ReelFlowController();
    private final LevelRpgArchetypeBindGateway archetypeBindGateway = new LevelRpgArchetypeBindGateway();
    private final BookCinematicManager cinematicManager = new BookCinematicManager();
    private final SkillTreeProjectionController projectionController = new SkillTreeProjectionController();
    
    
    private Component overlayTitle;
    private ReelFlowController.FlowPhase previousReelPhase = ReelFlowController.FlowPhase.INACTIVE;
    private boolean shouldRenderArchetypeReel() {
        return modeCoordinator.shouldHandleReelInput(archetypeReelState) && reelFlowController.shouldRenderReel();
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
        reelFlowController.tick(
                archetypeReelState,
                controller.frontCoverCardState().boundArchetypeId(),
                controller.context(),
                archetypeBindGateway
        );
        ReelFlowController.FlowPhase currentPhase = reelFlowController.phase();
        if (currentPhase != previousReelPhase) {
            cinematicManager.onReelPhaseChanged(previousReelPhase, currentPhase, width, height);
            previousReelPhase = currentPhase;
        }
        if (reelFlowController.phase() == ReelFlowController.FlowPhase.COMPLETED) {
            archetypeReelState.close();
            reelFlowController.onClosed();
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        }
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
            if (archetypeReelState.dragging()) {
                archetypeReelState.setDragMouse(mouseX, mouseY);
            }
            BookSceneRenderer.ScreenRect focusedCardRect = sceneRenderer.focusedArchetypeReelCardRect(viewState.layout(), archetypeReelState);
            float tiltAnchorX = focusedCardRect == null ? (width / 2.0f) : focusedCardRect.x() + (focusedCardRect.width() / 2.0f);
            float tiltAnchorY = focusedCardRect == null ? (height / 2.0f) : focusedCardRect.y() + (focusedCardRect.height() / 2.0f);
            archetypeReelState.updateFocusTilt(tiltAnchorX, tiltAnchorY, mouseX, mouseY);
            updateArchetypeReelHover(mouseX, mouseY);
        }
        if (projectionController.isActive()) {
            projectionController.updateHover(viewState, sceneRenderer, mouseX, mouseY);
        }
        BookInteractionResolver.Resolution interaction = resolveInteraction(mouseX, mouseY);
        renderBook(graphics, interaction, mouseX, mouseY, partialTick);
        if (shouldRenderArchetypeReel() && archetypeReelState.dragging()) {
            renderArchetypeDropTargetHint(graphics);
        }
        cinematicManager.renderPulse(graphics, archetypeDropTargetRect());
        if (projectionController.isActive()) {
            projectionController.render(graphics, font, viewState, controller, sceneRenderer, mouseX, mouseY);
        }
        cinematicManager.renderBolt(graphics, width, height, archetypeDropTargetRect());
        super.render(graphics, mouseX, mouseY, partialTick);
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
            return handleArchetypeReelKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
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
            return handleArchetypeReelClick(mouseX, mouseY, button);
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
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && handleArchetypeReelDrag(mouseX, mouseY)) {
            return true;
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
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && handleArchetypeReelRelease(mouseX, mouseY)) {
            return true;
        }
        projectionController.mouseReleased(button);
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
        boolean opened = archetypeReelState.open(choices, cardState.boundArchetypeId(), cardState.selectedArchetypeId());
        if (opened) {
            reelFlowController.onOpened();
        }
        return opened;
    }
    public boolean closeProjection() {
        return controller.closeProjection();
    }
    public boolean openSkillTreeProjection(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return false;
        }
        projectionController.close();
        archetypeReelState.close();
        reelFlowController.onClosed();
        controller.closeProjection();
        return projectionController.open(skillName, LevelRpgJournalInteractionBridge.availableJournalSkillNames());
    }
    public boolean beginArchetypeBinding(String focusId) {
        boolean begun = controller.beginArchetypeBinding(focusId);
        if (begun) {
            archetypeReelState.close();
            reelFlowController.onClosed();
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        }
        return begun;
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
        if (modeCoordinator.shouldResolvePageInteraction(controller, archetypeReelState, projectionController.isActive())) {
            viewState.setInteraction(interaction);
            return interaction;
        }
        if (modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
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
                archetypeReelState.hoveredArchetypeId(),
                archetypeReelState.focusedArchetypeId(),
                base.boundArchetypeId()
        );
    }
    private void updateArchetypeReelHover(int mouseX, int mouseY) {
        Integer hoveredIndex = sceneRenderer.hoveredArchetypeReelCardIndex(viewState.layout(), archetypeReelState, mouseX, mouseY);
        archetypeReelState.setHoveredIndex(hoveredIndex == null ? -1 : hoveredIndex);
    }
    private net.zoogle.enchiridion.client.anim.BookAnimState effectiveVisualState() {
        if (shouldRenderArchetypeReel()) {
            return archetypeReelState.dragFrontBlend() >= 0.5f
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
    private boolean handleArchetypeReelKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (canCancelArchetypeReel()) {
                archetypeReelState.close();
                reelFlowController.onClosed();
                return true;
            }
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            archetypeReelState.move(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            archetypeReelState.move(1);
            return true;
        }
        return false;
    }
    private boolean handleArchetypeReelClick(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (canCancelArchetypeReel()) {
                archetypeReelState.endDrag();
                archetypeReelState.close();
                reelFlowController.onClosed();
                return true;
            }
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        Integer hoveredIndex = sceneRenderer.hoveredArchetypeReelCardIndex(viewState.layout(), archetypeReelState, mouseX, mouseY);
        int dragIndex = hoveredIndex == null ? archetypeReelState.focusedIndex() : hoveredIndex;
        boolean started = archetypeReelState.beginDrag(dragIndex);
        if (started) {
            archetypeReelState.setDragMouse((float) mouseX, (float) mouseY);
        }
        return started;
    }
    private boolean handleArchetypeReelDrag(double mouseX, double mouseY) {
        if (!archetypeReelState.dragging()) {
            return false;
        }
        archetypeReelState.setDragMouse((float) mouseX, (float) mouseY);
        BookSceneRenderer.ScreenRect slot = archetypeDropTargetRect();
        boolean hovered = isPointInScreenRect(mouseX, mouseY, slot);
        archetypeReelState.setDropTargetHovered(hovered);
        return true;
    }
    private boolean handleArchetypeReelRelease(double mouseX, double mouseY) {
        if (!archetypeReelState.dragging()) {
            return false;
        }
        JournalArchetypeChoice droppedChoice = archetypeReelState.draggedChoice();
        BookSceneRenderer.ScreenRect slot = archetypeDropTargetRect();
        boolean droppedOnSlot = isPointInScreenRect(mouseX, mouseY, slot);
        archetypeReelState.endDrag();
        if (!droppedOnSlot) {
            return true;
        }
        if (droppedChoice != null) {
            return reelFlowController.confirmChoice(archetypeReelState, droppedChoice);
        }
        archetypeReelState.snapToNearestFocus();
        return confirmArchetypeReelFocus();
    }
    private boolean confirmArchetypeReelFocus() {
        if (archetypeReelState.phase() != ArchetypeReelState.Phase.IDLE) {
            return false;
        }
        archetypeReelState.endDrag();
        JournalArchetypeChoice focusedChoice = archetypeReelState.focusedChoice();
        if (focusedChoice == null) {
            return false;
        }
        return reelFlowController.confirmFocused(archetypeReelState);
    }
    private boolean canCancelArchetypeReel() {
        return reelFlowController.canCancel();
    }
    private void renderArchetypeReelInfoPanel(GuiGraphics graphics) {
        JournalArchetypeChoice focusedChoice = archetypeReelState.focusedChoice();
        if (focusedChoice == null || viewState.layout() == null) {
            return;
        }
        InfoPanelLayoutService.PanelRect panelRect = staticArchetypeReelPanelRect();
        if (!panelRect.visible()) {
            return;
        }
        float alpha = archetypeReelState.globalPresentationAlpha();
        int px = panelRect.x();
        int py = panelRect.y();
        int pw = panelRect.width();
        int ph = panelRect.height();
        // Background layers
        graphics.fill(px, py, px + pw, py + ph, scaleColorAlpha(0xB819120D, alpha));
        graphics.fill(px + 2, py + 2, px + pw - 2, py + ph - 2, scaleColorAlpha(0xDCCBAF86, alpha));
        graphics.renderOutline(px, py, pw, ph, scaleColorAlpha(0xFF6D4A2C, alpha));
        graphics.renderOutline(px + 4, py + 4, pw - 8, ph - 8, scaleColorAlpha(0x446D4A2C, alpha));
        // Nav hint strip separator
        int hintStripTop = py + ph - 20;
        graphics.fill(px + 4, hintStripTop, px + pw - 4, hintStripTop + 1, scaleColorAlpha(0x446D4A2C, alpha));
        int pad = 12;
        int textX = px + pad;
        int textW = pw - pad * 2;
        int contentBottom = hintStripTop - 4;
        int y = py + pad;
        // Title — centered
        String title = focusedChoice.title();
        int titleX = px + (pw / 2) - (font.width(title) / 2);
        graphics.drawString(font, title, titleX, y, scaleColorAlpha(0xFF2A180F, alpha), false);
        y += 12;
        // Decorative rule under title
        graphics.fill(px + 16, y, px + pw - 16, y + 1, scaleColorAlpha(0x886D4A2C, alpha));
        y += 6;
        // Phase label — only during binding states
        String phaseLabel = switch (reelFlowController.phase()) {
            case REQUESTING_BIND -> "Inscribing\u2026";
            case AWAITING_SYNC -> "Awaiting Seal";
            case FAILED -> "Binding Failed";
            default -> null;
        };
        if (phaseLabel != null) {
            int labelX = px + (pw / 2) - (font.width(phaseLabel) / 2);
            graphics.drawString(font, phaseLabel, labelX, y, scaleColorAlpha(0xFFB07030, alpha), false);
            y += 13;
        }
        // Description
        y = drawWrappedLines(
                graphics,
                focusedChoice.description().isBlank() ? "No inscription accompanies this archetype yet." : focusedChoice.description(),
                textX, y, textW, contentBottom - 22,
                scaleColorAlpha(0xFF3B271A, alpha)
        );
        // Disciplines section — only if space remains
        if (y + 22 <= contentBottom && !focusedChoice.startingDisciplines().isBlank()) {
            y += 6;
            graphics.drawString(font, "Disciplines", textX, y, scaleColorAlpha(0xFF8B6444, alpha), false);
            y += 12;
            drawWrappedLines(graphics, focusedChoice.startingDisciplines(), textX, y, textW, contentBottom, scaleColorAlpha(0xFF3B271A, alpha));
        }
        // Nav hints inside the panel's bottom strip
        if (reelFlowController.phase() == ReelFlowController.FlowPhase.SELECTING) {
            int hintY = hintStripTop + 6;
            int hintColor = scaleColorAlpha(0xFF6F513C, alpha);
            int centerX = px + pw / 2;
            String leftHint = "\u00AB A";
            String rightHint = "D \u00BB";
            graphics.drawString(font, leftHint, centerX - font.width(leftHint) - 8, hintY, hintColor, false);
            graphics.drawString(font, rightHint, centerX + 8, hintY, hintColor, false);
            String dragHint = "Drag to rotate \u2022 release to settle";
            int dragHintX = centerX - (font.width(dragHint) / 2);
            graphics.drawString(font, dragHint, dragHintX, hintY - 11, hintColor, false);
        }
    }
    private void renderArchetypeDropTargetHint(GuiGraphics graphics) {
        BookSceneRenderer.ScreenRect slot = archetypeDropTargetRect();
        if (slot == null) {
            return;
        }
        int left = Math.round(slot.x());
        int top = Math.round(slot.y());
        int width = Math.max(1, Math.round(slot.width()));
        int height = Math.max(1, Math.round(slot.height()));
        int tint = archetypeReelState.dropTargetHovered() ? 0x4AA3E66A : 0x304A2D17;
        int border = archetypeReelState.dropTargetHovered() ? 0xFFD3F2A9 : 0xFFC18A62;
        graphics.fill(left, top, left + width, top + height, tint);
        graphics.renderOutline(left, top, width, height, border);
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
    private InfoPanelLayoutService.PanelRect staticArchetypeReelPanelRect() {
        return infoPanelLayoutService.resolveStaticReelArchetypePanelRect(width, height);
    }
    private void renderArchetypeReelInfoPanelTopmost(GuiGraphics graphics) {
        if (!shouldRenderArchetypeReel()) {
            return;
        }
        if (archetypeReelState.dragging()) {
            return;
        }
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 640.0F);
        try {
            renderArchetypeReelInfoPanel(graphics);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }
    private int drawWrappedLines(GuiGraphics graphics, String text, int x, int y, int width, int maxY, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (y > maxY) {
                break;
            }
            graphics.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }
    private ArchetypeReelState reelStateForPresentation() {
        return shouldRenderArchetypeReel() ? archetypeReelState : null;
    }
    private static int scaleColorAlpha(int argb, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(((argb >>> 24) & 0xFF) * alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
