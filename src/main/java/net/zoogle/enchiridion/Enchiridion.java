package net.zoogle.enchiridion;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.zoogle.enchiridion.client.demo.DemoBookRegistration;
import net.zoogle.enchiridion.client.input.Keybinds;
import net.zoogle.enchiridion.client.input.OpenKeyHandler;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalBookRegistration;

@Mod(Enchiridion.MODID)
public final class Enchiridion {
    public static final String MODID = "enchiridion";

    public Enchiridion() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();
            modBus.addListener((RegisterKeyMappingsEvent e) -> Keybinds.onRegisterKeys(e));
            modBus.addListener(this::onClientSetup);
            modBus.addListener(DemoBookRegistration::onClientSetup);
            modBus.addListener(LevelRpgJournalBookRegistration::onClientSetup);
            NeoForge.EVENT_BUS.addListener(OpenKeyHandler::onKeyInput);
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        EnchiridionClient.ensure();
    }
}
