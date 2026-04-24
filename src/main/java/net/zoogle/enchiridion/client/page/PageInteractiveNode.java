package net.zoogle.enchiridion.client.page;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookRegionAction;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

public record PageInteractiveNode(
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
        VisualType visualType,
        BookSceneRenderer.ScreenRect screenRect,
        BookSceneRenderer.ScreenQuad screenQuad,
        HitTestMode hitTestMode,
        BookPageElement.InteractiveElement interactiveElement,
        BookInteractiveRegion region,
        BookTrackedRegion trackedRegion
) {
    public enum HitTestMode {
        PAGE_LOCAL,
        SCREEN_SPACE
    }

    public enum VisualType {
        INLINE_LINK,
        BUTTON,
        HOTSPOT
    }

    public boolean contains(float localX, float localY) {
        return enabled
                && localX >= this.localX
                && localX < this.localX + this.localWidth
                && localY >= this.localY
                && localY < this.localY + this.localHeight;
    }

    public boolean isPageNativeVisible() {
        return visualType != VisualType.HOTSPOT && label != null;
    }
}
