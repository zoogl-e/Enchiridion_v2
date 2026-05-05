package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimationSpec;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.levelrpg.archetype.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.archetype.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.projection.SkillTreeProjectionData;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.ui.BookInteractionGeometry;
import net.zoogle.enchiridion.client.ui.BookLayout;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * v2 internal scene renderer backed by GeckoLib.
 * This stays inside Enchiridion; external books still provide content only.
 */
public final class BookSceneRenderer implements BookInteractionGeometry {
    public static final float PROJECTION_BOOK_Y_OFFSET = 200.0f;
    public static final float PROJECTION_FOCUS_X_OFFSET = 200.0f;
    public static final float PROJECTION_ZOOM_SCALE = 2.5f;

    private static final float REFERENCE_MODEL_SCALE = 30.0f;
    static final float GUI_Z = 180.0f;
    private static final float MODEL_MIRROR_X = -1.0f;
    static final float MODEL_SCALE = 33.0f;
    private static final float CLOSED_HOVER_SCALE = 1.035f;
    private static final float CLOSED_CENTER_X_OFFSET = -52.0f * (MODEL_SCALE / REFERENCE_MODEL_SCALE);
    private static final float OPEN_CENTER_X_OFFSET = 18.0f * (MODEL_SCALE / REFERENCE_MODEL_SCALE);
    private static final float CLOSED_HITBOX_X_OFFSET = 0.0f;
    static final float MODEL_Y_OFFSET = 100.0f;
    private static final float MODEL_X_ROTATION = 180.0f;
    private static final float MODEL_Y_ROTATION = 0.0f;
    private static final float MAX_TILT_YAW_DEGREES = 8.0f;
    private static final float MAX_TILT_PITCH_DEGREES = 5.0f;
    private static final float MAX_INSPECT_PITCH_DEGREES = 28.0f;
    private static final float CLOSED_PRESENTATION_YAW = 0f;
    static final float PAGE_PICK_CAMERA_DEPTH = 1600.0f;
    private static final float MODEL_UNIT = 1.0f / 16.0f;
    private static final float BOOK_PIVOT_X = 0.0f;
    private static final float BOOK_PIVOT_Y = 45.0f;
    private static final float BOOK_PIVOT_Z = 1.5f;
    private static final float PAGE_SURFACE_DEPTH = 0.05f;
    private static final float REEL_SCENE_Z = GUI_Z + 24.0f;
    static final float SKILLTREE_BOOK_SCALE = 0.74f;
    static final float SKILLTREE_BOOK_Y_OFFSET = 56.0f;
    private static final float SKILLTREE_BOOK_X_OFFSET = 0.0f;
    /** Extra screen-space Y (positive = downward) while the archetype reel is open. */
    private static final float REEL_BOOK_EXTRA_DOWN = 68.0f;

    private static final EnchiridionBookRenderer BOOK_RENDERER = new EnchiridionBookRenderer();
    private final EnchiridionBookAnimatable bookAnimatable = new EnchiridionBookAnimatable();
    private final BookPageTexturePipeline pageTexturePipeline = new BookPageTexturePipeline();
    private final ArchetypeReelSceneRenderer archetypeReelRenderer = new ArchetypeReelSceneRenderer();
    private final SkillTreeProjectionRenderer skillTreeProjectionRenderer = new SkillTreeProjectionRenderer();
    private final ReelPresentationProfile reelPresentationProfile = ReelPresentationProfile.defaultProfile(REEL_SCENE_Z);
    private float currentTiltYaw;
    private float currentTiltPitch;
    private float currentClosedHoverScale = 1.0f;
    private float currentInspectYaw;
    private float currentInspectPitch;
    private @Nullable ArchetypeReelState presentationReelState;
    private int presentationViewportWidth;
    private int presentationViewportHeight;
    private @Nullable ReelFrameSnapshot cachedReelFrameSnapshot;
    private @Nullable BookLayout cachedReelFrameLayout;
    private @Nullable ArchetypeReelState cachedReelFrameState;

    /**
     * Per-frame presentation context: reel state (when active) and GUI viewport for centered reel placement.
     * Call from the book screen at the start of {@code render} and {@link #endPresentationFrame()} in {@code finally}.
     */
    public void beginPresentationFrame(@Nullable ArchetypeReelState archetypeReelState, int viewportWidth, int viewportHeight) {
        this.presentationReelState = archetypeReelState != null && archetypeReelState.active() ? archetypeReelState : null;
        this.presentationViewportWidth = viewportWidth;
        this.presentationViewportHeight = viewportHeight;
        this.cachedReelFrameSnapshot = null;
        this.cachedReelFrameLayout = null;
        this.cachedReelFrameState = null;
    }

    public void endPresentationFrame() {
        this.presentationReelState = null;
        this.presentationViewportWidth = 0;
        this.presentationViewportHeight = 0;
        this.cachedReelFrameSnapshot = null;
        this.cachedReelFrameLayout = null;
        this.cachedReelFrameState = null;
    }

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
            return toCanvasLocalPoint(point, pageSide);
        }
        PageLocalPoint point2 = resolvePointInTriangle(
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
        return point2 == null ? null : toCanvasLocalPoint(point2, pageSide);
    }

    /**
     * The captured left page surface has localX=0 at the spine (screen-right) because the
     * GeckoLib bone UVs are oriented with u=0 at the spine side. The page texture is rendered
     * with mirrorHorizontally=true so that canvas x=0 appears at the outer/left edge on screen.
     * This converts raw texture-space localX back to canvas-space localX (x=0 = outer left edge).
     */
    private static PageLocalPoint toCanvasLocalPoint(PageLocalPoint raw, BookPageSide pageSide) {
        if (pageSide != BookPageSide.LEFT) {
            return raw;
        }
        return new PageLocalPoint(raw.bounds(), raw.bounds().localWidth() - raw.localX(), raw.localY());
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
        if (pageSide == BookPageSide.LEFT) {
            // Canvas x=0 is the outer/left edge (screen-left), but the captured surface has
            // localX=0 at the spine (screen-right). Convert canvas coords to texture coords
            // before projecting: texX = totalWidth - canvasX. The quad corners become:
            //   topLeft  (screen-left) = canvas left edge  → tex totalW - localX
            //   topRight (screen-right) = canvas right edge → tex totalW - localX - localWidth
            int totalW = Math.round(bounds.localWidth());
            int texRight = totalW - localX;
            int texLeft = totalW - localX - localWidth;
            return new ScreenQuad(
                    projectPagePoint(bounds, texRight, localY),
                    projectPagePoint(bounds, texLeft, localY),
                    projectPagePoint(bounds, texLeft, localY + localHeight),
                    projectPagePoint(bounds, texRight, localY + localHeight)
            );
        }
        return new ScreenQuad(
                projectPagePoint(bounds, localX, localY),
                projectPagePoint(bounds, localX + localWidth, localY),
                projectPagePoint(bounds, localX + localWidth, localY + localHeight),
                projectPagePoint(bounds, localX, localY + localHeight)
        );
    }

    public ScreenRect projectTrackedRegionRect(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookTrackedRegion.Anchor anchor
    ) {
        ScreenQuad quad = projectTrackedRegionQuad(layout, state, animationProgress, projectionProgress, projectionFocusOffset, anchor);
        float left = min(quad.topLeft().x(), quad.topRight().x(), quad.bottomRight().x(), quad.bottomLeft().x());
        float top = min(quad.topLeft().y(), quad.topRight().y(), quad.bottomRight().y(), quad.bottomLeft().y());
        float right = max(quad.topLeft().x(), quad.topRight().x(), quad.bottomRight().x(), quad.bottomLeft().x());
        float bottom = max(quad.topLeft().y(), quad.topRight().y(), quad.bottomRight().y(), quad.bottomLeft().y());
        return new ScreenRect(left, top, Math.max(1.0f, right - left), Math.max(1.0f, bottom - top));
    }

    public ScreenQuad projectTrackedRegionQuad(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookTrackedRegion.Anchor anchor
    ) {
        EnchiridionBookRenderer.CapturedSurface surface = EnchiridionBookRenderer.capturedArchetypeCardSurface();
        if (surface != null) {
            return new ScreenQuad(surface.topLeft(), surface.topRight(), surface.bottomRight(), surface.bottomLeft());
        }
        PageSurfaceBounds fallback = pageSurfaceBounds(layout, state, animationProgress, projectionProgress, projectionFocusOffset, BookPageSide.LEFT);
        return new ScreenQuad(fallback.topLeft(), fallback.topRight(), fallback.bottomRight(), fallback.bottomLeft());
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
            BookFrontCoverCardState frontCoverCardState,
            ArchetypeReelState archetypeReelState,
            boolean closedHovered,
            float inspectYaw,
            float inspectPitch
    ) {
        try {
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
            bookAnimatable.setAnimProgress(animationProgress);
            bookAnimatable.setAnimState(state);
            ResourceLocation coverTexture = frontCoverCardState != null && frontCoverCardState.boundArchetypeId() == null
                    ? EnchiridionBookGeoModel.BLANK_FRONT_TEXTURE
                    : textures.baseTexture();
            bookAnimatable.setTextureLocation(coverTexture);
            bookAnimatable.setMagicTextTextureLocations(textures.magicLeftTexture(), textures.magicRightTexture());
            bookAnimatable.setFrontCoverCardState(frontCoverCardState);
            bookAnimatable.setReadableSurfaceTargets(leftSurfaceTargetFor(state), rightSurfaceTargetFor(state));
            PresentationTransform presentation = presentationTransform(layout, state, animationProgress, projectionProgress, projectionFocusOffset, inspectYaw, partialTick);
            ReelFrameSnapshot reelFrame = reelFrameSnapshot(layout, archetypeReelState);

            graphics.flush();
            RenderSystem.enableDepthTest();

            var poseStack = graphics.pose();
            if (archetypeReelState != null && archetypeReelState.active() && reelFrame != null) {
                poseStack.pushPose();
                poseStack.translate(reelFrame.presentation().centerX(), reelFrame.presentation().centerY(), reelFrame.presentation().centerZ());
                archetypeReelRenderer.render(
                        poseStack,
                        graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT,
                        reelFrame.presentation().scale(),
                        ArchetypeReelSceneRenderer.RenderLayer.BACK,
                        archetypeReelState,
                        reelFrame.layout()
                );
                poseStack.popPose();
            }
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
            if (archetypeReelState != null && archetypeReelState.active() && reelFrame != null) {
                poseStack.pushPose();
                poseStack.translate(reelFrame.presentation().centerX(), reelFrame.presentation().centerY(), reelFrame.presentation().centerZ());
                archetypeReelRenderer.render(
                        poseStack,
                        graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT,
                        reelFrame.presentation().scale(),
                        ArchetypeReelSceneRenderer.RenderLayer.FRONT,
                        archetypeReelState,
                        reelFrame.layout()
                );
                poseStack.popPose();
            }
        } finally {
            graphics.flush();
            restoreExpectedRenderState();
        }
    }

    public void renderSkillTreeProjection(
            GuiGraphics graphics,
            BookLayout layout,
            String skillName,
            SkillTreeProjectionData projectionData,
            float alpha,
            int ticksActive,
            float rotationYawDeg,
            float rotationPitchDeg,
            float zoomScale,
            float cameraOffsetX,
            float cameraOffsetY,
            int selectedNodeIndex,
            float holdProgress,
            float celebrationStrength
    ) {
        skillTreeProjectionRenderer.renderSkillTreeProjection(
                graphics, layout, skillName, projectionData, alpha, ticksActive, rotationYawDeg, rotationPitchDeg,
                zoomScale, cameraOffsetX, cameraOffsetY, selectedNodeIndex, holdProgress, celebrationStrength
        );
    }

    public SkillProjectionNodeHit pickSkillProjectionNode(
            BookLayout layout,
            String skillName,
            SkillTreeProjectionData projectionData,
            float alpha,
            int ticksActive,
            float rotationYawDeg,
            float rotationPitchDeg,
            float zoomScale,
            float cameraOffsetX,
            float cameraOffsetY,
            double mouseX,
            double mouseY
    ) {
        return skillTreeProjectionRenderer.pickSkillProjectionNode(
                layout, skillName, projectionData, alpha, ticksActive, rotationYawDeg, rotationPitchDeg,
                zoomScale, cameraOffsetX, cameraOffsetY, mouseX, mouseY
        );
    }

    public SkillProjectionNodeDescriptor describeSkillProjectionNode(SkillTreeProjectionData projectionData, int nodeIndex) {
        if (projectionData == null) {
            return new SkillProjectionNodeDescriptor("", "", "", "");
        }
        SkillTreeProjectionData.ProjectedNode node = projectionData.nodeByIndex(nodeIndex);
        if (node == null) {
            return new SkillProjectionNodeDescriptor("", "", "", "");
        }
        String title = node.title().isBlank() ? node.id() : node.title();
        String subtitle = node.branch().isBlank() ? node.status() : node.branch();
        String tier = "Req Lv " + node.requiredLevel();
        String requirement = node.missingRequirements().isEmpty()
                ? (node.available() ? "Ready to inscribe." : "No missing prerequisites.")
                : "Missing: " + String.join(", ", node.missingRequirements());
        return new SkillProjectionNodeDescriptor(title, subtitle, tier, requirement);
    }

    public ScreenPoint projectSkillProjectionNodeScreenPoint(
            BookLayout layout,
            String skillName,
            SkillTreeProjectionData projectionData,
            float alpha,
            int ticksActive,
            float rotationYawDeg,
            float rotationPitchDeg,
            float zoomScale,
            float cameraOffsetX,
            float cameraOffsetY,
            int nodeIndex
    ) {
        return skillTreeProjectionRenderer.projectSkillProjectionNodeScreenPoint(
                layout, skillName, projectionData, alpha, ticksActive, rotationYawDeg, rotationPitchDeg,
                zoomScale, cameraOffsetX, cameraOffsetY, nodeIndex
        );
    }

    public Integer hoveredArchetypeReelCardIndex(BookLayout layout, ArchetypeReelState state, double mouseX, double mouseY) {
        ReelFrameSnapshot snapshot = reelFrameSnapshot(layout, state);
        if (snapshot == null) {
            return null;
        }
        java.util.List<ReelCardProjection> interactive = snapshot.projections().stream()
                .filter(projection -> !projection.detached())
                .filter(projection -> projection.renderLayer() == ArchetypeReelSceneRenderer.RenderLayer.FRONT)
                .toList();
        return HitTestEngine.pickTopMostCardIndex(interactive, (float) mouseX, (float) mouseY);
    }

    public ScreenRect focusedArchetypeReelCardRect(BookLayout layout, ArchetypeReelState state) {
        ReelFrameSnapshot snapshot = reelFrameSnapshot(layout, state);
        if (snapshot == null) {
            return null;
        }
        ReelCardProjection focusedCard = snapshot.focusedProjection();
        if (focusedCard == null) {
            return null;
        }
        return quadScreenBounds(focusedCard.quad());
    }

    public void renderArchetypeReelCardTitleOverlays(GuiGraphics graphics, Font font, BookLayout layout, ArchetypeReelState state) {
        if (layout == null || state == null || !state.active()) {
            return;
        }
        ReelFrameSnapshot snapshot = reelFrameSnapshot(layout, state);
        if (snapshot == null || snapshot.projections().isEmpty()) {
            return;
        }
        float alpha = state.globalPresentationAlpha();
        int alphaByte = Math.min(255, Math.round(alpha * 255.0f));
        int color = (alphaByte << 24) | 0x002A180F;
        for (ReelCardProjection projection : snapshot.projections()) {
            if (projection.detached()) {
                continue;
            }
            JournalArchetypeChoice choice = choiceForIndex(snapshot.layout(), projection.index());
            if (choice == null) {
                continue;
            }
            ScreenRect bounds = quadScreenBounds(projection.quad());
            int maxWidth = Math.max(24, (int) bounds.width() - 6);
            String title = truncateToWidth(font, choice.title(), maxWidth);
            int textWidth = font.width(title);
            int drawX = Math.round(bounds.x() + (bounds.width() - textWidth) * 0.5f);
            // Place inside the projected card face (top band), not floating above the quad.
            int drawY = Math.round(bounds.y() + Math.max(4.0f, bounds.height() * 0.07f));
            graphics.drawString(font, title, drawX, drawY, color, false);
        }
    }

    private static @Nullable JournalArchetypeChoice choiceForIndex(ArchetypeReelSceneRenderer.ReelLayout layout, int index) {
        for (ArchetypeReelSceneRenderer.PlacedCard card : layout.cards()) {
            if (card.index() == index) {
                return card.choice();
            }
        }
        return null;
    }

    private static ScreenRect quadScreenBounds(ScreenQuad quad) {
        float left = min(quad.topLeft().x(), quad.topRight().x(), quad.bottomRight().x(), quad.bottomLeft().x());
        float top = min(quad.topLeft().y(), quad.topRight().y(), quad.bottomRight().y(), quad.bottomLeft().y());
        float right = max(quad.topLeft().x(), quad.topRight().x(), quad.bottomRight().x(), quad.bottomLeft().x());
        float bottom = max(quad.topLeft().y(), quad.topRight().y(), quad.bottomRight().y(), quad.bottomLeft().y());
        return new ScreenRect(left, top, Math.max(1.0f, right - left), Math.max(1.0f, bottom - top));
    }

    /**
     * Left screen X of the reel card that sits visually to the right of the focused card (for panel width clamping).
     */
    public @Nullable Float nextReelCardLeftEdgeToRightOfFocus(BookLayout layout, ArchetypeReelState state) {
        ReelFrameSnapshot snapshot = reelFrameSnapshot(layout, state);
        if (snapshot == null || snapshot.focusedProjection() == null) {
            return null;
        }
        ScreenRect focus = quadScreenBounds(snapshot.focusedProjection().quad());
        float focusCenterX = focus.x() + focus.width() * 0.5f;
        Float bestLeft = null;
        for (ReelCardProjection projection : snapshot.projections()) {
            if (projection.index() == state.focusedIndex()) {
                continue;
            }
            ScreenRect r = quadScreenBounds(projection.quad());
            float cx = r.x() + r.width() * 0.5f;
            if (cx > focusCenterX + 24.0f) {
                bestLeft = bestLeft == null ? r.x() : Math.min(bestLeft, r.x());
            }
        }
        return bestLeft;
    }

    private static String truncateToWidth(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        String trimmed = text;
        while (trimmed.length() > 1 && font.width(trimmed) > budget) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
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
            case OPENING, OPENING_FRONT, OPENING_BACK -> 1.0f - easedProgress;
            case CLOSING, CLOSING_FRONT, CLOSING_BACK -> easedProgress;
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

    /**
     * Conservative render-state reset to detect/contain state leakage from custom 3D/Geo rendering.
     */
    private static void restoreExpectedRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableScissor();
    }

    private static boolean isSkilltreeState(BookAnimState state) {
        return switch (state) {
            case IDLE_SKILLTREE, IDLE_FRONT_SKILLTREE, FLIPPING_NEXT_SKILLTREE, FLIPPING_PREV_SKILLTREE, RIFFLING_NEXT_SKILLTREE, RIFFLING_PREV_SKILLTREE -> true;
            default -> false;
        };
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

    private static EnchiridionBookAnimatable.ReadableSurfaceTarget leftSurfaceTargetFor(BookAnimState state) {
        return switch (state) {
            case IDLE_FRONT, FLIPPING_FRONT, FLIPPING_FRONT_TO_ORIGIN -> new EnchiridionBookAnimatable.ReadableSurfaceTarget(
                    EnchiridionBookAnimatable.MAGIC_TEXT_FRONT_LEFT_BONE,
                    false
            );
            case IDLE_BACK, FLIPPING_BACK, FLIPPING_BACK_TO_ORIGIN,
                    IDLE_SKILLTREE, IDLE_FRONT_SKILLTREE, FLIPPING_NEXT_SKILLTREE, FLIPPING_PREV_SKILLTREE, RIFFLING_NEXT_SKILLTREE, RIFFLING_PREV_SKILLTREE,
                    CLOSED, CLOSING, ARRIVING -> EnchiridionBookAnimatable.ReadableSurfaceTarget.none();
            default -> new EnchiridionBookAnimatable.ReadableSurfaceTarget(
                    EnchiridionBookAnimatable.MAGIC_TEXT_LEFT_BONE,
                    true
            );
        };
    }

    private static EnchiridionBookAnimatable.ReadableSurfaceTarget rightSurfaceTargetFor(BookAnimState state) {
        return switch (state) {
            // Render the legacy title page texture on the right page bone during front cover state.
            case IDLE_FRONT -> new EnchiridionBookAnimatable.ReadableSurfaceTarget(
                    EnchiridionBookAnimatable.MAGIC_TEXT_RIGHT_BONE,
                    true
            );
            case FLIPPING_FRONT, FLIPPING_FRONT_TO_ORIGIN,
                    IDLE_BACK, FLIPPING_BACK, FLIPPING_BACK_TO_ORIGIN,
                    IDLE_SKILLTREE, IDLE_FRONT_SKILLTREE, FLIPPING_NEXT_SKILLTREE, FLIPPING_PREV_SKILLTREE, RIFFLING_NEXT_SKILLTREE, RIFFLING_PREV_SKILLTREE,
                    CLOSED, CLOSING, ARRIVING -> EnchiridionBookAnimatable.ReadableSurfaceTarget.none();
            default -> new EnchiridionBookAnimatable.ReadableSurfaceTarget(
                    EnchiridionBookAnimatable.MAGIC_TEXT_RIGHT_BONE,
                    true
            );
        };
    }

    private PageSurfaceBounds projectedPageSurface(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide
    ) {
        EnchiridionBookRenderer.CapturedPageSurface captured = EnchiridionBookRenderer.capturedPageSurface(pageSide);
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
        PresentationTransform presentation = presentationTransform(layout, state, animationProgress, projectionProgress, projectionFocusOffset, currentInspectYaw, 0.0f);
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
            float inspectYaw,
            float partialTick
    ) {
        float closedness = closednessFor(state, animationProgress);
        float centerOffsetX = OPEN_CENTER_X_OFFSET + ((CLOSED_CENTER_X_OFFSET - OPEN_CENTER_X_OFFSET) * closedness);
        float wrappedInspectYaw = wrapDegrees(inspectYaw);
        float backFacingShift = Math.abs(wrappedInspectYaw) > 90.0f ? layout.bookWidth() * closedness : 0.0f;
        boolean reelOpen = presentationReelState != null;
        float reelFrontBlend = presentationReelState == null ? 0.0f : clamp(presentationReelState.dragFrontBlend(), 0.0f, 1.0f);
        float skilltreeBlend = (!reelOpen && isSkilltreeState(state))
                ? 1.0f
                : (reelOpen ? (presentationReelState.reelBookLowerBlend(partialTick) * (1.0f - reelFrontBlend)) : 0.0f);
        float skilltreeOffsetX = SKILLTREE_BOOK_X_OFFSET * skilltreeBlend;
        float skilltreeOffsetY = SKILLTREE_BOOK_Y_OFFSET * skilltreeBlend;
        float reelLowerY = 0.0f;
        if (presentationReelState != null) {
            float lowerBlend = presentationReelState.reelBookLowerBlend(partialTick);
            // Ease from lowered skilltree-style reel posture back to front-cover posture as drag blend rises.
            lowerBlend *= (1.0f - (0.88f * reelFrontBlend));
            reelLowerY = REEL_BOOK_EXTRA_DOWN * lowerBlend;
        }
        float centerX = layout.bookX() + (layout.bookWidth() / 2.0f) + centerOffsetX + backFacingShift + projectionFocusOffset + skilltreeOffsetX;
        float centerY = layout.bookY() + (layout.bookHeight() / 2.0f) + projectionVerticalOffset(projectionProgress) + MODEL_Y_OFFSET + skilltreeOffsetY + reelLowerY;
        float projectionScale = 1.0f + ((PROJECTION_ZOOM_SCALE - 1.0f) * clamp(projectionProgress, 0.0f, 1.0f));
        float skilltreeScale = 1.0f - ((1.0f - SKILLTREE_BOOK_SCALE) * skilltreeBlend);
        float baseScale = MODEL_SCALE * currentClosedHoverScale * projectionScale * skilltreeScale;
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

    private ReelPresentationTransform reelPresentationTransform(BookLayout layout) {
        float reelCenterX;
        float reelCenterY;
        if (presentationReelState != null && presentationViewportWidth > 0 && presentationViewportHeight > 0) {
            // Focused card uses local origin ~0; placing reel root at horizontal screen center centers the hero card.
            reelCenterX = (presentationViewportWidth * 0.5f) + reelPresentationProfile.horizontalBiasPixels();
            float fromViewport = presentationViewportHeight * 0.50f;
            reelCenterY = fromViewport + reelPresentationProfile.anchorOffsetY();
            reelCenterY = Math.max(72.0f, Math.min(reelCenterY, presentationViewportHeight - 80.0f));
        } else {
            reelCenterX = layout.bookX() + (layout.bookWidth() / 2.0f);
            reelCenterY = layout.bookY() + (layout.bookHeight() / 2.0f) + reelPresentationProfile.anchorOffsetY();
        }
        return new ReelPresentationTransform(
                reelCenterX,
                reelCenterY,
                reelPresentationProfile.centerZ(),
                reelPresentationProfile.scale()
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

    private ReelCardProjection projectArchetypeReelCardQuad(ReelPresentationTransform presentation, ArchetypeReelSceneRenderer.PlacedCard card, ArchetypeReelState state) {
        return SurfaceProjectionService.projectArchetypeReelCardProjection(
                presentation,
                card,
                state.focusedTiltYaw(),
                state.focusedTiltPitch()
        );
    }

    private ReelFrameSnapshot reelFrameSnapshot(BookLayout layout, ArchetypeReelState state) {
        if (layout == null || state == null || !state.active()) {
            return null;
        }
        if (cachedReelFrameSnapshot != null && cachedReelFrameLayout == layout && cachedReelFrameState == state) {
            return cachedReelFrameSnapshot;
        }
        ReelPresentationTransform presentation = reelPresentationTransform(layout);
        ArchetypeReelSceneRenderer.ReelLayout reelLayout = archetypeReelRenderer.layout(state, presentation);
        java.util.List<ReelCardProjection> projections = new java.util.ArrayList<>(reelLayout.cards().size());
        ReelCardProjection focused = null;
        for (ArchetypeReelSceneRenderer.PlacedCard card : reelLayout.cards()) {
            ReelCardProjection projection = projectArchetypeReelCardQuad(presentation, card, state);
            projections.add(projection);
            if (card.focused()) {
                focused = projection;
            }
        }
        ReelFrameSnapshot snapshot = new ReelFrameSnapshot(presentation, reelLayout, java.util.List.copyOf(projections), focused);
        cachedReelFrameSnapshot = snapshot;
        cachedReelFrameLayout = layout;
        cachedReelFrameState = state;
        return snapshot;
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

    static float perspectiveScale(float z) {
        return PAGE_PICK_CAMERA_DEPTH / Math.max(1.0f, PAGE_PICK_CAMERA_DEPTH - z);
    }

    private static float lerp(float from, float to, float progress) {
        return from + ((to - from) * progress);
    }

    private static int scaleAlpha(int argb, float alphaScale) {
        int sourceA = (argb >>> 24) & 0xFF;
        int scaledA = Math.min(255, Math.max(0, Math.round(sourceA * clamp(alphaScale, 0.0f, 1.0f))));
        return (scaledA << 24) | (argb & 0x00FFFFFF);
    }

    private static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    private static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    public static float skillTreeProjectionCenterX(BookLayout layout, float cameraOffsetX) {
        return SkillTreeProjectionRenderer.skillTreeProjectionCenterX(layout, cameraOffsetX);
    }

    public static float skillTreeProjectionCenterY(BookLayout layout, float cameraOffsetY) {
        return SkillTreeProjectionRenderer.skillTreeProjectionCenterY(layout, cameraOffsetY);
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

    public record SkillProjectionNodeHit(int index, ScreenPoint screenPoint, boolean unlocked) {}

    public record SkillProjectionNodeDescriptor(String title, String subtitle, String tierLabel, String requirement) {}

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

    record ReelPresentationTransform(
            float centerX,
            float centerY,
            float centerZ,
            float scale
    ) {}

    public record ReelCardProjection(
            int index,
            ScreenQuad quad,
            float depthZ,
            boolean focused,
            boolean detached,
            ArchetypeReelSceneRenderer.RenderLayer renderLayer
    ) {}

    private record ReelFrameSnapshot(
            ReelPresentationTransform presentation,
            ArchetypeReelSceneRenderer.ReelLayout layout,
            java.util.List<ReelCardProjection> projections,
            ReelCardProjection focusedProjection
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
