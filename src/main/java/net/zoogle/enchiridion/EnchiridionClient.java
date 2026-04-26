package net.zoogle.enchiridion;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.EnchiridionClientApi;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalBookRegistration;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgProfileSyncHook;
import net.zoogle.enchiridion.client.registry.BookRegistry;
import net.zoogle.enchiridion.client.ui.BookScreen;

/**
 * Small client-facing singleton for registering and opening books.
 * Enchiridion v2 keeps rendering internal and only exposes content registration.
 */
public final class EnchiridionClient implements EnchiridionClientApi {
    private static volatile EnchiridionClient INSTANCE;

    public static EnchiridionClient get() {
        if (INSTANCE == null) {
            ensure();
        }
        return INSTANCE;
    }

    public static void ensure() {
        if (INSTANCE == null) {
            INSTANCE = new EnchiridionClient();
        }
    }

    private EnchiridionClient() {}

    @Override
    public void registerBook(BookDefinition definition) {
        BookRegistry.register(definition);
    }

    public void registerBook(ResourceLocation id, Component title, BookPageProvider provider) {
        registerBook(BookDefinition.of(id, title, provider));
    }

    @Override
    public void openBook(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        BookDefinition definition = BookRegistry.get(id);
        if (definition == null) {
            return;
        }

        if (id.equals(LevelRpgJournalBookRegistration.LEVEL_RPG_JOURNAL_BOOK_ID)) {
            LevelRpgProfileSyncHook.requestServerSnapshotIfAvailable();
        }

        BookContext context = BookContext.of(minecraft, minecraft.player, id);
        minecraft.setScreen(new BookScreen(definition, context));
    }
}
