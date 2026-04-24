package net.zoogle.enchiridion.client.ui;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookRegionAction;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

record ResolvedBookInteractionTarget(
        String stableId,
        BookPageSide pageSide,
        int localX,
        int localY,
        int localWidth,
        int localHeight,
        Component label,
        Component tooltip,
        BookRegionAction action,
        boolean enabled,
        PageInteractiveNode.VisualType visualType,
        PageInteractiveNode.HitTestMode hitTestMode,
        BookPageElement.InteractiveElement interactiveElement,
        BookInteractiveRegion region,
        BookTrackedRegion trackedRegion
) {
    static ResolvedBookInteractionTarget fromInteractiveElement(
            BookPageSide pageSide,
            BookPageElement.InteractiveElement interactiveElement
    ) {
        return switch (interactiveElement) {
            case BookPageElement.InteractiveTextElement text -> {
                PageCanvasRenderer.RenderedTextGeometry geometry = PageCanvasRenderer.inlineInteractionGeometryFor(text);
                yield new ResolvedBookInteractionTarget(
                        text.stableId(),
                        pageSide,
                        geometry.drawX(),
                        geometry.drawY(),
                        geometry.width(),
                        geometry.height(),
                        text.text(),
                        text.tooltip(),
                        text.action(),
                        text.enabled(),
                        visualTypeFor(interactiveElement),
                        PageInteractiveNode.HitTestMode.PAGE_LOCAL,
                        interactiveElement,
                        null,
                        null
                );
            }
            case BookPageElement.ButtonElement button -> new ResolvedBookInteractionTarget(
                    button.stableId(),
                    pageSide,
                    button.x(),
                    button.y(),
                    button.width(),
                    button.height(),
                    button.label(),
                    button.tooltip(),
                    button.action(),
                    button.enabled(),
                    visualTypeFor(interactiveElement),
                    PageInteractiveNode.HitTestMode.PAGE_LOCAL,
                    interactiveElement,
                    null,
                    null
            );
        };
    }

    static ResolvedBookInteractionTarget fromRegion(BookInteractiveRegion region) {
        return new ResolvedBookInteractionTarget(
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
                PageInteractiveNode.HitTestMode.PAGE_LOCAL,
                null,
                region,
                null
        );
    }

    static ResolvedBookInteractionTarget fromTrackedRegion(BookTrackedRegion trackedRegion) {
        return new ResolvedBookInteractionTarget(
                "tracked:" + trackedRegion.anchor().name().toLowerCase(),
                BookPageSide.LEFT,
                0,
                0,
                1,
                1,
                trackedRegion.visibleLabel(),
                trackedRegion.tooltip(),
                trackedRegion.action(),
                true,
                PageInteractiveNode.VisualType.HOTSPOT,
                PageInteractiveNode.HitTestMode.SCREEN_SPACE,
                null,
                null,
                trackedRegion
        );
    }

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
