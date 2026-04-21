package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record BookInteractiveRegion(
        BookPageSide pageSide,
        int x,
        int y,
        int width,
        int height,
        Component tooltip,
        Component visibleLabel,
        Component interactiveText,
        BookRegionAction action
) {
    public BookInteractiveRegion {
        Objects.requireNonNull(pageSide, "pageSide");
        Objects.requireNonNull(action, "action");
        width = Math.max(1, width);
        height = Math.max(1, height);
    }

    public static BookInteractiveRegion of(
            BookPageSide pageSide,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            BookRegionAction action
    ) {
        return new BookInteractiveRegion(pageSide, x, y, width, height, tooltip, null, null, action);
    }

    public static BookInteractiveRegion of(
            BookPageSide pageSide,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            Component visibleLabel,
            BookRegionAction action
    ) {
        return new BookInteractiveRegion(pageSide, x, y, width, height, tooltip, visibleLabel, null, action);
    }

    public static BookInteractiveRegion of(
            BookPageSide pageSide,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            Component visibleLabel,
            Component interactiveText,
            BookRegionAction action
    ) {
        return new BookInteractiveRegion(pageSide, x, y, width, height, tooltip, visibleLabel, interactiveText, action);
    }
}
