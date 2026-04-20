package net.zoogle.enchiridion.client.input;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
    private Keybinds() {}

    public static final KeyMapping OPEN_UI =
            new KeyMapping("key.enchiridion.open", GLFW.GLFW_KEY_K, "key.categories.enchiridion");

    // Called from mod constructor via modBus.addListener(...)
    public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN_UI);
    }
}
