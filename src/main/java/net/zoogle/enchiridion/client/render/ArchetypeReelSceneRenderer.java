package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.client.levelrpg.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.JournalArchetypeChoice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArchetypeReelSceneRenderer {
    private static final ResourceLocation CARD_TEXTURE = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
    private static final float CARD_WIDTH = 59.0f / 16.0f;
    private static final float CARD_HEIGHT = 80.0f / 16.0f;
    private static final float CARD_THICKNESS = 0.04f;
    private static final float UV_MIN_U = 5.8125f / 16.0f;
    private static final float UV_MAX_U = 9.25f / 16.0f;
    private static final float UV_MIN_V = 10.84375f / 16.0f;
    private static final float UV_MAX_V = 15.59375f / 16.0f;

    public ReelLayout layout(ArchetypeReelState state) {
        List<PlacedCard> cards = new ArrayList<>();
        if (state == null || !state.active() || state.choices().isEmpty()) {
            return new ReelLayout(cards);
        }
        int size = state.choices().size();
        float animatedFocus = state.animatedFocusPosition();
        for (int index = 0; index < size; index++) {
            float relativeOffset = wrappedRelativeOffset(index, animatedFocus, size);
            if (Math.abs(relativeOffset) > 2.5f) {
                continue;
            }
            cards.add(buildPlacedCard(index, state.choices().get(index), relativeOffset, state));
        }
        cards.sort(Comparator.comparingDouble(PlacedCard::centerZ));
        return new ReelLayout(cards);
    }

    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float sceneScale,
            RenderLayer renderLayer,
            ArchetypeReelState state,
            ReelLayout layout
    ) {
        if (layout.cards().isEmpty()) {
            return;
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(CARD_TEXTURE));
        for (PlacedCard card : layout.cards()) {
            if (card.renderLayer() == renderLayer) {
                renderCard(poseStack, buffer, packedLight, sceneScale, card, state);
            }
        }
    }

    private void renderCard(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            float sceneScale,
            PlacedCard card,
            ArchetypeReelState state
    ) {
        poseStack.pushPose();
        // Match SurfaceProjectionService: reel centers are in model units and scaled by scene scale before screen mapping.
        poseStack.translate(card.centerX() * sceneScale, card.centerY() * sceneScale, card.centerZ() * sceneScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(card.yawDegrees()));
        if (card.focused()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.focusedTiltYaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.focusedTiltPitch()));
        }
        float scale = card.scale() * sceneScale;
        poseStack.scale(scale, scale, scale);

        CardTint tint = tintFor(card, state);
        float halfWidth = CARD_WIDTH * 0.5f;
        float halfHeight = CARD_HEIGHT * 0.5f;

        if (card.focused()) {
            float pulse = outlinePulse(state);
            float global = state.globalPresentationAlpha();
            // Place the outline strips just in front of the card face so they sit at the edges cleanly.
            float outlineZ = CARD_THICKNESS + 0.008f;

            // Outer penumbra — wide dim-amber strips that suggest a warm aura around the card.
            float auraWidth = 0.22f + (pulse * 0.12f);
            int auraAlpha = Math.min(255, Math.round((145 + (pulse * 60)) * global));
            emitCardOutline(poseStack, buffer, packedLight, halfWidth, halfHeight, outlineZ,
                    auraWidth, 160, 110, 32, auraAlpha);

            // Crisp inner rim — bright gold border that tightens on top of the aura.
            float rimWidth = 0.055f + (pulse * 0.018f);
            int rimAlpha = Math.min(255, Math.round((225 + (pulse * 30)) * global));
            emitCardOutline(poseStack, buffer, packedLight, halfWidth, halfHeight, outlineZ,
                    rimWidth, 255, 228, 100, rimAlpha);
        }

        // Front face.
        emitFace(poseStack, buffer, packedLight,
                -halfWidth, -halfHeight, CARD_THICKNESS, halfWidth, halfHeight,
                tint.red(), tint.green(), tint.blue(), tint.alpha(), 0.0f, 0.0f, 1.0f);
        // Back face (slightly darker).
        emitFace(poseStack, buffer, packedLight,
                -halfWidth, -halfHeight, -CARD_THICKNESS, halfWidth, halfHeight,
                Math.max(18, tint.red() - 42), Math.max(14, tint.green() - 42), Math.max(10, tint.blue() - 42),
                tint.alpha(), 0.0f, 0.0f, -1.0f);
        poseStack.popPose();
    }

    /**
     * Emits four thin border strips just outside the card's half-extents, forming a rectangular outline.
     * The horizontal strips cover the full width including corners; vertical strips fill the remaining sides.
     */
    private void emitCardOutline(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            float halfW,
            float halfH,
            float z,
            float bw,
            int r, int g, int b, int a
    ) {
        // Top strip — full width including corners.
        emitFace(poseStack, buffer, packedLight,
                -(halfW + bw), -(halfH + bw), z, (halfW + bw), -halfH,
                r, g, b, a, 0.0f, 0.0f, 1.0f);
        // Bottom strip — full width including corners.
        emitFace(poseStack, buffer, packedLight,
                -(halfW + bw), halfH, z, (halfW + bw), (halfH + bw),
                r, g, b, a, 0.0f, 0.0f, 1.0f);
        // Left strip — between top and bottom strips.
        emitFace(poseStack, buffer, packedLight,
                -(halfW + bw), -halfH, z, -halfW, halfH,
                r, g, b, a, 0.0f, 0.0f, 1.0f);
        // Right strip — between top and bottom strips.
        emitFace(poseStack, buffer, packedLight,
                halfW, -halfH, z, (halfW + bw), halfH,
                r, g, b, a, 0.0f, 0.0f, 1.0f);
    }

    private void emitFace(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            float left,
            float top,
            float z,
            float right,
            float bottom,
            int red,
            int green,
            int blue,
            int alpha,
            float normalX,
            float normalY,
            float normalZ
    ) {
        PoseStack.Pose pose = poseStack.last();
        buffer.addVertex(pose.pose(), left, bottom, z)
                .setColor(red, green, blue, alpha)
                .setUv(UV_MIN_U, UV_MAX_V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
        buffer.addVertex(pose.pose(), right, bottom, z)
                .setColor(red, green, blue, alpha)
                .setUv(UV_MAX_U, UV_MAX_V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
        buffer.addVertex(pose.pose(), right, top, z)
                .setColor(red, green, blue, alpha)
                .setUv(UV_MAX_U, UV_MIN_V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
        buffer.addVertex(pose.pose(), left, top, z)
                .setColor(red, green, blue, alpha)
                .setUv(UV_MIN_U, UV_MIN_V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private CardTint tintFor(PlacedCard card, ArchetypeReelState state) {
        int hash = artVariantKey(card.choice(), card.index());
        int baseRed = 154 + Math.floorMod(hash, 46);
        int baseGreen = 116 + Math.floorMod(hash >> 3, 48);
        int baseBlue = 88 + Math.floorMod(hash >> 6, 58);
        float global = state.globalPresentationAlpha();
        int alpha = Math.min(255, Math.round(card.alpha() * 255.0f * global));
        if (card.focused()) {
            return new CardTint(
                Math.min(255, baseRed + 50),
                Math.min(255, baseGreen + 42),
                Math.min(255, baseBlue + 36),
                alpha
            );
        }
        if (card.index() == state.hoveredIndex()) {
            return new CardTint(
                    Math.min(255, baseRed + 28),
                    Math.min(255, baseGreen + 24),
                    Math.min(255, baseBlue + 20),
                    Math.min(255, alpha + Math.round(22 * global))
            );
        }
        return new CardTint(baseRed, baseGreen, baseBlue, alpha);
    }

    private float outlinePulse(ArchetypeReelState state) {
        return switch (state.phase()) {
            case CONFIRMING -> state.transitionProgress();
            case BURNING -> 1.0f - (state.transitionProgress() * 0.35f);
            default -> 0.0f;
        };
    }

    private PlacedCard buildPlacedCard(int index, JournalArchetypeChoice choice, float relativeOffset, ArchetypeReelState state) {
        InterpolatedSlot slot = interpolateSlot(relativeOffset);
        boolean focused = Math.abs(relativeOffset) < 0.08f
                || (state.phase() == ArchetypeReelState.Phase.IDLE && index == state.settledFocusedIndex());
        boolean front = slot.centerZ() >= 7.8f;
        return new PlacedCard(
            index,
            choice,
            slot.centerX(),
            slot.centerY(),
            slot.centerZ(),
            slot.scale(),
            slot.yawDegrees(),
            slot.alpha(),
            front ? RenderLayer.FRONT : RenderLayer.BACK,
            focused
        );
    }

    private InterpolatedSlot interpolateSlot(float relativeOffset) {
        if (relativeOffset <= -2.0f) {
            return InterpolatedSlot.of(ReelSlot.FAR_LEFT);
        }
        if (relativeOffset < -1.0f) {
            float t = -relativeOffset - 1.0f;
            return InterpolatedSlot.lerp(ReelSlot.LEFT, ReelSlot.FAR_LEFT, t);
        }
        if (relativeOffset < 0.0f) {
            float t = -relativeOffset;
            return InterpolatedSlot.lerp(ReelSlot.FOCUSED, ReelSlot.LEFT, t);
        }
        if (relativeOffset < 1.0f) {
            float t = relativeOffset;
            return InterpolatedSlot.lerp(ReelSlot.FOCUSED, ReelSlot.RIGHT, t);
        }
        if (relativeOffset < 2.0f) {
            float t = relativeOffset - 1.0f;
            return InterpolatedSlot.lerp(ReelSlot.RIGHT, ReelSlot.FAR_RIGHT, t);
        }
        return InterpolatedSlot.of(ReelSlot.FAR_RIGHT);
    }

    private float wrappedRelativeOffset(int index, float animatedFocus, int size) {
        float direct = index - animatedFocus;
        float wrappedPositive = direct + size;
        float wrappedNegative = direct - size;
        float best = direct;
        if (Math.abs(wrappedPositive) < Math.abs(best)) {
            best = wrappedPositive;
        }
        if (Math.abs(wrappedNegative) < Math.abs(best)) {
            best = wrappedNegative;
        }
        return best;
    }

    private int artVariantKey(JournalArchetypeChoice choice, int fallback) {
        String artKey = choice.artKey();
        if (artKey != null && !artKey.isBlank()) {
            return artKey.hashCode();
        }
        @Nullable ResourceLocation archetypeId = choice.archetypeId();
        return archetypeId == null ? fallback : archetypeId.hashCode();
    }

    public record ReelLayout(List<PlacedCard> cards) {}

    public record PlacedCard(
        int index,
        JournalArchetypeChoice choice,
        float centerX,
        float centerY,
        float centerZ,
        float scale,
        float yawDegrees,
        float alpha,
        RenderLayer renderLayer,
        boolean focused
    ) {}

    private record CardTint(int red, int green, int blue, int alpha) {}

    private record InterpolatedSlot(
        float centerX,
        float centerY,
        float centerZ,
        float scale,
        float yawDegrees,
        float alpha
    ) {
        static InterpolatedSlot of(ReelSlot slot) {
            return new InterpolatedSlot(
                slot.centerX(),
                slot.centerY(),
                slot.centerZ(),
                slot.scale(),
                slot.yawDegrees(),
                slot.alpha()
            );
        }

        static InterpolatedSlot lerp(ReelSlot from, ReelSlot to, float t) {
            return new InterpolatedSlot(
                from.centerX() + ((to.centerX() - from.centerX()) * t),
                from.centerY() + ((to.centerY() - from.centerY()) * t),
                from.centerZ() + ((to.centerZ() - from.centerZ()) * t),
                from.scale() + ((to.scale() - from.scale()) * t),
                from.yawDegrees() + ((to.yawDegrees() - from.yawDegrees()) * t),
                from.alpha() + ((to.alpha() - from.alpha()) * t)
            );
        }
    }

    public enum RenderLayer {
        BACK,
        FRONT
    }

    private enum ReelSlot {
        // Center hero card + smaller neighbors (mockup: prev | focused | next).
        FOCUSED(0.0f, -0.68f, 9.35f, 1.34f, 0.0f, 1.0f),
        LEFT(-11.0f, -0.62f, 7.15f, 0.86f, 4.0f, 0.92f),
        RIGHT(11.0f, -0.62f, 7.15f, 0.86f, -4.0f, 0.92f),
        FAR_LEFT(-17.0f, -0.5f, 5.0f, 0.68f, 6.0f, 0.58f),
        FAR_RIGHT(17.0f, -0.5f, 5.0f, 0.68f, -6.0f, 0.58f);

        private final float centerX;
        private final float centerY;
        private final float centerZ;
        private final float scale;
        private final float yawDegrees;
        private final float alpha;

        ReelSlot(float centerX, float centerY, float centerZ, float scale, float yawDegrees, float alpha) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.scale = scale;
            this.yawDegrees = yawDegrees;
            this.alpha = alpha;
        }

        float centerX() { return centerX; }
        float centerY() { return centerY; }
        float centerZ() { return centerZ; }
        float scale() { return scale; }
        float yawDegrees() { return yawDegrees; }
        float alpha() { return alpha; }
    }
}
