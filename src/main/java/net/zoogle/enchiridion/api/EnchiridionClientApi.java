package net.zoogle.enchiridion.api;

import net.minecraft.resources.ResourceLocation;

public interface EnchiridionClientApi {
    void registerBook(BookDefinition definition);
    void openBook(ResourceLocation id);
}
