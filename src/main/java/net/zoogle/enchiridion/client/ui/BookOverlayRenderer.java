package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

final class BookOverlayRenderer {
    private final BookProjectionOverlayRenderer projectionOverlayRenderer = new BookProjectionOverlayRenderer();
    private final BookDebugOverlayRenderer debugOverlayRenderer = new BookDebugOverlayRenderer();

    ProjectionButtonHit resolveProjectionButton(BookLayout layout, int screenWidth, int screenHeight, BookScreenController controller, int mouseX, int mouseY) {
        return projectionOverlayRenderer.resolveProjectionButton(layout, screenWidth, screenHeight, controller, mouseX, mouseY);
    }

    void renderTopmostOverlay(
            GuiGraphics graphics,
            Font font,
            int screenWidth,
            int screenHeight,
            Component title,
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            BookInteractionResolver interactionResolver,
            int mouseX,
            int mouseY,
            boolean debugPageLocalInteraction,
            boolean debugHoveredInteractiveTextBounds
    ) {
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);

        try {
            if (controller.isProjectionVisible()) {
                projectionOverlayRenderer.renderProjectionOverlay(graphics, font, screenWidth, screenHeight, controller, viewState);
            }

            debugOverlayRenderer.renderDebugOverlay(
                    graphics,
                    font,
                    screenHeight,
                    controller,
                    viewState,
                    sceneRenderer,
                    interactionResolver,
                    debugPageLocalInteraction,
                    debugHoveredInteractiveTextBounds,
                    mouseX,
                    mouseY
            );

            renderOverlayTooltips(graphics, font, title, controller, viewState, mouseX, mouseY);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private void renderOverlayTooltips(GuiGraphics graphics, Font font, Component title, BookScreenController controller, BookViewState viewState, int mouseX, int mouseY) {
        if (viewState.closedBookHovered() && controller.isClosed() && title != null && !title.getString().isBlank()) {
            graphics.renderTooltip(font, viewState.tooltipLines(title), mouseX, mouseY);
        } else if (viewState.hoveredProjectionButton() != null && viewState.hoveredProjectionButton().view().primaryActionLabel() != null) {
            graphics.renderTooltip(font, viewState.hoveredProjectionButton().view().primaryActionLabel(), mouseX, mouseY);
        } else if (viewState.hoveredInteractiveTarget() != null && viewState.hoveredInteractiveTarget().tooltip() != null) {
            int tooltipX = Math.round(viewState.hoveredInteractiveTarget().screenRect().x() + (viewState.hoveredInteractiveTarget().screenRect().width() / 2.0f));
            int tooltipY = Math.round(viewState.hoveredInteractiveTarget().screenRect().y() + Math.max(0.0f, viewState.hoveredInteractiveTarget().screenRect().height() / 2.0f));
            graphics.renderTooltip(font, viewState.hoveredInteractiveTarget().tooltip(), tooltipX, tooltipY);
        }
    }
}
