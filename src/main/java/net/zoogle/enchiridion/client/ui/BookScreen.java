package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalTemplateEditor;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

public final class BookScreen extends Screen {
    private final BookScreenController controller;
    private final BookSceneRenderer sceneRenderer = new BookSceneRenderer();
    private final PageCanvasRenderer pageRenderer = new PageCanvasRenderer();
    private final BookInteractionResolver interactionResolver = new BookInteractionResolver();
    private final BookViewState viewState = new BookViewState();
    private final BookInputController inputController = new BookInputController();
    private final BookOverlayRenderer overlayRenderer = new BookOverlayRenderer();
    private final LevelRpgJournalTemplateEditor templateEditor = new LevelRpgJournalTemplateEditor();

    public BookScreen(BookDefinition definition) {
        this(definition, createContext(definition));
    }

    public BookScreen(BookDefinition definition, BookContext context) {
        super(definition.title());
        this.controller = new BookScreenController(definition, context);
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
        viewState.updateTextTransition(controller);
        viewState.updateProjectionFocusOffset(controller);
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
        BookInteractionResolver.Resolution interaction = resolveInteraction(mouseX, mouseY);
        renderBook(graphics, interaction, mouseX, mouseY, partialTick);

        if (BookDebugSettings.showGuiTextDebug()) {
            renderGuiTextDebug(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        overlayRenderer.renderTopmostOverlay(
                graphics,
                font,
                width,
                height,
                title,
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
        controller.beginClosing();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (templateEditor.mouseClicked(controller, viewState, sceneRenderer, mouseX, mouseY, button)) {
            return true;
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

    private void preparePresentation(int mouseX, int mouseY) {
        viewState.setClosedBookHovered(controller.isClosed() && sceneRenderer.isClosedBookHovered(viewState.layout(), mouseX, mouseY));
        sceneRenderer.preparePresentation(
                viewState.layout(),
                controller.visualState(),
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
        viewState.setInteraction(interaction);
        return interaction;
    }

    private void renderBook(GuiGraphics graphics, BookInteractionResolver.Resolution interaction, int mouseX, int mouseY, float partialTick) {
        sceneRenderer.renderBook(
                graphics,
                viewState.layout(),
                viewState.displayedSpread(),
                viewState.displayedSpreadIndex(),
                controller.visualState(),
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
                viewState.closedBookHovered(),
                viewState.inspectYaw(),
                viewState.inspectPitch()
        );
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
