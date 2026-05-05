package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.zoogle.enchiridion.client.audio.PostBindAmbientScheduler;
import net.zoogle.enchiridion.client.levelrpg.ReelFlowController;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

import java.util.function.Supplier;

/**
 * <b>Cinematic & Visual Effects Manager for Enchiridion Journal.</b>
 *
 * <p>Handles screen-shake, flash overlays, lightning bolts, and sound feedback
 * triggered by state changes in the book (e.g., archetype binding).
 *
 * <p>This manager is decoupled from the core book logic and focuses purely on
 * presentation/feedback timing and rendering.
 */
public final class BookCinematicManager {
    private int slotPulseTicks;
    private int bindFlashTicks;
    private int successFlashTicks;
    private int failureFlashTicks;
    private int cameraShakeTicks;
    private float cameraShakeStrength;
    private int cinematicTick;
    private int bindBoltTicks;
    private long bindBoltSeed;

    public void tick() {
        cinematicTick++;
        if (slotPulseTicks > 0) slotPulseTicks--;
        if (bindFlashTicks > 0) bindFlashTicks--;
        if (successFlashTicks > 0) successFlashTicks--;
        if (failureFlashTicks > 0) failureFlashTicks--;
        if (bindBoltTicks > 0) bindBoltTicks--;

        if (cameraShakeTicks > 0) {
            cameraShakeTicks--;
            cameraShakeStrength = Math.max(0.0f, cameraShakeStrength * 0.87f);
        } else {
            cameraShakeStrength = 0.0f;
        }
    }

    public void onReelPhaseChanged(ReelFlowController.FlowPhase previous, ReelFlowController.FlowPhase current, int screenWidth, int screenHeight) {
        if (current == ReelFlowController.FlowPhase.REQUESTING_BIND && previous == ReelFlowController.FlowPhase.SELECTING) {
            slotPulseTicks = 10;
            bindFlashTicks = Math.max(bindFlashTicks, 3);
            cameraShakeTicks = Math.max(cameraShakeTicks, 8);
            cameraShakeStrength = Math.max(cameraShakeStrength, 2.4f);
            playUiSound(SoundEvents.BOOK_PAGE_TURN, 0.85f, 0.78f);
            playUiSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.9f, 0.72f);
            return;
        }
        if (current == ReelFlowController.FlowPhase.AWAITING_SYNC) {
            bindFlashTicks = Math.max(bindFlashTicks, 6);
            cameraShakeTicks = Math.max(cameraShakeTicks, 14);
            cameraShakeStrength = Math.max(cameraShakeStrength, 1.8f);
            playUiSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.65f, 1.18f);
            return;
        }
        if (current == ReelFlowController.FlowPhase.COMPLETED) {
            successFlashTicks = Math.max(successFlashTicks, 7);
            cameraShakeTicks = Math.max(cameraShakeTicks, 10);
            cameraShakeStrength = Math.max(cameraShakeStrength, 2.8f);
            bindBoltTicks = 9;
            bindBoltSeed = ((long) cinematicTick << 32) ^ (long) (screenWidth * 131) ^ screenHeight;
            PostBindAmbientScheduler.scheduleRandomDelaySeconds(5, 10);
            playUiSound(SoundEvents.BEACON_ACTIVATE, 0.75f, 1.0f);
            playUiSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.12f);
            playUiSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55f, 1.0f);
            return;
        }
        if (current == ReelFlowController.FlowPhase.FAILED) {
            failureFlashTicks = Math.max(failureFlashTicks, 6);
            cameraShakeTicks = Math.max(cameraShakeTicks, 8);
            cameraShakeStrength = Math.max(cameraShakeStrength, 1.9f);
            playUiSound(SoundEvents.FIRE_EXTINGUISH, 0.85f, 0.78f);
            playUiSound(SoundEvents.ANVIL_LAND, 0.55f, 0.62f);
        }
    }

    public void applyCameraShake(GuiGraphics graphics, float partialTick) {
        if (cameraShakeTicks <= 0 || cameraShakeStrength <= 0.01f) {
            return;
        }
        float progress = Math.min(1.0f, (cameraShakeTicks + partialTick) / 14.0f);
        float envelope = progress * progress;
        float phase = ((float) (cinematicTick * 0.9f)) + (partialTick * 1.8f);
        float shakeX = (float) Math.sin(phase * 2.7f) * cameraShakeStrength * envelope;
        float shakeY = (float) Math.cos((phase * 3.4f) + 0.55f) * cameraShakeStrength * 0.7f * envelope;
        graphics.pose().translate(shakeX, shakeY, 0.0F);
    }

    public void renderPulse(GuiGraphics graphics, BookSceneRenderer.ScreenRect slot) {
        if (slotPulseTicks <= 0 || slot == null) {
            return;
        }
        float alpha = Math.min(1.0f, slotPulseTicks / 10.0f);
        int left = Math.round(slot.x());
        int top = Math.round(slot.y());
        int width = Math.max(1, Math.round(slot.width()));
        int height = Math.max(1, Math.round(slot.height()));
        int glow = scaleColorAlpha(0x88C8A2FF, alpha);
        int border = scaleColorAlpha(0xFFE8D7FF, alpha);
        graphics.fill(left - 4, top - 4, left + width + 4, top + height + 4, glow);
        graphics.renderOutline(left - 2, top - 2, width + 4, height + 4, border);
    }

    public void renderBolt(GuiGraphics graphics, int width, int height, BookSceneRenderer.ScreenRect slot) {
        if (bindBoltTicks <= 0) {
            return;
        }
        float targetX = slot == null ? (width * 0.5f) : slot.x() + (slot.width() * 0.5f);
        float targetY = slot == null ? (height * 0.57f) : slot.y() + (slot.height() * 0.5f);
        float startX = targetX + ((((bindBoltSeed & 31L) - 15L) * 2.2f));
        float startY = Math.max(10.0f, targetY - (160.0f + (((bindBoltSeed >> 5) & 15L) * 4.0f)));
        float life = Math.min(1.0f, bindBoltTicks / 9.0f);
        int coreColor = scaleColorAlpha(0xFFEFFFFF, life);
        int glowColor = scaleColorAlpha(0x99A8C8FF, life * 0.9f);
        float x = startX;
        float y = startY;
        int segments = 8;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float nx = lerp(startX, targetX, t);
            float ny = lerp(startY, targetY, t);
            float jitter = ((float) Math.sin((cinematicTick * 1.45f) + (i * 1.91f) + (bindBoltSeed & 7L))) * (8.0f * (1.0f - t));
            nx += jitter;
            drawBoltSegment(graphics, x, y, nx, ny, glowColor, 3);
            drawBoltSegment(graphics, x, y, nx, ny, coreColor, 1);
            x = nx;
            y = ny;
        }
        int burst = Math.max(2, Math.round(16.0f * life));
        int tx = Math.round(targetX);
        int ty = Math.round(targetY);
        graphics.fill(tx - burst, ty - burst, tx + burst, ty + burst, scaleColorAlpha(0x66C6DBFF, life));
    }

    public void renderFlash(GuiGraphics graphics, int width, int height) {
        int color = 0;
        if (successFlashTicks > 0) {
            float alpha = Math.min(1.0f, successFlashTicks / 7.0f);
            color = scaleColorAlpha(0x66CDB9FF, alpha);
        } else if (failureFlashTicks > 0) {
            float alpha = Math.min(1.0f, failureFlashTicks / 6.0f);
            color = scaleColorAlpha(0x55B84B4B, alpha);
        } else if (bindFlashTicks > 0) {
            float alpha = Math.min(1.0f, bindFlashTicks / 6.0f);
            color = scaleColorAlpha(0x44D7C3FF, alpha);
        }
        if (((color >>> 24) & 0xFF) > 0) {
            graphics.fill(0, 0, width, height, color);
        }
    }

    private void drawBoltSegment(GuiGraphics graphics, float x0, float y0, float x1, float y1, int color, int thickness) {
        int steps = Math.max(1, Math.round(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(lerp(x0, x1, t));
            int y = Math.round(lerp(y0, y1, t));
            graphics.fill(x - thickness, y - thickness, x + thickness + 1, y + thickness + 1, color);
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + ((to - from) * t);
    }

    private static int scaleColorAlpha(int argb, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(((argb >>> 24) & 0xFF) * alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static void playUiSound(SoundEvent soundEvent, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, volume, pitch));
    }
}
