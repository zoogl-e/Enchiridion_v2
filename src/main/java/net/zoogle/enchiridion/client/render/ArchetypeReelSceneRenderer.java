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
        float animatedFocus = state.animatedFocusPosition();
        int focusedFloor = (int) Math.floor(animatedFocus);
        for (int offset = -2; offset <= 2; offset++) {
            int index = Math.floorMod(focusedFloor + offset, state.choices().size());
            float relativeOffset = wrappedRelativeOffset(index, animatedFocus, state.choices().size());
            if (Math.abs(relativeOffset) > 2.35f) {
                continue;
            }
            cards.add(buildPlacedCard(index, state.choices().get(index), relativeOffset));
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
            ArchetypeReelState state
    ) {
        ReelLayout layout = layout(state);
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
        poseStack.translate(card.centerX(), card.centerY(), card.centerZ());
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
            float outlineScale = 1.1f + (outlinePulse(state) * 0.035f);
            emitFace(
                poseStack,
                buffer,
                packedLight,
                -halfWidth * outlineScale,
                -halfHeight * outlineScale,
                -CARD_THICKNESS * 2.2f,
                halfWidth * outlineScale,
                halfHeight * outlineScale,
                249,
                232,
                191,
                132,
                0.0f,
                0.0f,
                -1.0f
            );
            emitFace(
                poseStack,
                buffer,
                packedLight,
                -halfWidth * 1.03f,
                -halfHeight * 1.03f,
                -CARD_THICKNESS * 1.15f,
                halfWidth * 1.03f,
                halfHeight * 1.03f,
                116,
                82,
                44,
                118,
                0.0f,
                0.0f,
                -1.0f
            );
        }

        emitFace(
                poseStack,
                buffer,
                packedLight,
                -halfWidth,
                -halfHeight,
                CARD_THICKNESS,
                halfWidth,
                halfHeight,
                tint.red(),
                tint.green(),
                tint.blue(),
                tint.alpha(),
                0.0f,
                0.0f,
                1.0f
        );
        emitFace(
                poseStack,
                buffer,
                packedLight,
                -halfWidth,
                -halfHeight,
                -CARD_THICKNESS,
                halfWidth,
                halfHeight,
                Math.max(18, tint.red() - 42),
                Math.max(14, tint.green() - 42),
                Math.max(10, tint.blue() - 42),
                tint.alpha(),
                0.0f,
                0.0f,
                -1.0f
        );
        poseStack.popPose();
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
        int alpha = Math.round(card.alpha() * 255.0f);
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
                Math.min(255, alpha + 22)
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

    private PlacedCard buildPlacedCard(int index, JournalArchetypeChoice choice, float relativeOffset) {
        ReelSlot slot = slotFor(relativeOffset);
        ReelSlot nextSlot = nextSlotToward(relativeOffset, slot);
        float slotProgress = slotProgress(relativeOffset, slot);
        boolean front = slot == ReelSlot.FOCUSED || nextSlot == ReelSlot.FOCUSED;
        return new PlacedCard(
            index,
            choice,
            lerp(slot.centerX(), nextSlot.centerX(), slotProgress),
            lerp(slot.centerY(), nextSlot.centerY(), slotProgress),
            lerp(slot.centerZ(), nextSlot.centerZ(), slotProgress),
            lerp(slot.scale(), nextSlot.scale(), slotProgress),
            lerp(slot.yawDegrees(), nextSlot.yawDegrees(), slotProgress),
            lerp(slot.alpha(), nextSlot.alpha(), slotProgress),
            front ? RenderLayer.FRONT : RenderLayer.BACK,
            slot == ReelSlot.FOCUSED || nextSlot == ReelSlot.FOCUSED
        );
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

    private float lerp(float from, float to, float progress) {
        return from + ((to - from) * progress);
    }

    private int artVariantKey(JournalArchetypeChoice choice, int fallback) {
        String artKey = choice.artKey();
        if (artKey != null && !artKey.isBlank()) {
            return artKey.hashCode();
        }
        @Nullable ResourceLocation archetypeId = choice.archetypeId();
        return archetypeId == null ? fallback : archetypeId.hashCode();
    }

    private ReelSlot slotFor(float relativeOffset) {
        float magnitude = Math.abs(relativeOffset);
        if (magnitude < 0.5f) {
            return ReelSlot.FOCUSED;
        }
        if (magnitude < 1.5f) {
            return relativeOffset < 0.0f ? ReelSlot.LEFT : ReelSlot.RIGHT;
        }
        return relativeOffset < 0.0f ? ReelSlot.FAR_LEFT : ReelSlot.FAR_RIGHT;
    }

    private ReelSlot nextSlotToward(float relativeOffset, ReelSlot current) {
        return switch (current) {
            case FOCUSED -> relativeOffset < 0.0f ? ReelSlot.LEFT : ReelSlot.RIGHT;
            case LEFT -> relativeOffset < -1.0f ? ReelSlot.FAR_LEFT : ReelSlot.FOCUSED;
            case RIGHT -> relativeOffset > 1.0f ? ReelSlot.FAR_RIGHT : ReelSlot.FOCUSED;
            case FAR_LEFT -> ReelSlot.LEFT;
            case FAR_RIGHT -> ReelSlot.RIGHT;
        };
    }

    private float slotProgress(float relativeOffset, ReelSlot slot) {
        float magnitude = Math.abs(relativeOffset);
        return switch (slot) {
            case FOCUSED -> clamp(magnitude / 0.5f, 0.0f, 1.0f);
            case LEFT, RIGHT -> magnitude < 1.0f
                ? clamp((1.0f - magnitude) / 0.5f, 0.0f, 1.0f)
                : clamp((magnitude - 1.0f) / 0.5f, 0.0f, 1.0f);
            case FAR_LEFT, FAR_RIGHT -> clamp((2.0f - magnitude) / 0.5f, 0.0f, 1.0f);
        };
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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

    public enum RenderLayer {
        BACK,
        FRONT
    }

    private enum ReelSlot {
        FOCUSED(-1.65f, -1.56f, 12.4f, 1.58f, 0.0f, 1.0f),
        LEFT(-5.45f, -0.72f, -6.7f, 0.66f, 35.0f, 0.8f),
        RIGHT(3.45f, -0.72f, -6.3f, 0.66f, -35.0f, 0.8f),
        FAR_LEFT(-8.5f, -0.34f, -10.6f, 0.5f, 44.0f, 0.24f),
        FAR_RIGHT(6.8f, -0.34f, -10.2f, 0.5f, -44.0f, 0.24f);

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
