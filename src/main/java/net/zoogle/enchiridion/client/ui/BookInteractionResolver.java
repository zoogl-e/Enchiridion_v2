package net.zoogle.enchiridion.client.ui;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
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

    BookPageSide pageSideFor(BookSpread spread, BookPageElement.InteractiveElement element) {
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

    private List<PageInteractiveNode> resolveTargets(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            int displayedSpreadIndex,
            float projectionFocusOffset
    ) {
        List<PageInteractiveNode> resolved = new ArrayList<>();
        for (BookPageElement.InteractiveElement interactiveElement : pageInteractiveElements(displayedSpread)) {
            BookPageSide pageSide = pageSideFor(displayedSpread, interactiveElement);
            resolved.add(buildNode(
                    controller,
                    sceneRenderer,
                    layout,
                    projectionFocusOffset,
                    interactiveElement.stableId(),
                    pageSide,
                    interactiveElement.x(),
                    interactiveElement.y(),
                    interactiveElement.width(),
                    interactiveElement.height(),
                    labelFor(interactiveElement),
                    interactiveElement.tooltip(),
                    interactiveElement.action(),
                    interactiveElement.enabled(),
                    visualTypeFor(interactiveElement),
                    interactiveElement,
                    null
            ));
        }
        for (BookInteractiveRegion region : controller.definition().provider().interactiveRegions(controller.context(), displayedSpreadIndex)) {
            resolved.add(buildNode(
                    controller,
                    sceneRenderer,
                    layout,
                    projectionFocusOffset,
                    targetId("region", region.pageSide(), region.x(), region.y(), region.width(), region.height(), regionLabel(region)),
                    region.pageSide(),
                    region.x(),
                    region.y(),
                    region.width(),
                    region.height(),
                    regionLabelComponent(region),
                    region.tooltip(),
                    region.action(),
                    true,
                    visualTypeFor(region),
                    null,
                    region
            ));
        }
        return List.copyOf(resolved);
    }

    private PageInteractiveNode buildNode(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            float projectionFocusOffset,
            String stableId,
            BookPageSide pageSide,
            int localX,
            int localY,
            int localWidth,
            int localHeight,
            Component label,
            Component tooltip,
            net.zoogle.enchiridion.api.BookRegionAction action,
            boolean enabled,
            PageInteractiveNode.VisualType visualType,
            BookPageElement.InteractiveElement interactiveElement,
            BookInteractiveRegion region
    ) {
        return new PageInteractiveNode(
                stableId,
                pageSide,
                localX,
                localY,
                localWidth,
                localHeight,
                label,
                tooltip,
                action,
                enabled,
                visualType,
                sceneRenderer.projectPageRect(
                        layout,
                        controller.visualState(),
                        controller.animationProgress(),
                        controller.projectionProgress(),
                        projectionFocusOffset,
                        pageSide,
                        localX,
                        localY,
                        localWidth,
                        localHeight
                ),
                sceneRenderer.projectPageQuad(
                        layout,
                        controller.visualState(),
                        controller.animationProgress(),
                        controller.projectionProgress(),
                        projectionFocusOffset,
                        pageSide,
                        localX,
                        localY,
                        localWidth,
                        localHeight
                ),
                interactiveElement,
                region
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

    private List<BookPageElement.InteractiveElement> pageInteractiveElements(BookSpread spread) {
        if (spread == null) {
            return List.of();
        }
        List<BookPageElement.InteractiveElement> elements = new ArrayList<>();
        for (BookPageElement element : spread.left().elements()) {
            if (element instanceof BookPageElement.InteractiveElement interactive) {
                elements.add(interactive);
            }
        }
        for (BookPageElement element : spread.right().elements()) {
            if (element instanceof BookPageElement.InteractiveElement interactive) {
                elements.add(interactive);
            }
        }
        return elements;
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

    private static PageInteractiveNode.VisualType visualTypeFor(BookPageElement.InteractiveElement interactiveElement) {
        return switch (interactiveElement.visualStyle()) {
            case MANUSCRIPT_LINK -> PageInteractiveNode.VisualType.INLINE_LINK;
            case BUTTON -> PageInteractiveNode.VisualType.BUTTON;
        };
    }

    private static PageInteractiveNode.VisualType visualTypeFor(BookInteractiveRegion region) {
        if (region.visibleLabel() != null) {
            return PageInteractiveNode.VisualType.BUTTON;
        }
        if (region.interactiveText() != null) {
            return PageInteractiveNode.VisualType.INLINE_LINK;
        }
        return PageInteractiveNode.VisualType.HOTSPOT;
    }

    private static Component labelFor(BookPageElement.InteractiveElement interactiveElement) {
        return switch (interactiveElement) {
            case BookPageElement.InteractiveTextElement text -> text.text();
            case BookPageElement.ButtonElement button -> button.label();
        };
    }

    private static String regionLabel(BookInteractiveRegion region) {
        if (region.visibleLabel() != null) {
            return region.visibleLabel().getString();
        }
        if (region.interactiveText() != null) {
            return region.interactiveText().getString();
        }
        return "";
    }

    private static Component regionLabelComponent(BookInteractiveRegion region) {
        return region.visibleLabel() != null ? region.visibleLabel() : region.interactiveText();
    }

    private static String targetId(String prefix, BookPageSide side, int x, int y, int width, int height, String label) {
        return prefix + ":" + side + ":" + x + ":" + y + ":" + width + ":" + height + ":" + label;
    }
}
