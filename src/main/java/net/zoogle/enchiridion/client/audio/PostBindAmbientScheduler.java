package net.zoogle.enchiridion.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class PostBindAmbientScheduler {
    private static int pendingTicks = -1;

    private PostBindAmbientScheduler() {}

    public static void scheduleRandomDelaySeconds(int minSeconds, int maxSeconds) {
        int minTicks = Math.max(1, minSeconds) * 20;
        int maxTicks = Math.max(minTicks, maxSeconds * 20);
        pendingTicks = ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (pendingTicks < 0) {
            return;
        }
        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }
        pendingTicks = -1;
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        float pitch = 0.92f + (level.random.nextFloat() * 0.24f);
        level.playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.AMBIENT_CAVE.value(),
                SoundSource.AMBIENT,
                1.0f,
                pitch,
                false
        );
    }
}
