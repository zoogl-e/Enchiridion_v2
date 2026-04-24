package net.zoogle.enchiridion.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record BookFrontCoverCardState(
        boolean visible,
        boolean clickable,
        @Nullable ResourceLocation hoveredArchetypeId,
        @Nullable ResourceLocation selectedArchetypeId,
        @Nullable ResourceLocation boundArchetypeId
) {
    public static BookFrontCoverCardState hidden() {
        return new BookFrontCoverCardState(false, false, null, null, null);
    }

    public static BookFrontCoverCardState visible(@Nullable ResourceLocation boundArchetypeId) {
        return new BookFrontCoverCardState(true, false, null, null, boundArchetypeId);
    }

    public @Nullable ResourceLocation displayedArchetypeId() {
        if (boundArchetypeId != null) {
            return boundArchetypeId;
        }
        if (selectedArchetypeId != null) {
            return selectedArchetypeId;
        }
        return hoveredArchetypeId;
    }
}
