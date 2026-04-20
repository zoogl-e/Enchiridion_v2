package net.zoogle.enchiridion.client.input;

import net.neoforged.neoforge.client.event.InputEvent;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalBookRegistration;
import net.zoogle.enchiridion.EnchiridionClient;

public final class OpenKeyHandler {
    private OpenKeyHandler() {}

    public static void onKeyInput(InputEvent.Key event) {
        if (Keybinds.OPEN_UI.consumeClick()) {
            EnchiridionClient.get().openBook(LevelRpgJournalBookRegistration.LEVEL_RPG_JOURNAL_BOOK_ID);
        }
    }
}
