package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record BookPageInteractiveText(
        BookPageSide pageSide,
        int x,
        int y,
        int width,
        int height,
        Component text,
        Component tooltip,
        BookRegionAction action
) {
    public BookPageInteractiveText {
        Objects.requireNonNull(pageSide, "pageSide");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(action, "action");
        width = Math.max(1, width);
        height = Math.max(1, height);
    }

    public static BookPageInteractiveText of(
            BookPageSide pageSide,
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            BookRegionAction action
    ) {
        return new BookPageInteractiveText(pageSide, x, y, width, height, text, tooltip, action);
    }
}
