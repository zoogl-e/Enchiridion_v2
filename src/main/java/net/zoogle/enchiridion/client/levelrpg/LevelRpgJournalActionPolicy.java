package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgJournalInteractionBridge;

public final class LevelRpgJournalActionPolicy {
    public boolean bindArchetype(BookContext context, String focusId, int mouseButton) {
        return mouseButton == 0 && LevelRpgJournalInteractionBridge.beginArchetypeBinding(context, focusId);
    }

    public boolean handleFrontCoverCardInteraction(BookContext context, BookFrontCoverCardState cardState, String focusId) {
        if (cardState.boundArchetypeId() != null) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui != null) {
                minecraft.gui.setOverlayMessage(Component.literal("Archetype is already inscribed into this journal."), false);
            }
            return true;
        }
        return LevelRpgJournalInteractionBridge.beginArchetypeSelection(context);
    }
}
