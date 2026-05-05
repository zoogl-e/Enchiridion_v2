package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalTemplateEditor;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

/**
 * Coordinates and routes input/rendering for book editor tools.
 * Currently wraps the Journal Template Editor and handles the spread refresh lifecycle.
 */
public final class BookEditorCoordinator {
    private final LevelRpgJournalTemplateEditor templateEditor = new LevelRpgJournalTemplateEditor();

    public void render(
            GuiGraphics graphics,
            Font font,
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            int height
    ) {
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
            restoreExpectedRenderState();
        }
    }

    public boolean keyPressed(int keyCode, int modifiers, BookScreenController controller, BookViewState viewState) {
        return templateEditor.keyPressed(keyCode, modifiers, controller, viewState, () -> refreshSpread(controller, viewState));
    }

    public boolean charTyped(char codePoint, BookScreenController controller, BookViewState viewState) {
        return templateEditor.charTyped(codePoint, controller, viewState, () -> refreshSpread(controller, viewState));
    }

    public boolean mouseClicked(
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            double mouseX,
            double mouseY,
            int button
    ) {
        return templateEditor.mouseClicked(controller, viewState, sceneRenderer, mouseX, mouseY, button);
    }

    public boolean mouseDragged(
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            double mouseX,
            double mouseY
    ) {
        return templateEditor.mouseDragged(controller, viewState, sceneRenderer, mouseX, mouseY);
    }

    public boolean mouseReleased(int button) {
        return templateEditor.mouseReleased(button);
    }

    private void refreshSpread(BookScreenController controller, BookViewState viewState) {
        controller.reloadSpread();
        viewState.refreshDisplayedSpread(controller);
    }

    private void restoreExpectedRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
