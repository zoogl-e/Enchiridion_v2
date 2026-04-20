package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record BookTextBlock(Kind kind, Component text) {
    public BookTextBlock {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
    }

    public static BookTextBlock title(Component text) {
        return new BookTextBlock(Kind.TITLE, text);
    }

    public static BookTextBlock subtitle(Component text) {
        return new BookTextBlock(Kind.SUBTITLE, text);
    }

    public static BookTextBlock body(Component text) {
        return new BookTextBlock(Kind.BODY, text);
    }

    public enum Kind {
        TITLE,
        SUBTITLE,
        BODY
    }
}
