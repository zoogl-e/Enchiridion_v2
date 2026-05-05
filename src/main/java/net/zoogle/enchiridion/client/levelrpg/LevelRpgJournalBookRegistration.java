package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.zoogle.enchiridion.EnchiridionClient;
import net.zoogle.enchiridion.client.levelrpg.journal.LevelRpgJournalBookProvider;

public final class LevelRpgJournalBookRegistration {
    public static final ResourceLocation LEVEL_RPG_JOURNAL_BOOK_ID = ResourceLocation.fromNamespaceAndPath("enchiridion", "level_rpg_journal");

    private LevelRpgJournalBookRegistration() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> EnchiridionClient.get().registerBook(
                LEVEL_RPG_JOURNAL_BOOK_ID,
                Component.literal("Level RPG Journal"),
                new LevelRpgJournalBookProvider()
        ));
    }
}
