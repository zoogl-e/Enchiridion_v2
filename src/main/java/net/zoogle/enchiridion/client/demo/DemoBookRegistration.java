package net.zoogle.enchiridion.client.demo;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.zoogle.enchiridion.EnchiridionClient;

public final class DemoBookRegistration {
    public static final ResourceLocation DEMO_BOOK_ID = ResourceLocation.fromNamespaceAndPath("enchiridion", "demo_book");

    private DemoBookRegistration() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> EnchiridionClient.get().registerBook(
                DEMO_BOOK_ID,
                Component.literal("Enchiridion v2 Demo"),
                new DemoBookProvider()
        ));
    }
}
