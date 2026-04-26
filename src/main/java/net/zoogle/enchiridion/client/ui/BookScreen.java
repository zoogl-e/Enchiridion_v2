package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.client.levelrpg.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgArchetypeBindGateway;
import net.zoogle.enchiridion.client.levelrpg.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgIntroFlowAdapter;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalTemplateEditor;
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
    private final LevelRpgJournalTemplateEditor templateEditor = new LevelRpgJournalTemplateEditor();
    private final ArchetypeReelState archetypeReelState = new ArchetypeReelState();
    private final ReelFlowController reelFlowController = new ReelFlowController();
    private final LevelRpgArchetypeBindGateway archetypeBindGateway = new LevelRpgArchetypeBindGateway();
    private Component overlayTitle;

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

    @Override
    public void tick() {
        controller.tick();
        reelFlowController.tick(
                archetypeReelState,
                controller.frontCoverCardState().boundArchetypeId(),
                controller.context(),
                archetypeBindGateway
        );
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

        sceneRenderer.beginPresentationFrame(archetypeReelState, width, height);
        try {
            renderImpl(graphics, mouseX, mouseY, partialTick);
        } finally {
            sceneRenderer.endPresentationFrame();
        }
    }

    private void renderImpl(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        preparePresentation(mouseX, mouseY);
        if (modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
            BookSceneRenderer.ScreenRect focusedCardRect = sceneRenderer.focusedArchetypeReelCardRect(viewState.layout(), archetypeReelState);
            float tiltAnchorX = focusedCardRect == null ? (width / 2.0f) : focusedCardRect.x() + (focusedCardRect.width() / 2.0f);
            float tiltAnchorY = focusedCardRect == null ? (height / 2.0f) : focusedCardRect.y() + (focusedCardRect.height() / 2.0f);
            archetypeReelState.updateFocusTilt(tiltAnchorX, tiltAnchorY, mouseX, mouseY);
            updateArchetypeReelHover(mouseX, mouseY);
        }
        BookInteractionResolver.Resolution interaction = resolveInteraction(mouseX, mouseY);
        renderBook(graphics, interaction, mouseX, mouseY, partialTick);
        if (modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
            sceneRenderer.renderArchetypeReelCardTitleOverlays(graphics, font, viewState.layout(), archetypeReelState);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
            renderArchetypeReelInfoPanel(graphics);
            renderArchetypeReelNavigationHints(graphics);
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
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 700.0F);
        try {
            templateEditor.renderOverlay(graphics, font, controller, viewState, sceneRenderer, height);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static int scaleColorAlpha(int argb, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(((argb >>> 24) & 0xFF) * alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (templateEditor.keyPressed(keyCode, modifiers, controller, viewState, () -> {
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        })) {
            return true;
        }
        if (modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
            return handleArchetypeReelKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
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
        if (templateEditor.charTyped(codePoint, controller, viewState, () -> {
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        })) {
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
        if (templateEditor.mouseClicked(controller, viewState, sceneRenderer, mouseX, mouseY, button)) {
            return true;
        }
        if (modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
            return handleArchetypeReelClick(mouseX, mouseY, button);
        }
        if (inputController.mouseClicked(controller, modeCoordinator, viewState, viewState.layout(), sceneRenderer, mouseX, mouseY, button)) {
            return true;
        }
        return controller.isJournalReadable() && super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (templateEditor.mouseDragged(controller, viewState, sceneRenderer, mouseX, mouseY)) {
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
            return true;
        }
        return inputController.mouseDragged(controller, viewState, button, mouseX, mouseY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (templateEditor.mouseReleased(button)) {
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
            return true;
        }
        return inputController.mouseReleased(viewState, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean jumpToSpread(int spreadIndex) {
        return controller.isJournalReadable() && controller.jumpToSpread(spreadIndex);
    }

    public boolean openProjection(String focusId) {
        return controller.beginProjection(focusId);
    }

    public boolean openArchetypeReel() {
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
        if (modeCoordinator.shouldResolvePageInteraction(controller, archetypeReelState)) {
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
                archetypeReelState,
                viewState.closedBookHovered(),
                viewState.inspectYaw(),
                viewState.inspectPitch()
        );
    }

    private BookFrontCoverCardState currentFrontCoverCardState() {
        BookFrontCoverCardState base = controller.frontCoverCardState();
        if (!modeCoordinator.shouldHandleReelInput(archetypeReelState)) {
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
        return modeCoordinator.shouldHandleReelInput(archetypeReelState)
                ? net.zoogle.enchiridion.client.anim.BookAnimState.IDLE_SKILLTREE
                : controller.visualState();
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
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            return confirmArchetypeReelFocus();
        }
        return false;
    }

    private boolean handleArchetypeReelClick(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (canCancelArchetypeReel()) {
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
        if (hoveredIndex == null) {
            return false;
        }
        if (hoveredIndex != archetypeReelState.focusedIndex()) {
            archetypeReelState.focusIndex(hoveredIndex);
            return true;
        }
        return confirmArchetypeReelFocus();
    }

    private boolean confirmArchetypeReelFocus() {
        if (archetypeReelState.phase() != ArchetypeReelState.Phase.IDLE) {
            return false;
        }
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
            case COMMITTING, REQUESTING_BIND -> "Inscribing\u2026";
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
        }
    }

    private void renderArchetypeReelNavigationHints(GuiGraphics graphics) {
        // Hints are now rendered inside renderArchetypeReelInfoPanel.
    }

    private InfoPanelLayoutService.PanelRect staticArchetypeReelPanelRect() {
        return infoPanelLayoutService.resolveStaticReelArchetypePanelRect(width, height);
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

}
