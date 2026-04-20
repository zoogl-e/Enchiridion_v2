package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

public record BookPage(List<BookTextBlock> blocks) {
    public BookPage {
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }

    public static BookPage of(BookTextBlock... blocks) {
        return new BookPage(List.of(blocks));
    }

    public static BookPage text(Component title, Component body) {
        return new BookPage(List.of(
                BookTextBlock.title(title),
                BookTextBlock.body(body)
        ));
    }

    public static BookPage empty() {
        return new BookPage(List.of());
    }
}
