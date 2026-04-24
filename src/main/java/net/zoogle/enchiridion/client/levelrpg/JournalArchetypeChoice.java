package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record JournalArchetypeChoice(
        String focusId,
        String title,
        String description,
        String affectedSpreadText,
        String artKey
) {
    public @Nullable ResourceLocation archetypeId() {
        try {
            return ResourceLocation.parse(focusId);
        } catch (Exception exception) {
            return null;
        }
    }

    public String startingDisciplines() {
        return affectedSpreadText;
    }
}
