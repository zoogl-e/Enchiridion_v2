package net.zoogle.enchiridion.client.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.zoogle.enchiridion.client.ui.BookDebugSettings;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.IntBuffer;

public final class EnchiridionRenderDebugProbe {
    private static final Path DEBUG_LOG_PATH = Path.of("C:/Users/Zayne-PC/Desktop/Minecraft Modding/_IDEA (2025)/Enchiridion_v2/debug-e88f66.log");
    private static long lastGuiLogMs;
    private static long lastGuiPostLogMs;
    private static long lastLevelLogMs;
    private static long lastSanitizeLogMs;

    private EnchiridionRenderDebugProbe() {
    }

    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long throttleMs = player.tickCount < 140 ? 80L : 450L;
        if ((now - lastGuiLogMs) < throttleMs) {
            return;
        }
        String screenName = minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName();
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        float[] shader = RenderSystem.getShaderColor();
        String scissorBox = readScissorBox();
        String viewport = readViewport();
        int winW = minecraft.getWindow().getWidth();
        int winH = minecraft.getWindow().getHeight();
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        int targetW = minecraft.getMainRenderTarget().width;
        int targetH = minecraft.getMainRenderTarget().height;
        // #region agent log
        debugLog(
                "run1",
                "H6",
                "EnchiridionRenderDebugProbe.onRenderGuiPre",
                "Global gui render state snapshot",
                "\"screen\":\"" + esc(screenName) + "\",\"playerTick\":" + player.tickCount + ",\"debugAny\":" + BookDebugSettings.anyDebugEnabled() + ",\"debugInteraction\":" + BookDebugSettings.interactionBoundsDebug() + ",\"playerYaw\":" + player.getYRot() + ",\"playerPitch\":" + player.getXRot() + ",\"scissorEnabled\":" + scissorEnabled + ",\"blendEnabled\":" + blendEnabled + ",\"depthEnabled\":" + depthEnabled + ",\"shaderR\":" + shader[0] + ",\"shaderG\":" + shader[1] + ",\"shaderB\":" + shader[2] + ",\"shaderA\":" + shader[3] + ",\"scissorBox\":\"" + esc(scissorBox) + "\",\"viewport\":\"" + esc(viewport) + "\",\"win\":\"" + winW + "x" + winH + "\",\"gui\":\"" + guiW + "x" + guiH + "\",\"target\":\"" + targetW + "x" + targetH + "\""
        );
        // #endregion
        lastGuiLogMs = now;
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long throttleMs = player.tickCount < 140 ? 80L : 450L;
        if ((now - lastGuiPostLogMs) < throttleMs) {
            return;
        }
        String screenName = minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName();
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        float[] shader = RenderSystem.getShaderColor();
        String scissorBox = readScissorBox();
        String viewport = readViewport();
        int winW = minecraft.getWindow().getWidth();
        int winH = minecraft.getWindow().getHeight();
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        int targetW = minecraft.getMainRenderTarget().width;
        int targetH = minecraft.getMainRenderTarget().height;
        // #region agent log
        debugLog(
                "run1",
                "H21",
                "EnchiridionRenderDebugProbe.onRenderGuiPost",
                "Global gui POST render state snapshot",
                "\"screen\":\"" + esc(screenName) + "\",\"playerTick\":" + player.tickCount + ",\"debugAny\":" + BookDebugSettings.anyDebugEnabled() + ",\"debugInteraction\":" + BookDebugSettings.interactionBoundsDebug() + ",\"playerYaw\":" + player.getYRot() + ",\"playerPitch\":" + player.getXRot() + ",\"scissorEnabled\":" + scissorEnabled + ",\"blendEnabled\":" + blendEnabled + ",\"depthEnabled\":" + depthEnabled + ",\"shaderR\":" + shader[0] + ",\"shaderG\":" + shader[1] + ",\"shaderB\":" + shader[2] + ",\"shaderA\":" + shader[3] + ",\"scissorBox\":\"" + esc(scissorBox) + "\",\"viewport\":\"" + esc(viewport) + "\",\"win\":\"" + winW + "x" + winH + "\",\"gui\":\"" + guiW + "x" + guiH + "\",\"target\":\"" + targetW + "x" + targetH + "\""
        );
        // #endregion
        lastGuiPostLogMs = now;
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        observeEarlyRenderState(event, minecraft, player.tickCount);
        long now = System.currentTimeMillis();
        long throttleMs = player.tickCount < 80 ? 0L : (player.tickCount < 140 ? 80L : 450L);
        if ((now - lastLevelLogMs) < throttleMs) {
            return;
        }
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean[] colorMask = readColorMask();
        float[] shader = RenderSystem.getShaderColor();
        String scissorBox = readScissorBox();
        String viewport = readViewport();
        int winW = minecraft.getWindow().getWidth();
        int winH = minecraft.getWindow().getHeight();
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        int targetW = minecraft.getMainRenderTarget().width;
        int targetH = minecraft.getMainRenderTarget().height;
        // #region agent log
        debugLog(
                "run1",
                "H8",
                "EnchiridionRenderDebugProbe.onRenderLevelStage",
                "Global level render stage snapshot",
                "\"stage\":\"" + esc(String.valueOf(event.getStage())) + "\",\"debugAny\":" + BookDebugSettings.anyDebugEnabled() + ",\"screenNull\":" + (minecraft.screen == null) + ",\"playerTick\":" + player.tickCount + ",\"scissorEnabled\":" + scissorEnabled + ",\"blendEnabled\":" + blendEnabled + ",\"depthEnabled\":" + depthEnabled + ",\"cullEnabled\":" + cullEnabled + ",\"depthMask\":" + depthMask + ",\"colorMask\":\"" + colorMask[0] + "," + colorMask[1] + "," + colorMask[2] + "," + colorMask[3] + "\",\"shaderR\":" + shader[0] + ",\"shaderG\":" + shader[1] + ",\"shaderB\":" + shader[2] + ",\"shaderA\":" + shader[3] + ",\"scissorBox\":\"" + esc(scissorBox) + "\",\"viewport\":\"" + esc(viewport) + "\",\"win\":\"" + winW + "x" + winH + "\",\"gui\":\"" + guiW + "x" + guiH + "\",\"target\":\"" + targetW + "x" + targetH + "\""
        );
        // #endregion
        lastLevelLogMs = now;
    }

    private static void observeEarlyRenderState(RenderLevelStageEvent event, Minecraft minecraft, int playerTick) {
        String stageId = String.valueOf(event.getStage());
        if (!"minecraft:after_sky".equals(stageId) || minecraft.screen != null || playerTick > 240) {
            return;
        }
        int targetW = minecraft.getMainRenderTarget().width;
        int targetH = minecraft.getMainRenderTarget().height;
        if (targetW <= 0 || targetH <= 0) {
            return;
        }
        String beforeScissor = readScissorBox();
        String beforeViewport = readViewport();
        long now = System.currentTimeMillis();
        if ((now - lastSanitizeLogMs) < 80L) {
            return;
        }
        String afterScissor = readScissorBox();
        String afterViewport = readViewport();
        // #region agent log
        debugLog(
                "run1",
                "H24",
                "EnchiridionRenderDebugProbe.observeEarlyRenderState",
                "Observed early world viewport/scissor state",
                "\"stage\":\"" + esc(stageId) + "\",\"playerTick\":" + playerTick + ",\"beforeScissor\":\"" + esc(beforeScissor) + "\",\"afterScissor\":\"" + esc(afterScissor) + "\",\"beforeViewport\":\"" + esc(beforeViewport) + "\",\"afterViewport\":\"" + esc(afterViewport) + "\",\"target\":\"" + targetW + "x" + targetH + "\""
        );
        // #endregion
        lastSanitizeLogMs = now;
    }

    private static void debugLog(String runId, String hypothesisId, String location, String message, String dataJson) {
        String payload = "{\"sessionId\":\"e88f66\",\"runId\":\"" + esc(runId) + "\",\"hypothesisId\":\"" + esc(hypothesisId)
                + "\",\"location\":\"" + esc(location) + "\",\"message\":\"" + esc(message) + "\",\"data\":{" + dataJson
                + "},\"timestamp\":" + System.currentTimeMillis() + "}\n";
        try {
            Files.writeString(DEBUG_LOG_PATH, payload, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static String esc(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readScissorBox() {
        IntBuffer box = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
        return box.get(0) + "," + box.get(1) + "," + box.get(2) + "," + box.get(3);
    }

    private static String readViewport() {
        IntBuffer view = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, view);
        return view.get(0) + "," + view.get(1) + "," + view.get(2) + "," + view.get(3);
    }

    private static boolean[] readColorMask() {
        IntBuffer mask = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, mask);
        return new boolean[]{
                mask.get(0) != 0,
                mask.get(1) != 0,
                mask.get(2) != 0,
                mask.get(3) != 0
        };
    }
}
