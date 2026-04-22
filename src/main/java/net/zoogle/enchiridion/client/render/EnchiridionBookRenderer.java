package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookPageSide;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class EnchiridionBookRenderer extends GeoObjectRenderer<EnchiridionBookAnimatable> {
    private static final String MAGIC_TEXT_LEFT_BONE = "magic_text_left";
    private static final String MAGIC_TEXT_RIGHT_BONE = "magic_text_right";
    private static final float SURFACE_STABILITY_AREA_RATIO = 0.92f;
    private static volatile CapturedPageSurface leftPageSurface;
    private static volatile CapturedPageSurface rightPageSurface;

    EnchiridionBookRenderer() {
        super(new EnchiridionBookGeoModel());
        addRenderLayer(new DecorativePageMaskLayer(this));
        addRenderLayer(new MagicTextLayer(this));
    }

    static CapturedPageSurface capturedPageSurface(BookPageSide pageSide) {
        return pageSide == BookPageSide.LEFT ? leftPageSurface : rightPageSurface;
    }

    private static void setCapturedPageSurface(BookPageSide pageSide, CapturedPageSurface surface) {
        if (pageSide == BookPageSide.LEFT) {
            leftPageSurface = surface;
        } else {
            rightPageSurface = surface;
        }
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

            BookPageSide pageSide = bone.getName().equals(MAGIC_TEXT_LEFT_BONE) ? BookPageSide.LEFT : BookPageSide.RIGHT;
            CapturedPageSurface surface = capturePageSurface(pageSide, poseStack, bone);
            if (surface != null) {
                setCapturedPageSurface(pageSide, surface);
            }

            VertexConsumer magicBuffer = bufferSource.getBuffer(RenderType.entityCutout(magicTexture));
            getRenderer().renderCubesOfBone(poseStack, bone, magicBuffer, packedLight, packedOverlay, 0xFFFFFFFF);
        }

        private CapturedPageSurface capturePageSurface(BookPageSide pageSide, PoseStack poseStack, GeoBone bone) {
            if (bone.getCubes().isEmpty()) {
                return null;
            }
            List<CapturedFace> faces = new ArrayList<>();
            for (GeoCube cube : bone.getCubes()) {
                poseStack.pushPose();
                RenderUtil.translateToPivotPoint(poseStack, cube);
                RenderUtil.rotateMatrixAroundCube(poseStack, cube);
                RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
                Matrix4f matrix = new Matrix4f(poseStack.last().pose());
                for (GeoQuad quad : cube.quads()) {
                    CapturedFace face = captureFace(pageSide, matrix, quad);
                    if (face == null) {
                        continue;
                    }
                    faces.add(face);
                }
                poseStack.popPose();
            }
            if (faces.isEmpty()) {
                return null;
            }
            CapturedPageSurface previousSurface = EnchiridionBookRenderer.capturedPageSurface(pageSide);
            CapturedFace bestFace = selectStableFace(faces, previousSurface);
            return bestFace.surface();
        }

        private CapturedFace captureFace(BookPageSide pageSide, Matrix4f matrix, GeoQuad quad) {
            List<CapturedVertex> vertices = new ArrayList<>(4);
            for (GeoVertex vertex : quad.vertices()) {
                BookPageTexturePipeline.LocalTexturePoint local = BookPageTexturePipeline.localPointForTextureUv(pageSide, vertex.texU(), vertex.texV());
                if (local == null) {
                    return null;
                }
                Vector4f point = matrix.transform(new Vector4f(vertex.position().x(), vertex.position().y(), vertex.position().z(), 1.0f));
                vertices.add(new CapturedVertex(point.x(), point.y(), local.localX(), local.localY()));
            }
            vertices.sort(Comparator.comparingDouble(CapturedVertex::localY).thenComparingDouble(CapturedVertex::localX));
            CapturedVertex topLeft = nearest(vertices, 0.0f, 0.0f);
            CapturedVertex topRight = nearest(vertices, renderWidth(pageSide), 0.0f);
            CapturedVertex bottomRight = nearest(vertices, renderWidth(pageSide), renderHeight(pageSide));
            CapturedVertex bottomLeft = nearest(vertices, 0.0f, renderHeight(pageSide));
            if (topLeft == null || topRight == null || bottomRight == null || bottomLeft == null) {
                return null;
            }
            CapturedPageSurface surface = new CapturedPageSurface(
                    pageSide,
                    new BookSceneRenderer.ScreenPoint(topLeft.screenX(), topLeft.screenY()),
                    new BookSceneRenderer.ScreenPoint(topRight.screenX(), topRight.screenY()),
                    new BookSceneRenderer.ScreenPoint(bottomRight.screenX(), bottomRight.screenY()),
                    new BookSceneRenderer.ScreenPoint(bottomLeft.screenX(), bottomLeft.screenY()),
                    renderWidth(pageSide),
                    renderHeight(pageSide)
            );
            return new CapturedFace(surface, quadArea(surface));
        }

        private float renderWidth(BookPageSide pageSide) {
            return BookPageTexturePipeline.renderRegionSize(pageSide).width();
        }

        private float renderHeight(BookPageSide pageSide) {
            return BookPageTexturePipeline.renderRegionSize(pageSide).height();
        }

        private CapturedVertex nearest(List<CapturedVertex> vertices, float localX, float localY) {
            CapturedVertex best = null;
            float bestDistance = Float.MAX_VALUE;
            for (CapturedVertex vertex : vertices) {
                float dx = vertex.localX() - localX;
                float dy = vertex.localY() - localY;
                float distance = (dx * dx) + (dy * dy);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = vertex;
                }
            }
            return best;
        }

        private float quadArea(CapturedPageSurface surface) {
            return Math.abs(
                    (surface.topLeft().x() * surface.topRight().y())
                            + (surface.topRight().x() * surface.bottomRight().y())
                            + (surface.bottomRight().x() * surface.bottomLeft().y())
                            + (surface.bottomLeft().x() * surface.topLeft().y())
                            - (surface.topLeft().y() * surface.topRight().x())
                            - (surface.topRight().y() * surface.bottomRight().x())
                            - (surface.bottomRight().y() * surface.bottomLeft().x())
                            - (surface.bottomLeft().y() * surface.topLeft().x())
            ) * 0.5f;
        }

        private CapturedFace selectStableFace(List<CapturedFace> faces, CapturedPageSurface previousSurface) {
            CapturedFace largestFace = faces.stream()
                    .max(Comparator.comparingDouble(CapturedFace::area))
                    .orElseThrow();
            if (previousSurface == null || faces.size() == 1) {
                return largestFace;
            }

            float minArea = largestFace.area() * SURFACE_STABILITY_AREA_RATIO;
            CapturedFace stableFace = null;
            float stableDistance = Float.MAX_VALUE;
            for (CapturedFace face : faces) {
                if (face.area() < minArea) {
                    continue;
                }
                float distance = surfaceDistanceSquared(previousSurface, face.surface());
                if (stableFace == null || distance < stableDistance) {
                    stableFace = face;
                    stableDistance = distance;
                }
            }
            return stableFace == null ? largestFace : stableFace;
        }

        private float surfaceDistanceSquared(CapturedPageSurface first, CapturedPageSurface second) {
            return pointDistanceSquared(first.topLeft(), second.topLeft())
                    + pointDistanceSquared(first.topRight(), second.topRight())
                    + pointDistanceSquared(first.bottomRight(), second.bottomRight())
                    + pointDistanceSquared(first.bottomLeft(), second.bottomLeft());
        }

        private float pointDistanceSquared(BookSceneRenderer.ScreenPoint first, BookSceneRenderer.ScreenPoint second) {
            float dx = first.x() - second.x();
            float dy = first.y() - second.y();
            return (dx * dx) + (dy * dy);
        }
    }

    record CapturedPageSurface(
            BookPageSide pageSide,
            BookSceneRenderer.ScreenPoint topLeft,
            BookSceneRenderer.ScreenPoint topRight,
            BookSceneRenderer.ScreenPoint bottomRight,
            BookSceneRenderer.ScreenPoint bottomLeft,
            float localWidth,
            float localHeight
    ) {}

    private record CapturedFace(CapturedPageSurface surface, float area) {}

    private record CapturedVertex(float screenX, float screenY, float localX, float localY) {}
}
