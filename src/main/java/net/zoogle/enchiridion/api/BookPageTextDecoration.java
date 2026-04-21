package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record BookPageTextDecoration(
        BookPageSide pageSide,
        int x,
        int y,
        BookTextBlock.Kind kind,
        Component text
) {
    public BookPageTextDecoration {
        Objects.requireNonNull(pageSide, "pageSide");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
    }

    public static BookPageTextDecoration of(BookPageSide pageSide, int x, int y, BookTextBlock.Kind kind, Component text) {
        return new BookPageTextDecoration(pageSide, x, y, kind, text);
    }
}
