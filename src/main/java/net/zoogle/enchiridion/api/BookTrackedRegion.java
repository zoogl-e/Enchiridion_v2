package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record BookTrackedRegion(
        Anchor anchor,
        Component tooltip,
        Component visibleLabel,
        BookRegionAction action
) {
    public BookTrackedRegion {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(action, "action");
    }

    public static BookTrackedRegion of(Anchor anchor, Component tooltip, BookRegionAction action) {
        return new BookTrackedRegion(anchor, tooltip, null, action);
    }

    public enum Anchor {
        FRONT_COVER_CARD,
        FRONT_COVER_TITLE
    }
}
