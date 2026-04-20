package net.zoogle.enchiridion.client.registry;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BookRegistry {
    private static final Map<ResourceLocation, BookDefinition> BOOKS = new LinkedHashMap<>();

    private BookRegistry() {}

    public static void register(BookDefinition definition) {
        BOOKS.put(definition.id(), definition);
    }

    public static BookDefinition get(ResourceLocation id) {
        return BOOKS.get(id);
    }

    public static Collection<BookDefinition> all() {
        return Collections.unmodifiableCollection(BOOKS.values());
    }

    public static void clear() {
        BOOKS.clear();
    }
}
