package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.Enchiridion;
import net.zoogle.enchiridion.client.levelrpg.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.JournalArchetypeChoice;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArchetypeReelSceneRenderer {
    private static final ResourceLocation CARD_TEXTURE_FALLBACK = EnchiridionBookGeoModel.BLANK_FRONT_TEXTURE;
    private static final Map<String, String> CARD_TEXTURE_ALIASES = Map.of(
            "spellblade", "spellsword",
            "master_crafter", "inventor"
    );
    private static final Set<String> CARD_TEXTURE_KEYS = Set.of(
            "knight",
            "inventor",
            "peasant",
            "spellsword"
    );
    private static final float CARD_WIDTH = 59.0f / 16.0f;
    private static final float CARD_HEIGHT = 80.0f / 16.0f;
    private static final float CARD_THICKNESS = 0.04f;
    private static final float UV_MIN_U = 5.8125f / 16.0f;
    private static final float UV_MAX_U = 9.25f / 16.0f;
    private static final float UV_MIN_V = 10.84375f / 16.0f;
    private static final float UV_MAX_V = 15.59375f / 16.0f;
    private static final float CIRCLE_RADIUS_X = 13.2f;
    private static final float CIRCLE_RADIUS_Z = 4.2f;
    private static final float CIRCLE_CENTER_Z = 7.25f;
    private static final float CIRCLE_CENTER_Y = -0.64f;
    private static final float BASE_SCALE = 0.84f;
    private static final float SCALE_DEPTH_BOOST = 0.6f;
    private static final float FRONT_ALPHA_MIN = 0.46f;
    private static final float FRONT_ALPHA_MAX = 1.0f;
    private static final float HELD_CARD_SCALE = 2.05f;
    private static final float HOVER_SCALE_BOOST = 0.14f;

    public ReelLayout layout(ArchetypeReelState state, BookSceneRenderer.ReelPresentationTransform presentation) {
        List<PlacedCard> cards = new ArrayList<>();
        if (state == null || !state.active() || state.choices().isEmpty()) {
            return new ReelLayout(cards);
        }
        List<Integer> activeIndices = activeReelIndices(state);
        if (activeIndices.isEmpty()) {
            return new ReelLayout(cards);
        }
        if (!state.dragging()) {
            int count = activeIndices.size();
            float activeFocusPosition = activeFocusPosition(state, activeIndices);
            float stepDegrees = 360.0f / count;
            for (int ordinal = 0; ordinal < count; ordinal++) {
                int index = activeIndices.get(ordinal);
                float relative = wrappedRelativeOffset(ordinal, activeFocusPosition, count);
                float angleDeg = relative * stepDegrees;
                cards.add(buildPlacedCard(index, state.choices().get(index), angleDeg, state, count, false));
            }
        }
        if (state.dragging() && state.draggedChoice() != null && presentation != null) {
            PlacedCard heldCard = buildHeldCard(state, presentation);
            if (heldCard != null) {
                cards.add(heldCard);
            }
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
        for (PlacedCard card : layout.cards()) {
            if (card.renderLayer() == renderLayer) {
                ResourceLocation texture = textureFor(card.choice());
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
                boolean fullFaceTexture = !CARD_TEXTURE_FALLBACK.equals(texture);
                renderCard(poseStack, buffer, packedLight, sceneScale, card, state, fullFaceTexture);
            }
        }
    }

    private static ResourceLocation textureFor(JournalArchetypeChoice choice) {
        if (choice == null) {
            return CARD_TEXTURE_FALLBACK;
        }
        String normalized = normalizeArtKey(choice.artKey());
        if (normalized.isBlank()) {
            normalized = normalizeArtKey(choice.focusId());
        }
        if (normalized.isBlank()) {
            return CARD_TEXTURE_FALLBACK;
        }
        String path = CARD_TEXTURE_ALIASES.getOrDefault(normalized, normalized);
        if (!CARD_TEXTURE_KEYS.contains(path)) {
            return CARD_TEXTURE_FALLBACK;
        }
        return ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "textures/gui/" + path + ".png");
    }

    private static String normalizeArtKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        int namespaceSeparator = key.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < key.length()) {
            key = key.substring(namespaceSeparator + 1);
        }
        int pathSeparator = key.lastIndexOf('/');
        if (pathSeparator >= 0 && pathSeparator + 1 < key.length()) {
            key = key.substring(pathSeparator + 1);
        }
        return key.replace('-', '_');
    }

    private void renderCard(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            float sceneScale,
            PlacedCard card,
            ArchetypeReelState state,
            boolean fullFaceTexture
    ) {
        poseStack.pushPose();
        // Match SurfaceProjectionService: reel centers are in model units and scaled by scene scale before screen mapping.
        poseStack.translate(card.centerX() * sceneScale, card.centerY() * sceneScale, card.centerZ() * sceneScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(card.yawDegrees()));
        if (card.focused()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.focusedTiltYaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.focusedTiltPitch()));
        } else if (state.hoveredIndex() == card.index()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.focusedTiltYaw() * 0.45f));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.focusedTiltPitch() * 0.45f));
        }
        float scale = card.scale() * sceneScale;
        poseStack.scale(scale, scale, scale);

        CardTint tint = tintFor(card, state);
        float halfWidth = CARD_WIDTH * 0.5f;
        float halfHeight = CARD_HEIGHT * 0.5f;
        float uvMinU = fullFaceTexture ? 0.0f : UV_MIN_U;
        float uvMaxU = fullFaceTexture ? 1.0f : UV_MAX_U;
        float uvMinV = fullFaceTexture ? 0.0f : UV_MIN_V;
        float uvMaxV = fullFaceTexture ? 1.0f : UV_MAX_V;

        // Front face.
        emitFace(poseStack, buffer, packedLight,
                -halfWidth, -halfHeight, CARD_THICKNESS, halfWidth, halfHeight,
                tint.red(), tint.green(), tint.blue(), tint.alpha(),
                uvMinU, uvMaxU, uvMinV, uvMaxV,
                0.0f, 0.0f, 1.0f);
        // Back face (slightly darker).
        emitFace(poseStack, buffer, packedLight,
                -halfWidth, -halfHeight, -CARD_THICKNESS, halfWidth, halfHeight,
                Math.max(18, tint.red() - 42), Math.max(14, tint.green() - 42), Math.max(10, tint.blue() - 42),
                tint.alpha(),
                uvMinU, uvMaxU, uvMinV, uvMaxV,
                0.0f, 0.0f, -1.0f);
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
            float uvMinU,
            float uvMaxU,
            float uvMinV,
            float uvMaxV,
            float normalX,
            float normalY,
            float normalZ
    ) {
        PoseStack.Pose pose = poseStack.last();
        buffer.addVertex(pose.pose(), left, bottom, z)
                .setColor(red, green, blue, alpha)
                .setUv(uvMinU, uvMaxV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
        buffer.addVertex(pose.pose(), right, bottom, z)
                .setColor(red, green, blue, alpha)
                .setUv(uvMaxU, uvMaxV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
        buffer.addVertex(pose.pose(), right, top, z)
                .setColor(red, green, blue, alpha)
                .setUv(uvMaxU, uvMinV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
        buffer.addVertex(pose.pose(), left, top, z)
                .setColor(red, green, blue, alpha)
                .setUv(uvMinU, uvMinV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private CardTint tintFor(PlacedCard card, ArchetypeReelState state) {
        float global = state.globalPresentationAlpha();
        int alpha = Math.min(255, Math.round(card.alpha() * 255.0f * global));
        return new CardTint(255, 255, 255, alpha);
    }

    private PlacedCard buildPlacedCard(int index, JournalArchetypeChoice choice, float angleDeg, ArchetypeReelState state, int count, boolean detached) {
        double angleRad = Math.toRadians(angleDeg);
        float sin = (float) Math.sin(angleRad);
        float cos = (float) Math.cos(angleRad);
        float radiusBoost = Math.max(0.0f, count - 7) * 0.52f;
        float radiusX = CIRCLE_RADIUS_X + radiusBoost;
        float radiusZ = CIRCLE_RADIUS_Z + (radiusBoost * 0.24f);
        float centerX = sin * radiusX;
        float centerY = CIRCLE_CENTER_Y;
        float centerZ = CIRCLE_CENTER_Z + (cos * radiusZ);
        float depthNorm = (cos + 1.0f) * 0.5f;
        float densityScale = 1.0f - (Math.max(0.0f, count - 8) * 0.03f);
        float scale = (BASE_SCALE + (depthNorm * SCALE_DEPTH_BOOST)) * Math.max(0.68f, densityScale);
        if (!detached && state.hoveredIndex() == index) {
            scale += HOVER_SCALE_BOOST;
        }
        float alpha = FRONT_ALPHA_MIN + ((FRONT_ALPHA_MAX - FRONT_ALPHA_MIN) * depthNorm);
        float yaw = -sin * 18.0f;
        boolean focused = !detached && index == state.focusedIndex();
        boolean front = centerZ >= CIRCLE_CENTER_Z;
        return new PlacedCard(
            index,
            choice,
            centerX,
            centerY,
            centerZ,
            scale,
            yaw,
            alpha,
            front ? RenderLayer.FRONT : RenderLayer.BACK,
            focused,
            detached
        );
    }

    private PlacedCard buildHeldCard(ArchetypeReelState state, BookSceneRenderer.ReelPresentationTransform presentation) {
        JournalArchetypeChoice choice = state.draggedChoice();
        int index = state.draggedIndex();
        if (choice == null || index < 0) {
            return null;
        }
        float heldDepthLocal = CIRCLE_CENTER_Z + 4.6f;
        float heldDepth = presentation.centerZ() + (heldDepthLocal * presentation.scale());
        float perspective = BookSceneRenderer.PAGE_PICK_CAMERA_DEPTH / Math.max(1.0f, BookSceneRenderer.PAGE_PICK_CAMERA_DEPTH - heldDepth);
        float localScale = Math.max(0.0001f, presentation.scale() * perspective);
        float centerX = (state.dragMouseX() - presentation.centerX()) / localScale;
        float centerY = (state.dragMouseY() - presentation.centerY()) / localScale;
        return new PlacedCard(
                index,
                choice,
                centerX,
                centerY,
                heldDepthLocal,
                HELD_CARD_SCALE,
                0.0f,
                0.98f,
                RenderLayer.FRONT,
                false,
                true
        );
    }

    private List<Integer> activeReelIndices(ArchetypeReelState state) {
        int size = state.choices().size();
        if (size <= 0) {
            return List.of();
        }
        int draggedIndex = state.draggedIndex();
        boolean hideDragged = state.dragging() && draggedIndex >= 0 && draggedIndex < size;
        List<Integer> indices = new ArrayList<>(hideDragged ? size - 1 : size);
        for (int index = 0; index < size; index++) {
            if (hideDragged && index == draggedIndex) {
                continue;
            }
            indices.add(index);
        }
        return indices;
    }

    private float activeFocusPosition(ArchetypeReelState state, List<Integer> activeIndices) {
        if (activeIndices.isEmpty()) {
            return 0.0f;
        }
        float focus = state.animatedFocusPosition();
        int dragged = state.draggedIndex();
        if (state.dragging() && dragged >= 0) {
            if (focus >= dragged) {
                focus -= 1.0f;
            }
        }
        int count = activeIndices.size();
        if (count <= 0) {
            return 0.0f;
        }
        float wrapped = focus % count;
        if (wrapped < 0.0f) {
            wrapped += count;
        }
        return wrapped;
    }

    private float wrappedRelativeOffset(float index, float animatedFocus, int size) {
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
        boolean focused,
        boolean detached
    ) {}

    private record CardTint(int red, int green, int blue, int alpha) {}

    public enum RenderLayer {
        BACK,
        FRONT
    }
}
