package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BookDefinition(
        ResourceLocation id,
        Component title,
        BookPageProvider provider
) {
    public BookDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(provider, "provider");
    }

    public static BookDefinition of(ResourceLocation id, Component title, BookPageProvider provider) {
        return new BookDefinition(id, title, provider);
    }
}
