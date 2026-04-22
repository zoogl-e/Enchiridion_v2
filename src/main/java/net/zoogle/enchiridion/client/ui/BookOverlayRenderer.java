package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
            if (isOverlayRegionTarget(viewState.hoveredInteractiveTarget())) {
                drawRegionHover(graphics, viewState.hoveredInteractiveTarget());
            }

            drawVisibleOverlayActions(graphics, font, controller, viewState);

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
        if (viewState.closedBookHovered() && controller.isClosed()) {
            graphics.renderTooltip(font, viewState.tooltipLines(title), mouseX, mouseY);
        } else if (viewState.hoveredProjectionButton() != null && viewState.hoveredProjectionButton().view().primaryActionLabel() != null) {
            graphics.renderTooltip(font, viewState.hoveredProjectionButton().view().primaryActionLabel(), mouseX, mouseY);
        } else if (viewState.hoveredInteractiveTarget() != null && viewState.hoveredInteractiveTarget().tooltip() != null) {
            int tooltipX = Math.round(viewState.hoveredInteractiveTarget().screenRect().x() + (viewState.hoveredInteractiveTarget().screenRect().width() / 2.0f));
            int tooltipY = Math.round(viewState.hoveredInteractiveTarget().screenRect().y() + Math.max(0.0f, viewState.hoveredInteractiveTarget().screenRect().height() / 2.0f));
            graphics.renderTooltip(font, viewState.hoveredInteractiveTarget().tooltip(), tooltipX, tooltipY);
        }
    }

    private void drawVisibleOverlayActions(GuiGraphics graphics, Font font, BookScreenController controller, BookViewState viewState) {
        if (viewState.layout() == null || !controller.isJournalReadable()) {
            return;
        }
        for (BookInteractionResolver.ResolvedInteractiveTarget region : viewState.resolvedInteractiveTargets()) {
            if (!isOverlayLabeledAction(region)) {
                continue;
            }
            boolean hovered = viewState.hoveredInteractiveTarget() != null
                    && viewState.hoveredInteractiveTarget().stableId().equals(region.stableId());
            drawVisibleActionButton(
                    graphics,
                    font,
                    region.region().visibleLabel(),
                    Math.round(region.screenRect().x()),
                    Math.round(region.screenRect().y()),
                    Math.round(region.screenRect().width()),
                    Math.round(region.screenRect().height()),
                    hovered
            );
        }
    }

    private void drawVisibleActionButton(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int height, boolean hovered) {
        int drawWidth = Math.max(36, font.width(label) + 12);
        int drawHeight = Math.max(font.lineHeight + 4, height);
        int drawX = x;
        int drawY = y;
        int fill = hovered ? 0xAA65421E : 0x88342011;
        int border = hovered ? 0xFFF0D48A : 0xD0C8A86A;
        graphics.fill(drawX, drawY, drawX + drawWidth, drawY + drawHeight, fill);
        graphics.fill(drawX, drawY, drawX + drawWidth, drawY + 1, border);
        graphics.fill(drawX, drawY + drawHeight - 1, drawX + drawWidth, drawY + drawHeight, border);
        graphics.fill(drawX, drawY, drawX + 1, drawY + drawHeight, border);
        graphics.fill(drawX + drawWidth - 1, drawY, drawX + drawWidth, drawY + drawHeight, border);
        int labelY = drawY + Math.max(1, (drawHeight - font.lineHeight) / 2);
        drawCenteredText(graphics, font, label, drawX, drawWidth, labelY, hovered ? 0xFFFFF1 : 0xF8E8C8);
    }

    private void drawCenteredText(GuiGraphics graphics, Font font, Component text, int x, int width, int y, int color) {
        int textWidth = font.width(text);
        graphics.drawString(font, text, x + (width - textWidth) / 2, y, color, false);
    }

    private static void drawRegionHover(GuiGraphics graphics, BookInteractionResolver.ResolvedInteractiveTarget region) {
        int x0 = Math.round(region.screenRect().x());
        int y0 = Math.round(region.screenRect().y());
        int x1 = Math.round(region.screenRect().x() + region.screenRect().width());
        int y1 = Math.round(region.screenRect().y() + region.screenRect().height());
        graphics.fill(x0, y0, x1, y1, 0x221B140D);
        graphics.fill(x0, y0, x1, y0 + 1, 0x90D7C38A);
        graphics.fill(x0, y1 - 1, x1, y1, 0x90D7C38A);
        graphics.fill(x0, y0, x0 + 1, y1, 0x90D7C38A);
        graphics.fill(x1 - 1, y0, x1, y1, 0x90D7C38A);
    }

    private static boolean isOverlayRegionTarget(BookInteractionResolver.ResolvedInteractiveTarget target) {
        return target != null
                && target.interactiveElement() == null
                && target.role() == BookInteractionResolver.InteractiveVisualRole.PAGE_REGION;
    }

    private static boolean isOverlayLabeledAction(BookInteractionResolver.ResolvedInteractiveTarget target) {
        return target != null
                && target.interactiveElement() == null
                && target.role() == BookInteractionResolver.InteractiveVisualRole.LABELED_ACTION
                && target.region() != null;
    }
}
