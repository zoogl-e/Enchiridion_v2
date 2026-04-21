package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record BookProjectionView(
        String focusId,
        Component title,
        Component dominantValue,
        Component secondaryValue,
        Component description,
        Component progressLabel,
        float progress,
        Component statusLabel,
        boolean emphasizedStatus,
        @Nullable Component primaryActionLabel,
        @Nullable BookRegionAction primaryAction
) {
    public BookProjectionView {
        Objects.requireNonNull(focusId, "focusId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(dominantValue, "dominantValue");
        Objects.requireNonNull(secondaryValue, "secondaryValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(progressLabel, "progressLabel");
        Objects.requireNonNull(statusLabel, "statusLabel");
        progress = Math.clamp(progress, 0.0f, 1.0f);
    }

    public boolean hasPrimaryAction() {
        return primaryActionLabel != null && primaryAction != null;
    }
}
