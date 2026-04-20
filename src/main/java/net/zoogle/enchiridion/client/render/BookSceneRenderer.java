package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.ui.BookLayout;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * v2 internal scene renderer backed by GeckoLib.
 * This stays inside Enchiridion; external books still provide content only.
 */
public final class BookSceneRenderer {
    private static final float GUI_Z = 180.0f;
    private static final float MODEL_SCALE = 30.0f;
    private static final float CLOSED_HOVER_SCALE = 1.035f;
    private static final float CLOSED_CENTER_X_OFFSET = 56.0f;
    private static final float MODEL_Y_OFFSET = 100.0f;
    private static final float MODEL_X_ROTATION = 180.0f;
    private static final float MODEL_Y_ROTATION = 0.0f;
    private static final float MAX_TILT_YAW_DEGREES = 8.0f;
    private static final float MAX_TILT_PITCH_DEGREES = 5.0f;
    private static final float CLOSED_PRESENTATION_YAW = -90.0f;
    private static final float TILT_SMOOTHING = 0.14f;
    private static final float HOVER_SMOOTHING = 0.2f;

    private static final GeoObjectRenderer<EnchiridionBookAnimatable> BOOK_RENDERER = new EnchiridionBookRenderer();
    private final EnchiridionBookAnimatable bookAnimatable = new EnchiridionBookAnimatable();
    private final BookPageTexturePipeline pageTexturePipeline = new BookPageTexturePipeline();
    private float currentTiltYaw;
    private float currentTiltPitch;
    private float currentClosedHoverScale = 1.0f;

    public void renderBook(
            GuiGraphics graphics,
            BookLayout layout,
            BookSpread spread,
            int spreadIndex,
            BookAnimState state,
            float animationProgress,
            int mouseX,
            int mouseY,
            float textAlpha,
            float partialTick,
            boolean closedHovered
    ) {
        BookPageTexturePipeline.TextureSet textures = pageTexturePipeline.textureSetFor(spread, spreadIndex, textAlpha);
        bookAnimatable.setAnimState(state);
        bookAnimatable.setTextureLocation(textures.baseTexture());
        bookAnimatable.setMagicTextTextureLocations(textures.magicLeftTexture(), textures.magicRightTexture());

        float closedness = closednessFor(state, animationProgress);
        float targetClosedHoverScale = closedHovered && closedness >= 0.999f ? CLOSED_HOVER_SCALE : 1.0f;
        currentClosedHoverScale = approach(currentClosedHoverScale, targetClosedHoverScale, HOVER_SMOOTHING);

        int centerX = Math.round(layout.bookX() + (layout.bookWidth() / 2.0f) + (CLOSED_CENTER_X_OFFSET * closedness));
        int centerY = layout.bookY() + (layout.bookHeight() / 2);
        float normalizedMouseX = normalizeMouseOffset(mouseX, layout.bookX(), layout.bookWidth());
        float normalizedMouseY = normalizeMouseOffset(mouseY, layout.bookY(), layout.bookHeight());
        float targetTiltYaw = -normalizedMouseX * MAX_TILT_YAW_DEGREES;
        float targetTiltPitch = -normalizedMouseY * MAX_TILT_PITCH_DEGREES;
        currentTiltYaw = approach(currentTiltYaw, targetTiltYaw, TILT_SMOOTHING);
        currentTiltPitch = approach(currentTiltPitch, targetTiltPitch, TILT_SMOOTHING);

        graphics.flush();
        RenderSystem.enableDepthTest();

        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY + MODEL_Y_OFFSET, GUI_Z);
        poseStack.scale(
                MODEL_SCALE * currentClosedHoverScale,
                MODEL_SCALE * currentClosedHoverScale,
                MODEL_SCALE * currentClosedHoverScale
        );

        float presentationYaw = MODEL_Y_ROTATION + (CLOSED_PRESENTATION_YAW * closedness);

// Keep the base orientation that makes the open book correct.
// Only bias the yaw while the book is closed / closing / opening.
        poseStack.mulPose(Axis.XP.rotationDegrees(MODEL_X_ROTATION));
        poseStack.mulPose(Axis.YP.rotationDegrees(presentationYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(currentTiltPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(currentTiltYaw));

        BOOK_RENDERER.render(
                poseStack,
                bookAnimatable,
                graphics.bufferSource(),
                null,
                null,
                LightTexture.FULL_BRIGHT,
                partialTick
        );

        poseStack.popPose();
        graphics.flush();
    }

    public boolean isClosedBookHovered(BookLayout layout, int mouseX, int mouseY) {
        int centerX = Math.round(layout.bookX() + (layout.bookWidth() / 2.0f) + CLOSED_CENTER_X_OFFSET);
        int halfWidth = Math.round(layout.bookWidth() * 0.22f);
        int halfHeight = Math.round(layout.bookHeight() * 0.45f);
        int centerY = layout.bookY() + (layout.bookHeight() / 2);
        return mouseX >= centerX - halfWidth
                && mouseX <= centerX + halfWidth
                && mouseY >= centerY - halfHeight
                && mouseY <= centerY + halfHeight;
    }

    private static float closednessFor(BookAnimState state, float animationProgress) {
        return switch (state) {
            case ARRIVING -> 1.0f;
            case CLOSED -> 1.0f;
            case OPENING -> 1.0f - animationProgress;
            case CLOSING -> animationProgress;
            default -> 0.0f;
        };
    }

    private static float normalizeMouseOffset(int mouse, int regionStart, int regionSize) {
        if (regionSize <= 0) {
            return 0.0f;
        }

        float regionCenter = regionStart + (regionSize / 2.0f);
        float halfSize = regionSize / 2.0f;
        return clamp((mouse - regionCenter) / halfSize, -1.0f, 1.0f);
    }

    private static float approach(float current, float target, float smoothing) {
        return current + ((target - current) * smoothing);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
