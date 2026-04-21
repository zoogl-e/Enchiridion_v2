package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<ResolvedRegion> visibleActionRegions = resolveVisibleActionRegions(
                controller,
                sceneRenderer,
                layout,
                displayedSpreadIndex,
                projectionFocusOffset
        );
        ResolvedPageInteractiveText hoveredPageText = controller.isOpenReadable()
                ? resolveHoveredPageInteractiveText(controller, sceneRenderer, layout, displayedSpread, projectionFocusOffset, mouseX, mouseY)
                : null;
        ResolvedRegion hoveredPageRegion = controller.isOpenReadable()
                ? resolveHoveredPageRegion(controller, sceneRenderer, layout, displayedSpreadIndex, projectionFocusOffset, visibleActionRegions, mouseX, mouseY)
                : null;
        PageInteractionDebugState debugState = debugPageLocalInteraction && controller.isOpenReadable()
                ? buildPageInteractionDebugState(controller, sceneRenderer, layout, displayedSpread, projectionFocusOffset, mouseX, mouseY)
                : null;
        return new Resolution(hoveredPageText, hoveredPageRegion, visibleActionRegions, debugState);
    }

    BookPageElement.InteractiveTextElement hoveredInteractiveElement(Resolution resolution, BookSpread spread, BookPageSide pageSide) {
        if (resolution.hoveredPageText() == null) {
            return null;
        }
        return pageSideFor(spread, resolution.hoveredPageText().text()) == pageSide ? resolution.hoveredPageText().text() : null;
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

    private ResolvedRegion resolveHoveredPageRegion(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            int displayedSpreadIndex,
            float projectionFocusOffset,
            List<ResolvedRegion> visibleActionRegions,
            int mouseX,
            int mouseY
    ) {
        for (ResolvedRegion region : visibleActionRegions) {
            if (mouseX >= region.screenRect().x()
                    && mouseX < region.screenRect().x() + region.screenRect().width()
                    && mouseY >= region.screenRect().y()
                    && mouseY < region.screenRect().y() + region.screenRect().height()) {
                return region;
            }
        }

        for (BookInteractiveRegion region : controller.definition().provider().interactiveRegions(controller.context(), displayedSpreadIndex)) {
            if (region.visibleLabel() != null || region.interactiveText() != null) {
                continue;
            }
            BookSceneRenderer.PageLocalPoint localPoint = sceneRenderer.pageLocalPoint(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    projectionFocusOffset,
                    region.pageSide(),
                    mouseX,
                    mouseY
            );
            if (localPoint == null) {
                continue;
            }
            if (localPoint.localX() >= region.x()
                    && localPoint.localX() < region.x() + region.width()
                    && localPoint.localY() >= region.y()
                    && localPoint.localY() < region.y() + region.height()) {
                BookSceneRenderer.ScreenRect rect = sceneRenderer.projectPageRect(
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
                );
                return new ResolvedRegion(region, rect);
            }
        }
        return null;
    }

    private ResolvedPageInteractiveText resolveHoveredPageInteractiveText(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            BookSpread displayedSpread,
            float projectionFocusOffset,
            int mouseX,
            int mouseY
    ) {
        for (BookPageElement.InteractiveTextElement text : pageInteractiveElements(displayedSpread)) {
            BookPageSide pageSide = pageSideFor(displayedSpread, text);
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
            if (localPoint == null) {
                continue;
            }
            if (localPoint.localX() >= text.x()
                    && localPoint.localX() < text.x() + text.width()
                    && localPoint.localY() >= text.y()
                    && localPoint.localY() < text.y() + text.height()) {
                BookSceneRenderer.ScreenRect rect = sceneRenderer.projectPageRect(
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
                );
                return new ResolvedPageInteractiveText(text, rect);
            }
        }
        return null;
    }

    private List<ResolvedRegion> resolveVisibleActionRegions(
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            BookLayout layout,
            int displayedSpreadIndex,
            float projectionFocusOffset
    ) {
        List<BookInteractiveRegion> labeledRegions = controller.definition().provider().interactiveRegions(controller.context(), displayedSpreadIndex).stream()
                .filter(region -> region.visibleLabel() != null)
                .collect(Collectors.toList());
        if (labeledRegions.isEmpty()) {
            return List.of();
        }

        List<ResolvedRegion> resolved = new ArrayList<>(labeledRegions.size());
        for (BookInteractiveRegion region : labeledRegions) {
            BookSceneRenderer.ScreenRect rect = sceneRenderer.projectPageRect(
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
            );
            resolved.add(new ResolvedRegion(region, rect));
        }
        return resolved;
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
            ResolvedPageInteractiveText hoveredPageText,
            ResolvedRegion hoveredPageRegion,
            List<ResolvedRegion> visibleActionRegions,
            PageInteractionDebugState debugState
    ) {
        static Resolution empty() {
            return new Resolution(null, null, List.of(), null);
        }
    }

    record ResolvedRegion(BookInteractiveRegion region, BookSceneRenderer.ScreenRect screenRect) {}

    record ResolvedPageInteractiveText(BookPageElement.InteractiveTextElement text, BookSceneRenderer.ScreenRect screenRect) {}

    record PageInteractionDebugState(PageDebugSide left, PageDebugSide right) {}

    record PageDebugSide(
            BookPageSide pageSide,
            BookSceneRenderer.PageSurfaceBounds bounds,
            BookSceneRenderer.PageLocalPoint localPoint,
            BookSceneRenderer.ScreenPoint mouseProjection,
            BookPageElement.InteractiveTextElement containingText
    ) {}
}
