package net.zoogle.enchiridion.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record BookContext(
        Minecraft minecraft,
        @Nullable LocalPlayer player,
        ResourceLocation bookId,
        BookContentSession contentSession
) {
    public static BookContext of(Minecraft minecraft, @Nullable LocalPlayer player, ResourceLocation bookId) {
        return new BookContext(minecraft, player, bookId, new BookContentSession());
    }
}
