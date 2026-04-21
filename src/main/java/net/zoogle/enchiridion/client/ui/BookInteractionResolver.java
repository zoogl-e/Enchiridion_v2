package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

import java.util.ArrayList;
import java.util.List;

final class BookInteractionResolver {
    Resolution resolve(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            int displayedSpreadIndex,
            float projectionFocusOffset,
            int mouseX,
            int mouseY,
            boolean debugPageLocalInteraction
    ) {
        if (layout == null || displayedSpread == null || controller.isProjectionVisible()) {
            return Resolution.empty();
        }
        List<ResolvedInteractiveTarget> targets = controller.isOpenReadable()
                ? resolveTargets(controller, sceneRenderer, layout, displayedSpread, displayedSpreadIndex, projectionFocusOffset)
                : List.of();
        ResolvedInteractiveTarget hoveredTarget = controller.isOpenReadable()
                ? resolveHoveredTarget(sceneRenderer, layout, controller, projectionFocusOffset, mouseX, mouseY, targets)
                : null;
        PageInteractionDebugState debugState = debugPageLocalInteraction && controller.isOpenReadable()
                ? buildPageInteractionDebugState(controller, sceneRenderer, layout, displayedSpread, projectionFocusOffset, mouseX, mouseY)
                : null;
        return new Resolution(hoveredTarget, targets, debugState);
    }

    BookPageElement.InteractiveTextElement hoveredInteractiveElement(Resolution resolution, BookSpread spread, BookPageSide pageSide) {
        if (resolution.hoveredTarget() == null || resolution.hoveredTarget().role() != InteractiveVisualRole.MANUSCRIPT_LINK) {
            return null;
        }
        BookPageElement.InteractiveTextElement text = resolution.hoveredTarget().textElement();
        return text != null && pageSideFor(spread, text) == pageSide ? text : null;
    }

    List<BookPageElement.InteractiveTextElement> pageInteractiveElements(BookSpread spread) {
        if (spread == null) {
            return List.of();
        }
        List<BookPageElement.InteractiveTextElement> elements = new ArrayList<>();
        for (BookPageElement element : spread.left().elements()) {
            if (element instanceof BookPageElement.InteractiveTextElement interactive) {
                elements.add(interactive);
            }
        }
        for (BookPageElement element : spread.right().elements()) {
            if (element instanceof BookPageElement.InteractiveTextElement interactive) {
                elements.add(interactive);
            }
        }
        return elements;
    }

    List<BookPageElement.InteractiveTextElement> interactiveTextElementsFor(BookSpread spread, BookPageSide pageSide) {
        if (spread == null) {
            return List.of();
        }
        List<BookPageElement> elements = pageSide == BookPageSide.LEFT ? spread.left().elements() : spread.right().elements();
        List<BookPageElement.InteractiveTextElement> interactive = new ArrayList<>();
        for (BookPageElement element : elements) {
            if (element instanceof BookPageElement.InteractiveTextElement text) {
                interactive.add(text);
            }
        }
        return interactive;
    }

    BookPageSide pageSideFor(BookSpread spread, BookPageElement.InteractiveTextElement element) {
        if (spread == null) {
            return BookPageSide.LEFT;
        }
        for (BookPageElement pageElement : spread.left().elements()) {
            if (pageElement == element) {
                return BookPageSide.LEFT;
            }
        }
        for (BookPageElement pageElement : spread.right().elements()) {
            if (pageElement == element) {
                return BookPageSide.RIGHT;
            }
        }
        return spread.left().elements().contains(element) ? BookPageSide.LEFT : BookPageSide.RIGHT;
    }

    private List<ResolvedInteractiveTarget> resolveTargets(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            int displayedSpreadIndex,
            float projectionFocusOffset
    ) {
        List<ResolvedInteractiveTarget> resolved = new ArrayList<>();
        for (BookPageElement.InteractiveTextElement text : pageInteractiveElements(displayedSpread)) {
            BookPageSide pageSide = pageSideFor(displayedSpread, text);
            resolved.add(new ResolvedInteractiveTarget(
                    targetId("text", pageSide, text.x(), text.y(), text.width(), text.height(), text.text().getString()),
                    pageSide,
                    text.x(),
                    text.y(),
                    text.width(),
                    text.height(),
                    text.tooltip(),
                    text.action(),
                    InteractiveVisualRole.MANUSCRIPT_LINK,
                    true,
                    sceneRenderer.projectPageRect(
                            layout,
                            controller.visualState(),
                            controller.animationProgress(),
                            controller.projectionProgress(),
                            projectionFocusOffset,
                            pageSide,
                            text.x(),
                            text.y(),
                            text.width(),
                            text.height()
                    ),
                    text,
                    null
            ));
        }
        for (BookInteractiveRegion region : controller.definition().provider().interactiveRegions(controller.context(), displayedSpreadIndex)) {
            InteractiveVisualRole role = region.visibleLabel() != null
                    ? InteractiveVisualRole.LABELED_ACTION
                    : InteractiveVisualRole.PAGE_REGION;
            resolved.add(new ResolvedInteractiveTarget(
                    targetId("region", region.pageSide(), region.x(), region.y(), region.width(), region.height(), region.visibleLabel() != null ? region.visibleLabel().getString() : ""),
                    region.pageSide(),
                    region.x(),
                    region.y(),
                    region.width(),
                    region.height(),
                    region.tooltip(),
                    region.action(),
                    role,
                    true,
                    sceneRenderer.projectPageRect(
                        layout,
                        controller.visualState(),
                        controller.animationProgress(),
                        controller.projectionProgress(),
                        projectionFocusOffset,
                        region.pageSide(),
                        region.x(),
                        region.y(),
                        region.width(),
                        region.height()
                    ),
                    null,
                    region
            ));
        }
        return List.copyOf(resolved);
    }

    private ResolvedInteractiveTarget resolveHoveredTarget(
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            int mouseX,
            int mouseY,
            List<ResolvedInteractiveTarget> targets
    ) {
        for (ResolvedInteractiveTarget target : targets) {
            BookSceneRenderer.PageLocalPoint localPoint = sceneRenderer.pageLocalPoint(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    projectionFocusOffset,
                    target.pageSide(),
                    mouseX,
                    mouseY
            );
            if (localPoint == null) {
                continue;
            }
            if (localPoint.localX() >= target.localX()
                    && localPoint.localX() < target.localX() + target.localWidth()
                    && localPoint.localY() >= target.localY()
                    && localPoint.localY() < target.localY() + target.localHeight()) {
                return target;
            }
        }
        return null;
    }

    private PageInteractionDebugState buildPageInteractionDebugState(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            float projectionFocusOffset,
            int mouseX,
            int mouseY
    ) {
        return new PageInteractionDebugState(
                buildPageDebugSide(controller, sceneRenderer, layout, displayedSpread, projectionFocusOffset, BookPageSide.LEFT, mouseX, mouseY),
                buildPageDebugSide(controller, sceneRenderer, layout, displayedSpread, projectionFocusOffset, BookPageSide.RIGHT, mouseX, mouseY)
        );
    }

    private PageDebugSide buildPageDebugSide(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int mouseX,
            int mouseY
    ) {
        BookSceneRenderer.PageSurfaceBounds bounds = sceneRenderer.pageSurfaceBounds(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                pageSide
        );
        BookSceneRenderer.PageLocalPoint localPoint = sceneRenderer.pageLocalPoint(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                pageSide,
                mouseX,
                mouseY
        );
        BookPageElement.InteractiveTextElement containing = null;
        if (localPoint != null) {
            for (BookPageElement.InteractiveTextElement text : interactiveTextElementsFor(displayedSpread, pageSide)) {
                if (localPoint.localX() >= text.x()
                        && localPoint.localX() < text.x() + text.width()
                        && localPoint.localY() >= text.y()
                        && localPoint.localY() < text.y() + text.height()) {
                    containing = text;
                    break;
                }
            }
        }
        BookSceneRenderer.ScreenPoint mouseProjection = localPoint == null
                ? null
                : sceneRenderer.projectPagePoint(
                        layout,
                        controller.visualState(),
                        controller.animationProgress(),
                        controller.projectionProgress(),
                        projectionFocusOffset,
                        pageSide,
                        localPoint.localX(),
                        localPoint.localY()
                );
        return new PageDebugSide(pageSide, bounds, localPoint, mouseProjection, containing);
    }

    record Resolution(
            ResolvedInteractiveTarget hoveredTarget,
            List<ResolvedInteractiveTarget> targets,
            PageInteractionDebugState debugState
    ) {
        static Resolution empty() {
            return new Resolution(null, List.of(), null);
        }
    }

    record ResolvedInteractiveTarget(
            String stableId,
            BookPageSide pageSide,
            int localX,
            int localY,
            int localWidth,
            int localHeight,
            net.minecraft.network.chat.Component tooltip,
            net.zoogle.enchiridion.api.BookRegionAction action,
            InteractiveVisualRole role,
            boolean enabled,
            BookSceneRenderer.ScreenRect screenRect,
            BookPageElement.InteractiveTextElement textElement,
            BookInteractiveRegion region
    ) {}

    enum InteractiveVisualRole {
        MANUSCRIPT_LINK,
        LABELED_ACTION,
        PAGE_REGION
    }

    record PageInteractionDebugState(PageDebugSide left, PageDebugSide right) {}

    record PageDebugSide(
            BookPageSide pageSide,
            BookSceneRenderer.PageSurfaceBounds bounds,
            BookSceneRenderer.PageLocalPoint localPoint,
            BookSceneRenderer.ScreenPoint mouseProjection,
            BookPageElement.InteractiveTextElement containingText
    ) {}

    private static String targetId(String prefix, BookPageSide side, int x, int y, int width, int height, String label) {
        return prefix + ":" + side + ":" + x + ":" + y + ":" + width + ":" + height + ":" + label;
    }
}
