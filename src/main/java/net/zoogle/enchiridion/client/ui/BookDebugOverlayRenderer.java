package net.zoogle.enchiridion.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

final class BookDebugOverlayRenderer {
    void renderDebugOverlay(
            GuiGraphics graphics,
            Font font,
            int screenHeight,
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            BookInteractionResolver interactionResolver,
            boolean debugPageLocalInteraction,
            boolean debugHoveredInteractiveTextBounds,
            int mouseX,
            int mouseY
    ) {
        if (debugHoveredInteractiveTextBounds) {
            drawHoveredInteractiveTextBounds(graphics, viewState.hoveredInteractiveTarget());
        }
        if (!debugPageLocalInteraction) {
            return;
        }

        if (viewState.pageInteractionDebugState() != null) {
            BookInteractionResolver.PageDebugSide debugCursorSide = debugCursorSide(viewState.pageInteractionDebugState(), viewState.hoveredInteractiveTarget());
            drawPageSideDebug(graphics, font, controller, viewState, sceneRenderer, viewState.pageInteractionDebugState().left(), 0xCC4FD6A8, 0xFFB9FFF0, debugCursorSide, mouseX, mouseY);
            drawPageSideDebug(graphics, font, controller, viewState, sceneRenderer, viewState.pageInteractionDebugState().right(), 0xCCD66A4F, 0xFFFFE0B9, debugCursorSide, mouseX, mouseY);
            int debugY = screenHeight - 38;
            drawDebugLine(graphics, font, describePageDebug("L", viewState.pageInteractionDebugState().left()), 8, debugY);
            drawDebugLine(graphics, font, describePageDebug("R", viewState.pageInteractionDebugState().right()), 8, debugY + 10);
        }

        for (BookInteractionResolver.ResolvedInteractiveTarget target : viewState.resolvedInteractiveTargets()) {
            if (target.interactiveElement() == null) {
                continue;
            }
            int color = target == viewState.hoveredInteractiveTarget()
                    ? 0xCCAA33FF
                    : targetInsideVisiblePage(target, viewState.pageInteractionDebugState()) ? 0xAA33CC66 : 0xCCFF4444;
            drawQuadOutline(graphics, target.screenQuad(), color);
        }
        if (viewState.hoveredInteractiveTarget() != null && viewState.hoveredInteractiveTarget().interactiveElement() != null) {
            BookSceneRenderer.PageLocalPoint localPoint = sceneRenderer.pageLocalPoint(
                    viewState.layout(),
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    viewState.currentProjectionFocusOffset(),
                    interactionResolver.pageSideFor(viewState.displayedSpread(), viewState.hoveredInteractiveTarget().interactiveElement()),
                    mouseX,
                    mouseY
            );
            if (localPoint != null) {
                graphics.drawString(font, String.format("page %.1f, %.1f", localPoint.localX(), localPoint.localY()), 8, screenHeight - 16, 0xFFFFFFFF, false);
            }
        }
    }

    private void drawPageSideDebug(
            GuiGraphics graphics,
            Font font,
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            BookInteractionResolver.PageDebugSide side,
            int quadColor,
            int pointColor,
            BookInteractionResolver.PageDebugSide debugCursorSide,
            int mouseX,
            int mouseY
    ) {
        if (side == null || side.bounds() == null) {
            return;
        }
        drawQuadOutline(graphics, side.bounds(), quadColor);
        drawContentRegionInset(graphics, side.bounds(), 10.0f, 10.0f, 0x66FFFFFF);
        if (debugCursorSide != null && debugCursorSide == side) {
            int px = mouseX;
            int py = mouseY;
            graphics.fill(px - 2, py - 1, px + 3, py + 2, pointColor);
            graphics.fill(px - 1, py - 2, px + 2, py + 3, pointColor);
        }
        if (side.containingText() != null) {
            BookSceneRenderer.ScreenRect rect = sceneRenderer.projectPageRect(
                    viewState.layout(),
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    viewState.currentProjectionFocusOffset(),
                    side.pageSide(),
                    side.containingText().x(),
                    side.containingText().y(),
                    side.containingText().width(),
                    side.containingText().height()
            );
            drawRectOutline(graphics, rect, 0xFFFF66CC);
        }
    }

    private String describePageDebug(String prefix, BookInteractionResolver.PageDebugSide side) {
        if (side == null) {
            return prefix + ": no side";
        }
        if (side.localPoint() == null) {
            return prefix + ": local=null hit=none";
        }
        String hit = describeInteractiveElement(side.containingText());
        return String.format(
                "%s: local=(%.1f, %.1f) hit=%s quad=(%.1f,%.1f)-(%.1f,%.1f)",
                prefix,
                side.localPoint().localX(),
                side.localPoint().localY(),
                hit,
                side.bounds().screenX(),
                side.bounds().screenY(),
                side.bounds().screenX() + side.bounds().screenWidth(),
                side.bounds().screenY() + side.bounds().screenHeight()
        );
    }

    private String describeInteractiveElement(BookPageElement.InteractiveElement element) {
        if (element == null) {
            return "none";
        }
        if (element instanceof BookPageElement.InteractiveTextElement text) {
            return text.text().getString();
        }
        if (element instanceof BookPageElement.ButtonElement button) {
            return button.label().getString();
        }
        return element.stableId();
    }

    private void drawHoveredInteractiveTextBounds(GuiGraphics graphics, BookInteractionResolver.ResolvedInteractiveTarget hoveredTarget) {
        if (hoveredTarget == null) {
            return;
        }
        drawQuadOutline(graphics, hoveredTarget.screenQuad(), 0xEEFFCC33);
        drawQuadOutline(graphics, insetQuad(hoveredTarget.screenQuad(), 1.0f), 0xEE6A38FF);
    }

    private void drawQuadOutline(GuiGraphics graphics, BookSceneRenderer.PageSurfaceBounds bounds, int color) {
        drawLineRect(graphics, bounds.topLeft(), bounds.topRight(), color);
        drawLineRect(graphics, bounds.topRight(), bounds.bottomRight(), color);
        drawLineRect(graphics, bounds.bottomRight(), bounds.bottomLeft(), color);
        drawLineRect(graphics, bounds.bottomLeft(), bounds.topLeft(), color);
    }

    private void drawQuadOutline(GuiGraphics graphics, BookSceneRenderer.ScreenQuad quad, int color) {
        drawLineRect(graphics, quad.topLeft(), quad.topRight(), color);
        drawLineRect(graphics, quad.topRight(), quad.bottomRight(), color);
        drawLineRect(graphics, quad.bottomRight(), quad.bottomLeft(), color);
        drawLineRect(graphics, quad.bottomLeft(), quad.topLeft(), color);
    }

    private void drawLineRect(GuiGraphics graphics, BookSceneRenderer.ScreenPoint from, BookSceneRenderer.ScreenPoint to, int color) {
        int minX = Math.round(Math.min(from.x(), to.x()));
        int minY = Math.round(Math.min(from.y(), to.y()));
        int maxX = Math.round(Math.max(from.x(), to.x()));
        int maxY = Math.round(Math.max(from.y(), to.y()));
        graphics.fill(minX, minY, Math.max(minX + 1, maxX + 1), Math.max(minY + 1, maxY + 1), color);
    }

    private void drawRectOutline(GuiGraphics graphics, BookSceneRenderer.ScreenRect rect, int color) {
        int x0 = Math.round(rect.x());
        int y0 = Math.round(rect.y());
        int x1 = Math.round(rect.x() + rect.width());
        int y1 = Math.round(rect.y() + rect.height());
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    private BookSceneRenderer.ScreenQuad insetQuad(BookSceneRenderer.ScreenQuad quad, float inset) {
        return new BookSceneRenderer.ScreenQuad(
                offsetTowardCenter(quad.topLeft(), quad, inset),
                offsetTowardCenter(quad.topRight(), quad, inset),
                offsetTowardCenter(quad.bottomRight(), quad, inset),
                offsetTowardCenter(quad.bottomLeft(), quad, inset)
        );
    }

    private BookSceneRenderer.ScreenPoint offsetTowardCenter(BookSceneRenderer.ScreenPoint point, BookSceneRenderer.ScreenQuad quad, float amount) {
        float centerX = (quad.topLeft().x() + quad.topRight().x() + quad.bottomRight().x() + quad.bottomLeft().x()) / 4.0f;
        float centerY = (quad.topLeft().y() + quad.topRight().y() + quad.bottomRight().y() + quad.bottomLeft().y()) / 4.0f;
        float dx = centerX - point.x();
        float dy = centerY - point.y();
        float length = (float) Math.sqrt((dx * dx) + (dy * dy));
        if (length < 0.001f) {
            return point;
        }
        float scale = amount / length;
        return new BookSceneRenderer.ScreenPoint(point.x() + (dx * scale), point.y() + (dy * scale));
    }

    private boolean targetInsideVisiblePage(BookInteractionResolver.ResolvedInteractiveTarget target, BookInteractionResolver.PageInteractionDebugState debugState) {
        if (debugState == null) {
            return true;
        }
        BookInteractionResolver.PageDebugSide side = target.pageSide() == net.zoogle.enchiridion.api.BookPageSide.LEFT ? debugState.left() : debugState.right();
        if (side == null || side.bounds() == null) {
            return true;
        }
        float x0 = side.bounds().screenX();
        float y0 = side.bounds().screenY();
        float x1 = x0 + side.bounds().screenWidth();
        float y1 = y0 + side.bounds().screenHeight();
        return target.screenRect().x() >= x0
                && target.screenRect().y() >= y0
                && target.screenRect().x() + target.screenRect().width() <= x1
                && target.screenRect().y() + target.screenRect().height() <= y1;
    }

    private void drawContentRegionInset(GuiGraphics graphics, BookSceneRenderer.PageSurfaceBounds bounds, float insetX, float insetY, int color) {
        BookSceneRenderer.ScreenPoint topLeft = insetPoint(bounds, insetX, insetY);
        BookSceneRenderer.ScreenPoint topRight = insetPoint(bounds, bounds.localWidth() - insetX, insetY);
        BookSceneRenderer.ScreenPoint bottomRight = insetPoint(bounds, bounds.localWidth() - insetX, bounds.localHeight() - insetY);
        BookSceneRenderer.ScreenPoint bottomLeft = insetPoint(bounds, insetX, bounds.localHeight() - insetY);
        drawLineRect(graphics, topLeft, topRight, color);
        drawLineRect(graphics, topRight, bottomRight, color);
        drawLineRect(graphics, bottomRight, bottomLeft, color);
        drawLineRect(graphics, bottomLeft, topLeft, color);
    }

    private BookSceneRenderer.ScreenPoint insetPoint(BookSceneRenderer.PageSurfaceBounds bounds, float localX, float localY) {
        float u = bounds.localWidth() <= 0.0f ? 0.0f : clamp(localX / bounds.localWidth(), 0.0f, 1.0f);
        float v = bounds.localHeight() <= 0.0f ? 0.0f : clamp(localY / bounds.localHeight(), 0.0f, 1.0f);
        float topX = lerp(bounds.topLeft().x(), bounds.topRight().x(), u);
        float topY = lerp(bounds.topLeft().y(), bounds.topRight().y(), u);
        float bottomX = lerp(bounds.bottomLeft().x(), bounds.bottomRight().x(), u);
        float bottomY = lerp(bounds.bottomLeft().y(), bounds.bottomRight().y(), u);
        return new BookSceneRenderer.ScreenPoint(lerp(topX, bottomX, v), lerp(topY, bottomY, v));
    }

    private static float lerp(float from, float to, float progress) {
        return from + ((to - from) * progress);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawDebugLine(GuiGraphics graphics, Font font, String text, int x, int y) {
        graphics.drawString(font, text, x, y, 0xFFFFFFFF, false);
    }

    private BookInteractionResolver.PageDebugSide debugCursorSide(
            BookInteractionResolver.PageInteractionDebugState debugState,
            BookInteractionResolver.ResolvedInteractiveTarget hoveredTarget
    ) {
        if (debugState == null) {
            return null;
        }
        if (hoveredTarget != null) {
            return hoveredTarget.pageSide() == net.zoogle.enchiridion.api.BookPageSide.LEFT ? debugState.left() : debugState.right();
        }
        if (debugState.left() != null && debugState.left().localPoint() != null) {
            return debugState.left();
        }
        if (debugState.right() != null && debugState.right().localPoint() != null) {
            return debugState.right();
        }
        return null;
    }
}
