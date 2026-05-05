package net.zoogle.enchiridion.client.levelrpg.archetype;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ArchetypeBindFeedback(
        long version,
        boolean success,
        String message,
        @Nullable ResourceLocation archetypeId
) {}
