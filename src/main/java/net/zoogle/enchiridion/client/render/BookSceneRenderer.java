package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimationSpec;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.ui.BookLayout;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.List;

/**
 * v2 internal scene renderer backed by GeckoLib.
 * This stays inside Enchiridion; external books still provide content only.
 */
public final class BookSceneRenderer {
    public static final float PROJECTION_BOOK_Y_OFFSET = 200.0f;
    public static final float PROJECTION_FOCUS_X_OFFSET = 200.0f;
    public static final float PROJECTION_ZOOM_SCALE = 2.5f;

    private static final float REFERENCE_MODEL_SCALE = 30.0f;
    private static final float GUI_Z = 180.0f;
    private static final float MODEL_MIRROR_X = -1.0f;
    private static final float MODEL_SCALE = 33.0f;
    private static final float CLOSED_HOVER_SCALE = 1.035f;
    private static final float CLOSED_CENTER_X_OFFSET = -52.0f * (MODEL_SCALE / REFERENCE_MODEL_SCALE);
    private static final float OPEN_CENTER_X_OFFSET = 18.0f * (MODEL_SCALE / REFERENCE_MODEL_SCALE);
    private static final float CLOSED_HITBOX_X_OFFSET = 0.0f;
    private static final float MODEL_Y_OFFSET = 100.0f;
    private static final float MODEL_X_ROTATION = 180.0f;
    private static final float MODEL_Y_ROTATION = 0.0f;
    private static final float MAX_TILT_YAW_DEGREES = 8.0f;
    private static final float MAX_TILT_PITCH_DEGREES = 5.0f;
    private static final float MAX_INSPECT_PITCH_DEGREES = 28.0f;
    private static final float CLOSED_PRESENTATION_YAW = 0f;
    private static final float PAGE_PICK_CAMERA_DEPTH = 1600.0f;
    private static final float MODEL_UNIT = 1.0f / 16.0f;
    private static final float BOOK_PIVOT_X = 0.0f;
    private static final float BOOK_PIVOT_Y = 45.0f;
    private static final float BOOK_PIVOT_Z = 1.5f;
    private static final float PAGE_SURFACE_DEPTH = 0.05f;

    private static final EnchiridionBookRenderer BOOK_RENDERER = new EnchiridionBookRenderer();
    private final EnchiridionBookAnimatable bookAnimatable = new EnchiridionBookAnimatable();
    private final BookPageTexturePipeline pageTexturePipeline = new BookPageTexturePipeline();
    private float currentTiltYaw;
    private float currentTiltPitch;
    private float currentClosedHoverScale = 1.0f;
    private float currentInspectYaw;
    private float currentInspectPitch;

    public PageSurfaceBounds pageSurfaceBounds(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide
    ) {
        return projectedPageSurface(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide);
    }

    public void preparePresentation(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            int mouseX,
            int mouseY,
            boolean closedHovered,
            float inspectYaw,
            float inspectPitch
    ) {
        float closedness = closednessFor(state, animationProgress);
        float targetClosedHoverScale = closedHovered && closedness >= 0.999f ? CLOSED_HOVER_SCALE : 1.0f;
        currentClosedHoverScale = approach(currentClosedHoverScale, targetClosedHoverScale, BookAnimationSpec.hoverSmoothing());

        float normalizedMouseX = normalizeMouseOffset(mouseX, layout.bookX(), layout.bookWidth());
        float normalizedMouseY = normalizeMouseOffset(mouseY, layout.bookY(), layout.bookHeight());
        float targetTiltYaw = normalizedMouseX * MAX_TILT_YAW_DEGREES;
        float targetTiltPitch = -normalizedMouseY * MAX_TILT_PITCH_DEGREES;
        currentTiltYaw = approach(currentTiltYaw, targetTiltYaw, BookAnimationSpec.tiltSmoothing());
        currentTiltPitch = approach(currentTiltPitch, targetTiltPitch, BookAnimationSpec.tiltSmoothing());
        float targetInspectYaw = inspectYaw * closedness;
        float targetInspectPitch = clamp(inspectPitch, -MAX_INSPECT_PITCH_DEGREES, MAX_INSPECT_PITCH_DEGREES) * closedness;
        currentInspectYaw = approach(currentInspectYaw, targetInspectYaw, BookAnimationSpec.inspectSmoothing());
        currentInspectPitch = approach(currentInspectPitch, targetInspectPitch, BookAnimationSpec.inspectSmoothing());
    }

    public PageLocalPoint pageLocalPoint(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            double mouseX,
            double mouseY
    ) {
        PageSurfaceBounds bounds = pageSurfaceBounds(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide);
        if (mouseX < bounds.screenX()
                || mouseX >= bounds.screenX() + bounds.screenWidth()
                || mouseY < bounds.screenY()
                || mouseY >= bounds.screenY() + bounds.screenHeight()) {
            return null;
        }
        ScreenPoint mousePoint = new ScreenPoint((float) mouseX, (float) mouseY);
        PageLocalPoint point = resolvePointInTriangle(
                bounds,
                mousePoint,
                bounds.topLeft(),
                bounds.topRight(),
                bounds.bottomRight(),
                0.0f,
                0.0f,
                bounds.localWidth(),
                0.0f,
                bounds.localWidth(),
                bounds.localHeight()
        );
        if (point != null) {
            return point;
        }
        return resolvePointInTriangle(
                bounds,
                mousePoint,
                bounds.topLeft(),
                bounds.bottomRight(),
                bounds.bottomLeft(),
                0.0f,
                0.0f,
                bounds.localWidth(),
                bounds.localHeight(),
                0.0f,
                bounds.localHeight()
        );
    }

    public ScreenRect projectPageRect(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int localX,
            int localY,
            int localWidth,
            int localHeight
    ) {
        ScreenQuad quad = projectPageQuad(
                layout,
                state,
                animationProgress,
                projectionProgress,
                projectionFocusOffset,
                pageSide,
                localX,
                localY,
                localWidth,
                localHeight
        );
        ScreenPoint topLeft = quad.topLeft();
        ScreenPoint topRight = quad.topRight();
        ScreenPoint bottomRight = quad.bottomRight();
        ScreenPoint bottomLeft = quad.bottomLeft();
        float left = min(topLeft.x(), topRight.x(), bottomRight.x(), bottomLeft.x());
        float top = min(topLeft.y(), topRight.y(), bottomRight.y(), bottomLeft.y());
        float right = max(topLeft.x(), topRight.x(), bottomRight.x(), bottomLeft.x());
        float bottom = max(topLeft.y(), topRight.y(), bottomRight.y(), bottomLeft.y());
        return new ScreenRect(left, top, Math.max(1.0f, right - left), Math.max(1.0f, bottom - top));
    }

    public ScreenQuad projectPageQuad(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int localX,
            int localY,
            int localWidth,
            int localHeight
    ) {
        PageSurfaceBounds bounds = pageSurfaceBounds(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide);
        return new ScreenQuad(
                projectPagePoint(bounds, localX, localY),
                projectPagePoint(bounds, localX + localWidth, localY),
                projectPagePoint(bounds, localX + localWidth, localY + localHeight),
                projectPagePoint(bounds, localX, localY + localHeight)
        );
    }

    public void renderBook(
            GuiGraphics graphics,
            BookLayout layout,
            BookSpread spread,
            int spreadIndex,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide focusedPageSide,
            java.util.List<PageInteractiveNode> leftInteractiveNodes,
            java.util.List<PageInteractiveNode> rightInteractiveNodes,
            PageInteractiveNode leftHoveredInteractiveNode,
            PageInteractiveNode rightHoveredInteractiveNode,
            int mouseX,
            int mouseY,
            float textAlpha,
            float partialTick,
            boolean closedHovered,
            float inspectYaw,
            float inspectPitch
    ) {
        BookPageTexturePipeline.TextureSet textures = pageTexturePipeline.textureSetFor(
                spread,
                spreadIndex,
                textAlpha,
                projectionProgress,
                focusedPageSide,
                leftInteractiveNodes,
                rightInteractiveNodes,
                leftHoveredInteractiveNode,
                rightHoveredInteractiveNode
        );
        bookAnimatable.setAnimState(state);
        bookAnimatable.setTextureLocation(textures.baseTexture());
        bookAnimatable.setMagicTextTextureLocations(textures.magicLeftTexture(), textures.magicRightTexture());
        PresentationTransform presentation = presentationTransform(layout, state, animationProgress, projectionProgress, projectionFocusOffset, inspectYaw);

        graphics.flush();
        RenderSystem.enableDepthTest();

        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(presentation.centerX(), presentation.centerY(), GUI_Z);
        poseStack.scale(
                presentation.scaleX(),
                presentation.scaleY(),
                presentation.scaleZ()
        );

// Keep the base orientation that makes the open book correct.
// Only bias the yaw while the book is closed / closing / opening.
        poseStack.mulPose(Axis.XP.rotationDegrees(MODEL_X_ROTATION));
        poseStack.mulPose(Axis.YP.rotationDegrees(presentation.presentationYaw()));
        poseStack.mulPose(Axis.YP.rotationDegrees(presentation.inspectYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(presentation.inspectPitch()));
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

    public void prewarmPageTextures(BookSpread spread, int spreadIndex) {
        pageTexturePipeline.textureSetFor(
                spread,
                spreadIndex,
                0.0f,
                0.0f,
                null,
                List.of(),
                List.of(),
                null,
                null
        );
    }

    public boolean isClosedBookHovered(BookLayout layout, int mouseX, int mouseY) {
        int centerX = Math.round(layout.bookX() + (layout.bookWidth() / 2.0f) + CLOSED_HITBOX_X_OFFSET);
        int halfWidth = Math.round(layout.bookWidth() * 0.22f);
        int halfHeight = Math.round(layout.bookHeight() * 0.45f);
        int centerY = layout.bookY() + (layout.bookHeight() / 2);
        return mouseX >= centerX - halfWidth
                && mouseX <= centerX + halfWidth
                && mouseY >= centerY - halfHeight
                && mouseY <= centerY + halfHeight;
    }

    public static float projectionVerticalOffset(float projectionProgress) {
        return PROJECTION_BOOK_Y_OFFSET * clamp(projectionProgress, 0.0f, 1.0f);
    }

    private static float closednessFor(BookAnimState state, float animationProgress) {
        float easedProgress = BookAnimationSpec.presentationProgress(state, animationProgress);
        return switch (state) {
            case ARRIVING -> 1.0f;
            case CLOSED -> 1.0f;
            case OPENING -> 1.0f - easedProgress;
            case CLOSING -> easedProgress;
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

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private PageSurfaceBounds projectedPageSurface(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide
    ) {
        EnchiridionBookRenderer.CapturedPageSurface captured = BOOK_RENDERER.capturedPageSurface(pageSide);
        if (captured != null) {
            float left = min(captured.topLeft().x(), captured.topRight().x(), captured.bottomRight().x(), captured.bottomLeft().x());
            float top = min(captured.topLeft().y(), captured.topRight().y(), captured.bottomRight().y(), captured.bottomLeft().y());
            float right = max(captured.topLeft().x(), captured.topRight().x(), captured.bottomRight().x(), captured.bottomLeft().x());
            float bottom = max(captured.topLeft().y(), captured.topRight().y(), captured.bottomRight().y(), captured.bottomLeft().y());
            return new PageSurfaceBounds(
                    pageSide,
                    captured.topLeft(),
                    captured.topRight(),
                    captured.bottomRight(),
                    captured.bottomLeft(),
                    left,
                    top,
                    Math.max(1.0f, right - left),
                    Math.max(1.0f, bottom - top),
                    captured.localWidth(),
                    captured.localHeight()
            );
        }
        BookPageTexturePipeline.RenderRegionSize renderRegion = BookPageTexturePipeline.renderRegionSize(pageSide);
        float pageWidth = renderRegion.width();
        float pageHeight = renderRegion.height();
        ScreenPoint topLeft = projectPagePoint(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide, 0.0f, 0.0f);
        ScreenPoint topRight = projectPagePoint(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide, pageWidth, 0.0f);
        ScreenPoint bottomRight = projectPagePoint(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide, pageWidth, pageHeight);
        ScreenPoint bottomLeft = projectPagePoint(layout, state, animationProgress, projectionProgress, projectionFocusOffset, pageSide, 0.0f, pageHeight);
        float left = min(topLeft.x(), topRight.x(), bottomRight.x(), bottomLeft.x());
        float top = min(topLeft.y(), topRight.y(), bottomRight.y(), bottomLeft.y());
        float right = max(topLeft.x(), topRight.x(), bottomRight.x(), bottomLeft.x());
        float bottom = max(topLeft.y(), topRight.y(), bottomRight.y(), bottomLeft.y());
        return new PageSurfaceBounds(
                pageSide,
                topLeft,
                topRight,
                bottomRight,
                bottomLeft,
                left,
                top,
                Math.max(1.0f, right - left),
                Math.max(1.0f, bottom - top),
                pageWidth,
                pageHeight
        );
    }

    public ScreenPoint projectPagePoint(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            float localX,
            float localY
    ) {
        PresentationTransform presentation = presentationTransform(layout, state, animationProgress, projectionProgress, projectionFocusOffset, currentInspectYaw);
        PageSurfaceMapping surface = pageSurfaceMapping(pageSide);
        Vector3f point = surface.modelPointForLocal(localX, localY);
        point.mul(presentation.scaleX(), presentation.scaleY(), presentation.scaleZ());
        point.rotateX((float) Math.toRadians(MODEL_X_ROTATION));
        point.rotateY((float) Math.toRadians(presentation.presentationYaw()));
        point.rotateY((float) Math.toRadians(presentation.inspectYaw()));
        point.rotateX((float) Math.toRadians(presentation.inspectPitch()));
        point.rotateX((float) Math.toRadians(currentTiltPitch));
        point.rotateY((float) Math.toRadians(currentTiltYaw));
        float perspective = perspectiveScale(GUI_Z + point.z);
        return new ScreenPoint(
                presentation.centerX() + (point.x * perspective),
                presentation.centerY() + (point.y * perspective)
        );
    }

    private PageSurfaceMapping pageSurfaceMapping(BookPageSide pageSide) {
        BookPageTexturePipeline.RenderRegionSize renderRegion = BookPageTexturePipeline.renderRegionSize(pageSide);
        return switch (pageSide) {
            case LEFT -> new PageSurfaceMapping(
                    pageSide,
                    renderRegion.width(),
                    renderRegion.height(),
                    geoToModel(2.0f, 0.0f, 0.5f + PAGE_SURFACE_DEPTH),
                    70.0f * MODEL_UNIT,
                    90.0f * MODEL_UNIT,
                    geoToModel(2.0f, 45.0f, 1.0f),
                    0.3f
            );
            case RIGHT -> new PageSurfaceMapping(
                    pageSide,
                    renderRegion.width(),
                    renderRegion.height(),
                    geoToModel(2.0f, 0.0f, -1.5f + PAGE_SURFACE_DEPTH),
                    70.0f * MODEL_UNIT,
                    90.0f * MODEL_UNIT,
                    geoToModel(2.0f, 45.0f, -1.0f),
                    -0.3f
            );
        };
    }

    private static Vector3f geoToModel(float x, float y, float z) {
        return new Vector3f(
                (x - BOOK_PIVOT_X) * MODEL_UNIT,
                (y - BOOK_PIVOT_Y) * MODEL_UNIT,
                (z - BOOK_PIVOT_Z) * MODEL_UNIT
        );
    }

    private PresentationTransform presentationTransform(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            float inspectYaw
    ) {
        float closedness = closednessFor(state, animationProgress);
        float centerOffsetX = OPEN_CENTER_X_OFFSET + ((CLOSED_CENTER_X_OFFSET - OPEN_CENTER_X_OFFSET) * closedness);
        float wrappedInspectYaw = wrapDegrees(inspectYaw);
        float backFacingShift = Math.abs(wrappedInspectYaw) > 90.0f ? layout.bookWidth() * closedness : 0.0f;
        float centerX = layout.bookX() + (layout.bookWidth() / 2.0f) + centerOffsetX + backFacingShift + projectionFocusOffset;
        float centerY = layout.bookY() + (layout.bookHeight() / 2.0f) + projectionVerticalOffset(projectionProgress) + MODEL_Y_OFFSET;
        float projectionScale = 1.0f + ((PROJECTION_ZOOM_SCALE - 1.0f) * clamp(projectionProgress, 0.0f, 1.0f));
        float baseScale = MODEL_SCALE * currentClosedHoverScale * projectionScale;
        return new PresentationTransform(
                centerX,
                centerY,
                baseScale * MODEL_MIRROR_X,
                baseScale,
                baseScale,
                MODEL_Y_ROTATION + (CLOSED_PRESENTATION_YAW * closedness),
                currentInspectYaw,
                currentInspectPitch
        );
    }

    private ScreenPoint projectPagePoint(PageSurfaceBounds bounds, float localX, float localY) {
        float u = bounds.localWidth() <= 0.0f ? 0.0f : clamp(localX / bounds.localWidth(), 0.0f, 1.0f);
        float v = bounds.localHeight() <= 0.0f ? 0.0f : clamp(localY / bounds.localHeight(), 0.0f, 1.0f);
        float topX = lerp(bounds.topLeft().x(), bounds.topRight().x(), u);
        float topY = lerp(bounds.topLeft().y(), bounds.topRight().y(), u);
        float bottomX = lerp(bounds.bottomLeft().x(), bounds.bottomRight().x(), u);
        float bottomY = lerp(bounds.bottomLeft().y(), bounds.bottomRight().y(), u);
        return new ScreenPoint(lerp(topX, bottomX, v), lerp(topY, bottomY, v));
    }

    private PageLocalPoint resolvePointInTriangle(
            PageSurfaceBounds bounds,
            ScreenPoint point,
            ScreenPoint a,
            ScreenPoint b,
            ScreenPoint c,
            float localAX,
            float localAY,
            float localBX,
            float localBY,
            float localCX,
            float localCY
    ) {
        Barycentric barycentric = barycentric(point, a, b, c);
        if (barycentric == null || !barycentric.isInside()) {
            return null;
        }
        float localX = (barycentric.alpha() * localAX) + (barycentric.beta() * localBX) + (barycentric.gamma() * localCX);
        float localY = (barycentric.alpha() * localAY) + (barycentric.beta() * localBY) + (barycentric.gamma() * localCY);
        return new PageLocalPoint(bounds, localX, localY);
    }

    private static Barycentric barycentric(ScreenPoint p, ScreenPoint a, ScreenPoint b, ScreenPoint c) {
        float denominator = ((b.y() - c.y()) * (a.x() - c.x())) + ((c.x() - b.x()) * (a.y() - c.y()));
        if (Math.abs(denominator) < 0.0001f) {
            return null;
        }
        float alpha = (((b.y() - c.y()) * (p.x() - c.x())) + ((c.x() - b.x()) * (p.y() - c.y()))) / denominator;
        float beta = (((c.y() - a.y()) * (p.x() - c.x())) + ((a.x() - c.x()) * (p.y() - c.y()))) / denominator;
        float gamma = 1.0f - alpha - beta;
        return new Barycentric(alpha, beta, gamma);
    }

    private static float perspectiveScale(float z) {
        return PAGE_PICK_CAMERA_DEPTH / Math.max(1.0f, PAGE_PICK_CAMERA_DEPTH - z);
    }

    private static float lerp(float from, float to, float progress) {
        return from + ((to - from) * progress);
    }

    private static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public record PageSurfaceBounds(
            BookPageSide pageSide,
            ScreenPoint topLeft,
            ScreenPoint topRight,
            ScreenPoint bottomRight,
            ScreenPoint bottomLeft,
            float screenX,
            float screenY,
            float screenWidth,
            float screenHeight,
            float localWidth,
            float localHeight
    ) {}

    public record PageLocalPoint(PageSurfaceBounds bounds, float localX, float localY) {}

    public record ScreenRect(float x, float y, float width, float height) {}

    public record ScreenQuad(
            ScreenPoint topLeft,
            ScreenPoint topRight,
            ScreenPoint bottomRight,
            ScreenPoint bottomLeft
    ) {}

    public record ScreenPoint(float x, float y) {}

    private record PresentationTransform(
            float centerX,
            float centerY,
            float scaleX,
            float scaleY,
            float scaleZ,
            float presentationYaw,
            float inspectYaw,
            float inspectPitch
    ) {}

    private record PageSurfaceMapping(
            BookPageSide pageSide,
            float localWidth,
            float localHeight,
            Vector3f topLeftOrigin,
            float modelWidth,
            float modelHeight,
            Vector3f pivot,
            float localPitchDegrees
    ) {
        private Vector3f modelPointForLocal(float localX, float localY) {
            float u = localWidth <= 0.0f ? 0.0f : clamp(localX / localWidth, 0.0f, 1.0f);
            float v = localHeight <= 0.0f ? 0.0f : clamp(localY / localHeight, 0.0f, 1.0f);
            Vector3f point = new Vector3f(
                    topLeftOrigin.x + (modelWidth * u),
                    topLeftOrigin.y + (modelHeight * v),
                    topLeftOrigin.z
            );
            point.sub(pivot);
            point.rotateX((float) Math.toRadians(localPitchDegrees));
            point.add(pivot);
            return point;
        }
    }

    private record Barycentric(float alpha, float beta, float gamma) {
        private boolean isInside() {
            float epsilon = 0.001f;
            return alpha >= -epsilon && beta >= -epsilon && gamma >= -epsilon;
        }
    }
}
