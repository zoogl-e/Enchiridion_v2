package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.render.HitTestService;

import java.util.ArrayList;
import java.util.List;

final class BookInteractionResolver {
    Resolution resolve(
            BookScreenController controller,
            BookInteractionGeometry geometry,
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
                ? resolveTargets(controller, geometry, layout, displayedSpread, displayedSpreadIndex, projectionFocusOffset)
                : List.of();
        PageInteractiveNode hoveredTarget = controller.isInteractionStableReadable()
                ? resolveHoveredTarget(geometry, layout, controller, projectionFocusOffset, mouseX, mouseY, targets)
                : null;
        PageInteractionDebugState debugState = debugPageLocalInteraction && controller.isOpenReadable()
                ? buildPageInteractionDebugState(controller, geometry, layout, projectionFocusOffset, mouseX, mouseY, targets)
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
            BookInteractionGeometry geometry,
            BookLayout layout,
            BookSpread displayedSpread,
            int displayedSpreadIndex,
            float projectionFocusOffset
    ) {
        List<PageInteractiveNode> resolved = new ArrayList<>();
        for (ResolvedBookInteractionTarget target : resolvedTargets(
                displayedSpread,
                controller.currentInteractiveRegions(displayedSpreadIndex),
                controller.currentTrackedInteractiveRegions(displayedSpreadIndex)
        )) {
            resolved.add(buildNode(
                    controller,
                    geometry,
                    layout,
                    projectionFocusOffset,
                    target
            ));
        }
        return List.copyOf(resolved);
    }

    private PageInteractiveNode buildNode(
            BookScreenController controller,
            BookInteractionGeometry geometry,
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
                screenRectForTarget(geometry, layout, controller, projectionFocusOffset, target),
                screenQuadForTarget(geometry, layout, controller, projectionFocusOffset, target),
                target.hitTestMode(),
                target.interactiveElement(),
                target.region(),
                target.trackedRegion()
        );
    }

    private PageInteractiveNode resolveHoveredTarget(
            BookInteractionGeometry geometry,
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
                if (HitTestService.containsPoint(target.screenQuad(), mouseX, mouseY)) {
                    return target;
                }
                continue;
            }
            BookSceneRenderer.PageLocalPoint localPoint = target.pageSide() == BookPageSide.LEFT
                    ? (leftLocalPoint != null ? leftLocalPoint : (leftLocalPoint = pageLocalPoint(geometry, layout, controller, projectionFocusOffset, BookPageSide.LEFT, mouseX, mouseY)))
                    : (rightLocalPoint != null ? rightLocalPoint : (rightLocalPoint = pageLocalPoint(geometry, layout, controller, projectionFocusOffset, BookPageSide.RIGHT, mouseX, mouseY)));
            if (localPoint != null && target.contains(localPoint.localX(), localPoint.localY())) {
                return target;
            }
        }
        return null;
    }

    private PageInteractionDebugState buildPageInteractionDebugState(
            BookScreenController controller,
            BookInteractionGeometry geometry,
            BookLayout layout,
            float projectionFocusOffset,
            int mouseX,
            int mouseY,
            List<PageInteractiveNode> targets
    ) {
        return new PageInteractionDebugState(
                buildPageDebugSide(controller, geometry, layout, projectionFocusOffset, BookPageSide.LEFT, mouseX, mouseY, targets),
                buildPageDebugSide(controller, geometry, layout, projectionFocusOffset, BookPageSide.RIGHT, mouseX, mouseY, targets)
        );
    }

    private PageDebugSide buildPageDebugSide(
            BookScreenController controller,
            BookInteractionGeometry geometry,
            BookLayout layout,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int mouseX,
            int mouseY,
            List<PageInteractiveNode> targets
    ) {
        BookSceneRenderer.PageSurfaceBounds bounds = geometry.pageSurfaceBounds(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                projectionFocusOffset,
                pageSide
        );
        BookSceneRenderer.PageLocalPoint localPoint = pageLocalPoint(geometry, layout, controller, projectionFocusOffset, pageSide, mouseX, mouseY);
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
                : geometry.projectPagePoint(
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
            BookInteractionGeometry geometry,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int mouseX,
            int mouseY
    ) {
        return geometry.pageLocalPoint(
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
            BookInteractionGeometry geometry,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            ResolvedBookInteractionTarget target
    ) {
        if (target.trackedRegion() != null) {
            return geometry.projectTrackedRegionRect(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    projectionFocusOffset,
                    target.trackedRegion().anchor()
            );
        }
        return geometry.projectPageRect(
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
            BookInteractionGeometry geometry,
            BookLayout layout,
            BookScreenController controller,
            float projectionFocusOffset,
            ResolvedBookInteractionTarget target
    ) {
        if (target.trackedRegion() != null) {
            return geometry.projectTrackedRegionQuad(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    projectionFocusOffset,
                    target.trackedRegion().anchor()
            );
        }
        return geometry.projectPageQuad(
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
