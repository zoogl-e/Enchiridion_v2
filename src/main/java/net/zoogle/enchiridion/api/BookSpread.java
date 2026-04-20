package net.zoogle.enchiridion.api;

import java.util.Objects;

public record BookSpread(BookPage left, BookPage right) {
    public BookSpread {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    public static BookSpread of(BookPage left, BookPage right) {
        return new BookSpread(left, right);
    }
}
