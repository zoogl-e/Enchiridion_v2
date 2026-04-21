package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

public record BookPage(List<BookTextBlock> blocks, List<BookPageElement> elements) {
    public BookPage {
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(elements, "elements");
        blocks = List.copyOf(blocks);
        elements = List.copyOf(elements);
    }

    public BookPage(List<BookTextBlock> blocks) {
        this(blocks, List.of());
    }

    public static BookPage of(BookTextBlock... blocks) {
        return new BookPage(List.of(blocks), List.of());
    }

    public static BookPage of(List<BookTextBlock> blocks, List<BookPageElement> elements) {
        return new BookPage(blocks, elements);
    }

    public static BookPage text(Component title, Component body) {
        return new BookPage(List.of(
                BookTextBlock.title(title),
                BookTextBlock.body(body)
        ), List.of());
    }

    public static BookPage empty() {
        return new BookPage(List.of(), List.of());
    }
}
