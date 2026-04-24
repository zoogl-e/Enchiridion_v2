package net.zoogle.enchiridion.client.ui;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
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
        List<PageInteractiveNode> targets = controller.isOpenReadable()
                ? resolveTargets(controller, sceneRenderer, layout, displayedSpread, displayedSpreadIndex, projectionFocusOffset)
                : List.of();
        PageInteractiveNode hoveredTarget = controller.isInteractionStableReadable()
                ? resolveHoveredTarget(sceneRenderer, layout, controller, projectionFocusOffset, mouseX, mouseY, targets)
                : null;
        PageInteractionDebugState debugState = debugPageLocalInteraction && controller.isOpenReadable()
                ? buildPageInteractionDebugState(controller, sceneRenderer, layout, projectionFocusOffset, mouseX, mouseY, targets)
                : null;
        return new Resolution(hoveredTarget, targets, debugState);
    }

    PageInteractiveNode hoveredInteractiveNode(Resolution resolution, BookPageSide pageSide) {
        if (resolution.hoveredTarget() == null) {
            return null;
        }
        return resolution.hoveredTarget().pageSide() == pageSide ? resolution.hoveredTarget() : null;
    }

    List<PageInteractiveNode> pageNodesFor(Resolution resolution, BookPageSide pageSide) {
        if (resolution == null || resolution.targets() == null || resolution.targets().isEmpty()) {
            return List.of();
        }
        List<PageInteractiveNode> nodes = new ArrayList<>();
        for (PageInteractiveNode target : resolution.targets()) {
            if (target.pageSide() == pageSide) {
                nodes.add(target);
            }
        }
        return List.copyOf(nodes);
    }

    private List<PageInteractiveNode> resolveTargets(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            int displayedSpreadIndex,
            float projectionFocusOffset
    ) {
        List<PageInteractiveNode> resolved = new ArrayList<>();
        for (ResolvedBookInteractionTarget target : resolvedTargets(
                displayedSpread,
                controller.definition().provider().interactiveRegions(controller.context(), displayedSpreadIndex),
                controller.definition().provider().trackedInteractiveRegions(controller.context(), displayedSpreadIndex)
        )) {
            resolved.add(buildNode(
                    controller,
                    sceneRenderer,
                    layout,
                    projectionFocusOffset,
                    target
            ));
        }
        return List.copyOf(resolved);
    }

    private PageInteractiveNode buildNode(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            float projectionFocusOffset,
            ResolvedBookInteractionTarget target
    ) {
        return new PageInteractiveNode(
                target.stableId(),
                target.pageSide(),
                target.localX(),
                target.localY(),
                target.localWidth(),
                target.localHeight(),
                target.label(),
                target.tooltip(),
                target.action(),
                target.enabled(),
                target.visualType(),
                screenRectForTarget(sceneRenderer, layout, controller, projectionFocusOffset, target),
                screenQuadForTarget(sceneRenderer, layout, controller, projectionFocusOffset, target),
                target.hitTestMode(),
                target.interactiveElement(),
                target.region(),
                target.trackedRegion()
        );
    }

    private PageInteractiveNode resolveHoveredTarget(
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            int mouseX,
            int mouseY,
            List<PageInteractiveNode> targets
    ) {
        BookSceneRenderer.PageLocalPoint leftLocalPoint = null;
        BookSceneRenderer.PageLocalPoint rightLocalPoint = null;
        for (PageInteractiveNode target : targets) {
            if (target.hitTestMode() == PageInteractiveNode.HitTestMode.SCREEN_SPACE) {
                if (containsScreenPoint(target.screenQuad(), mouseX, mouseY)) {
                    return target;
                }
                continue;
            }
            BookSceneRenderer.PageLocalPoint localPoint = target.pageSide() == BookPageSide.LEFT
                    ? (leftLocalPoint != null ? leftLocalPoint : (leftLocalPoint = pageLocalPoint(sceneRenderer, layout, controller, projectionFocusOffset, BookPageSide.LEFT, mouseX, mouseY)))
                    : (rightLocalPoint != null ? rightLocalPoint : (rightLocalPoint = pageLocalPoint(sceneRenderer, layout, controller, projectionFocusOffset, BookPageSide.RIGHT, mouseX, mouseY)));
            if (localPoint != null && target.contains(localPoint.localX(), localPoint.localY())) {
                return target;
            }
        }
        return null;
    }

    private PageInteractionDebugState buildPageInteractionDebugState(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            float projectionFocusOffset,
            int mouseX,
            int mouseY,
            List<PageInteractiveNode> targets
    ) {
        return new PageInteractionDebugState(
                buildPageDebugSide(controller, sceneRenderer, layout, projectionFocusOffset, BookPageSide.LEFT, mouseX, mouseY, targets),
                buildPageDebugSide(controller, sceneRenderer, layout, projectionFocusOffset, BookPageSide.RIGHT, mouseX, mouseY, targets)
        );
    }

    private PageDebugSide buildPageDebugSide(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int mouseX,
            int mouseY,
            List<PageInteractiveNode> targets
    ) {
        BookSceneRenderer.PageSurfaceBounds bounds = sceneRenderer.pageSurfaceBounds(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                pageSide
        );
        BookSceneRenderer.PageLocalPoint localPoint = pageLocalPoint(sceneRenderer, layout, controller, projectionFocusOffset, pageSide, mouseX, mouseY);
        PageInteractiveNode containing = null;
        if (localPoint != null) {
            for (PageInteractiveNode target : targets) {
                if (target.pageSide() == pageSide && target.contains(localPoint.localX(), localPoint.localY())) {
                    containing = target;
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

    private BookSceneRenderer.PageLocalPoint pageLocalPoint(
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int mouseX,
            int mouseY
    ) {
        return sceneRenderer.pageLocalPoint(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                pageSide,
                mouseX,
                mouseY
        );
    }

    private List<ResolvedBookInteractionTarget> resolvedTargets(
            BookSpread spread,
            List<BookInteractiveRegion> providerRegions,
            List<BookTrackedRegion> trackedRegions
    ) {
        List<ResolvedBookInteractionTarget> resolved = new ArrayList<>();
        if (spread != null) {
            addResolvedInteractiveElements(resolved, spread.left().elements(), BookPageSide.LEFT);
            addResolvedInteractiveElements(resolved, spread.right().elements(), BookPageSide.RIGHT);
        }
        for (BookInteractiveRegion region : providerRegions) {
            resolved.add(ResolvedBookInteractionTarget.fromRegion(region));
        }
        for (BookTrackedRegion trackedRegion : trackedRegions) {
            resolved.add(ResolvedBookInteractionTarget.fromTrackedRegion(trackedRegion));
        }
        return List.copyOf(resolved);
    }

    private BookSceneRenderer.ScreenRect screenRectForTarget(
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            ResolvedBookInteractionTarget target
    ) {
        if (target.trackedRegion() != null) {
            return sceneRenderer.projectTrackedRegionRect(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    projectionFocusOffset,
                    target.trackedRegion().anchor()
            );
        }
        return sceneRenderer.projectPageRect(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                target.pageSide(),
                target.localX(),
                target.localY(),
                target.localWidth(),
                target.localHeight()
        );
    }

    private BookSceneRenderer.ScreenQuad screenQuadForTarget(
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            ResolvedBookInteractionTarget target
    ) {
        if (target.trackedRegion() != null) {
            return sceneRenderer.projectTrackedRegionQuad(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    projectionFocusOffset,
                    target.trackedRegion().anchor()
            );
        }
        return sceneRenderer.projectPageQuad(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                target.pageSide(),
                target.localX(),
                target.localY(),
                target.localWidth(),
                target.localHeight()
        );
    }

    private boolean containsScreenPoint(BookSceneRenderer.ScreenQuad quad, float x, float y) {
        if (quad == null) {
            return false;
        }
        BookSceneRenderer.ScreenPoint point = new BookSceneRenderer.ScreenPoint(x, y);
        return pointInsideTriangle(point, quad.topLeft(), quad.topRight(), quad.bottomRight())
                || pointInsideTriangle(point, quad.topLeft(), quad.bottomRight(), quad.bottomLeft());
    }

    private boolean pointInsideTriangle(
            BookSceneRenderer.ScreenPoint point,
            BookSceneRenderer.ScreenPoint a,
            BookSceneRenderer.ScreenPoint b,
            BookSceneRenderer.ScreenPoint c
    ) {
        float denominator = ((b.y() - c.y()) * (a.x() - c.x())) + ((c.x() - b.x()) * (a.y() - c.y()));
        if (Math.abs(denominator) < 0.0001f) {
            return false;
        }
        float alpha = (((b.y() - c.y()) * (point.x() - c.x())) + ((c.x() - b.x()) * (point.y() - c.y()))) / denominator;
        float beta = (((c.y() - a.y()) * (point.x() - c.x())) + ((a.x() - c.x()) * (point.y() - c.y()))) / denominator;
        float gamma = 1.0f - alpha - beta;
        float epsilon = 0.001f;
        return alpha >= -epsilon && beta >= -epsilon && gamma >= -epsilon;
    }

    private void addResolvedInteractiveElements(
            List<ResolvedBookInteractionTarget> resolved,
            List<BookPageElement> elements,
            BookPageSide pageSide
    ) {
        for (BookPageElement element : elements) {
            if (element instanceof BookPageElement.InteractiveElement interactive) {
                resolved.add(ResolvedBookInteractionTarget.fromInteractiveElement(pageSide, interactive));
            }
        }
    }

    record Resolution(
            PageInteractiveNode hoveredTarget,
            List<PageInteractiveNode> targets,
            PageInteractionDebugState debugState
    ) {
        static Resolution empty() {
            return new Resolution(null, List.of(), null);
        }
    }

    record PageInteractionDebugState(PageDebugSide left, PageDebugSide right) {}

    record PageDebugSide(
            BookPageSide pageSide,
            BookSceneRenderer.PageSurfaceBounds bounds,
            BookSceneRenderer.PageLocalPoint localPoint,
            BookSceneRenderer.ScreenPoint mouseProjection,
            PageInteractiveNode containingTarget
    ) {}

}
