package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

final class EnchiridionBookRenderer extends GeoObjectRenderer<EnchiridionBookAnimatable> {
    private static final String MAGIC_TEXT_LEFT_BONE = "magic_text_left";
    private static final String MAGIC_TEXT_RIGHT_BONE = "magic_text_right";

    EnchiridionBookRenderer() {
        super(new EnchiridionBookGeoModel());
        addRenderLayer(new DecorativePageMaskLayer(this));
        addRenderLayer(new MagicTextLayer(this));
    }

    private static final class DecorativePageMaskLayer extends GeoRenderLayer<EnchiridionBookAnimatable> {
        private DecorativePageMaskLayer(EnchiridionBookRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(
                PoseStack poseStack,
                EnchiridionBookAnimatable animatable,
                GeoBone bone,
                RenderType renderType,
                MultiBufferSource bufferSource,
                VertexConsumer buffer,
                float partialTick,
                int packedLight,
                int packedOverlay
        ) {
            if (!isDecorativePageBone(bone.getName())) {
                return;
            }

            ResourceLocation parchmentTexture = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
            VertexConsumer parchmentBuffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(parchmentTexture));
            getRenderer().renderCubesOfBone(poseStack, bone, parchmentBuffer, packedLight, packedOverlay, 0xFFFFFFFF);
        }

        private static boolean isDecorativePageBone(String boneName) {
            return isPageBone(boneName);
        }

        private static boolean isPageBone(String boneName) {
            return boneName.startsWith("page_") || boneName.startsWith("a_page_");
        }

    }

    private static final class MagicTextLayer extends GeoRenderLayer<EnchiridionBookAnimatable> {
        private MagicTextLayer(EnchiridionBookRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(
                PoseStack poseStack,
                EnchiridionBookAnimatable animatable,
                GeoBone bone,
                RenderType renderType,
                MultiBufferSource bufferSource,
                VertexConsumer buffer,
                float partialTick,
                int packedLight,
                int packedOverlay
        ) {
            ResourceLocation magicTexture = switch (bone.getName()) {
                case MAGIC_TEXT_LEFT_BONE -> animatable.magicLeftTextureLocation();
                case MAGIC_TEXT_RIGHT_BONE -> animatable.magicRightTextureLocation();
                default -> null;
            };

            if (magicTexture == null) {
                return;
            }

            VertexConsumer magicBuffer = bufferSource.getBuffer(RenderType.entityCutout(magicTexture));
            getRenderer().renderCubesOfBone(poseStack, bone, magicBuffer, packedLight, packedOverlay, 0xFFFFFFFF);
        }
    }
}
