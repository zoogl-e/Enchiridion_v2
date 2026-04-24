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
import net.zoogle.enchiridion.client.levelrpg.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalIntroFlowState;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalTemplateEditor;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class BookScreen extends Screen {
    private final BookScreenController controller;
    private final BookSceneRenderer sceneRenderer = new BookSceneRenderer();
    private final PageCanvasRenderer pageRenderer = new PageCanvasRenderer();
    private final BookInteractionResolver interactionResolver = new BookInteractionResolver();
    private final BookViewState viewState = new BookViewState();
    private final BookInputController inputController = new BookInputController();
    private final BookOverlayRenderer overlayRenderer = new BookOverlayRenderer();
    private final LevelRpgJournalTemplateEditor templateEditor = new LevelRpgJournalTemplateEditor();
    private final ArchetypeReelState archetypeReelState = new ArchetypeReelState();
    private Component overlayTitle;

    public BookScreen(BookDefinition definition) {
        this(definition, createContext(definition));
    }

    public BookScreen(BookDefinition definition, BookContext context) {
        super(definition.provider().displayTitle(context, definition.title()));
        this.controller = new BookScreenController(definition, context);
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
        archetypeReelState.tick(controller.frontCoverCardState().boundArchetypeId());
        if (archetypeReelState.readyToRequestBind()) {
            JournalArchetypeChoice focusedChoice = archetypeReelState.focusedChoice();
            if (focusedChoice != null && requestArchetypeBinding(focusedChoice.focusId(), false)) {
                archetypeReelState.markBindingRequestIssued();
            }
        }
        if (archetypeReelState.readyToCloseAfterBoundReturn()) {
            archetypeReelState.close();
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

        renderBackground(graphics, mouseX, mouseY, partialTick);
        preparePresentation(mouseX, mouseY);
        if (archetypeReelState.active()) {
            BookSceneRenderer.ScreenRect focusedCardRect = sceneRenderer.focusedArchetypeReelCardRect(viewState.layout(), archetypeReelState);
            float tiltAnchorX = focusedCardRect == null ? (width / 2.0f) : focusedCardRect.x() + (focusedCardRect.width() / 2.0f);
            float tiltAnchorY = focusedCardRect == null ? (height / 2.0f) : focusedCardRect.y() + (focusedCardRect.height() / 2.0f);
            archetypeReelState.updateFocusTilt(tiltAnchorX, tiltAnchorY, mouseX, mouseY);
            updateArchetypeReelHover(mouseX, mouseY);
        }
        BookInteractionResolver.Resolution interaction = resolveInteraction(mouseX, mouseY);
        renderBook(graphics, interaction, mouseX, mouseY, partialTick);

        if (BookDebugSettings.showGuiTextDebug()) {
            renderGuiTextDebug(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (archetypeReelState.active()) {
            renderArchetypeReelInfoPanel(graphics);
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (templateEditor.keyPressed(keyCode, modifiers, controller, viewState, () -> {
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        })) {
            return true;
        }
        if (archetypeReelState.active()) {
            return handleArchetypeReelKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return inputController.keyPressed(controller, viewState, keyCode, () -> super.keyPressed(keyCode, scanCode, modifiers), super::onClose);
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
        if (controller.isClosed() || controller.isArriving()) {
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
        if (archetypeReelState.active()) {
            return handleArchetypeReelClick(mouseX, mouseY, button);
        }
        if (inputController.mouseClicked(controller, viewState, viewState.layout(), sceneRenderer, mouseX, mouseY, button)) {
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

    public boolean openSkillProjection(String focusId) {
        return controller.beginProjection(focusId);
    }

    public boolean openProjection(String focusId) {
        return controller.beginProjection(focusId);
    }

    public boolean openArchetypeReel() {
        controller.closeProjection();
        List<JournalArchetypeChoice> choices = LevelRpgJournalInteractionBridge.availableArchetypes();
        BookFrontCoverCardState cardState = controller.frontCoverCardState();
        return archetypeReelState.open(choices, cardState.boundArchetypeId(), cardState.selectedArchetypeId());
    }

    public boolean closeProjection() {
        return controller.closeProjection();
    }

    public boolean beginArchetypeBinding(String focusId) {
        return requestArchetypeBinding(focusId, true);
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
        if (archetypeReelState.active()) {
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
        if (!archetypeReelState.active()) {
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
        return archetypeReelState.active()
                ? net.zoogle.enchiridion.client.anim.BookAnimState.IDLE_SKILLTREE
                : controller.visualState();
    }

    private boolean handleArchetypeReelKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (canCancelArchetypeReel()) {
                archetypeReelState.close();
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
        JournalArchetypeChoice focusedChoice = archetypeReelState.focusedChoice();
        if (focusedChoice == null) {
            return false;
        }
        LevelRpgJournalIntroFlowState.get().setSelectedFocus(focusedChoice.focusId());
        archetypeReelState.beginConfirming();
        return true;
    }

    private boolean canCancelArchetypeReel() {
        return archetypeReelState.phase() == ArchetypeReelState.Phase.IDLE
                || archetypeReelState.phase() == ArchetypeReelState.Phase.MOVING;
    }

    private boolean requestArchetypeBinding(String focusId, boolean closeReelOnStart) {
        boolean begun = controller.beginArchetypeBinding(focusId);
        if (begun) {
            try {
                archetypeReelState.markSelected(net.minecraft.resources.ResourceLocation.parse(focusId));
            } catch (Exception ignored) {
                // keep the reel state resilient to malformed focus ids
            }
            if (closeReelOnStart) {
                archetypeReelState.close();
                controller.reloadSpread();
                viewState.refreshDisplayedSpread(controller);
            }
        }
        return begun;
    }

    private void renderArchetypeReelInfoPanel(GuiGraphics graphics) {
        JournalArchetypeChoice focusedChoice = archetypeReelState.focusedChoice();
        if (focusedChoice == null || viewState.layout() == null) {
            return;
        }
        BookSceneRenderer.ScreenRect focusedCardRect = sceneRenderer.focusedArchetypeReelCardRect(viewState.layout(), archetypeReelState);
        int panelX = focusedCardRect == null
                ? Math.min(width - 262, viewState.layout().bookX() + viewState.layout().bookWidth() + 74)
                : Math.min(width - 262, Math.round(focusedCardRect.x() + focusedCardRect.width() + 52.0f));
        int panelY = focusedCardRect == null
                ? viewState.layout().bookY() + 46
                : Math.max(24, Math.round(focusedCardRect.y() + 8.0f));
        int panelWidth = Math.min(240, width - panelX - 22);
        if (panelWidth < 120) {
            return;
        }
        int panelHeight = 182;
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xB819120D);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xDCCBAF86);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF6D4A2C);

        int textX = panelX + 14;
        int y = panelY + 14;
        graphics.drawString(font, focusedChoice.title(), textX, y, 0xFF2A180F, false);
        y += 18;
        graphics.drawString(font, switch (archetypeReelState.phase()) {
            case CONFIRMING -> "Confirming";
            case BURNING -> "Inscribing";
            case BOUND_RETURN -> "Returning";
            default -> "Archetype";
        }, textX, y, 0xFF6F513C, false);
        y += 18;

        y = drawWrappedLines(graphics, focusedChoice.description().isBlank() ? "No inscription accompanies this archetype yet." : focusedChoice.description(), textX, y, panelWidth - 28, 0xFF3B271A);
        y += 10;
        graphics.drawString(font, "Affected Skills", textX, y, 0xFF6D4A2C, false);
        y += 16;
        drawWrappedLines(graphics, focusedChoice.affectedSpreadText(), textX, y, panelWidth - 28, 0xFF3B271A);
    }

    private int drawWrappedLines(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    private void renderGuiTextDebug(GuiGraphics graphics) {
        pageRenderer.renderPage(
                graphics,
                viewState.displayedSpread().left(),
                viewState.layout().leftPageX(),
                viewState.layout().leftPageY(),
                viewState.layout().pageWidth(),
                viewState.layout().pageHeight(),
                viewState.textAlpha()
        );
        pageRenderer.renderPage(
                graphics,
                viewState.displayedSpread().right(),
                viewState.layout().rightPageX(),
                viewState.layout().rightPageY(),
                viewState.layout().pageWidth(),
                viewState.layout().pageHeight(),
                viewState.textAlpha(),
                PageCanvasRenderer.RIGHT_PAGE_INSET
        );
    }
}
