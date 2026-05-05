package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.client.levelrpg.projection.SkillTreeProjectionData;
import net.zoogle.enchiridion.client.ui.BookLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dedicated renderer for the 3D Skill Tree projection overlay.
 * Handles node/edge projection, rendering, and hit-testing.
 */
public final class SkillTreeProjectionRenderer {

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
        if (layout == null || alpha <= 0.01f || projectionData == null || projectionData.nodes().isEmpty()) {
            return;
        }
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 260.0F);
        try {
            float progress = clamp(alpha, 0.0f, 1.0f);
            float centerX = skillTreeProjectionCenterX(layout, cameraOffsetX);
            float centerY = skillTreeProjectionCenterY(layout, cameraOffsetY);
            float baseScale = skillTreeProjectionScale(layout, projectionData.nodes().size(), zoomScale);
            float depthLift = skillTreeProjectionDepthLift(progress);
            float wobble = (float) Math.sin((ticksActive * 0.07f) + (skillName == null ? 0.0f : skillName.hashCode() * 0.01f));
            float pulse = 1.0f + (celebrationStrength * 0.46f);
            float treeRadiusX = projectionTreeRadiusX(projectionData, baseScale);
            float treeRadiusY = projectionTreeRadiusY(projectionData, baseScale);

            drawProjectionHalo(graphics, centerX, centerY, treeRadiusX * pulse, treeRadiusY * pulse, ticksActive, progress);

            List<ProjectedSkillPoint> projectedPoints = new ArrayList<>(projectionData.nodes().size());
            for (int index = 0; index < projectionData.nodes().size(); index++) {
                SkillTreeProjectionData.ProjectedNode node = projectionData.nodeByIndex(index);
                projectedPoints.add(projectSkillPoint(centerX, centerY, baseScale, depthLift, wobble, rotationYawDeg, rotationPitchDeg, node, index));
            }

            SkillTreeProjectionData.ProjectedNode selectedNode = projectionData.nodeByIndex(selectedNodeIndex);
            Set<String> selectedRequirements = requiredNodeIdSet(selectedNode);

            List<ProjectedEdgeRender> projectedEdges = new ArrayList<>(projectionData.edges().size());
            for (SkillTreeProjectionData.ProjectedEdge edge : projectionData.edges()) {
                int fromIndex = projectionData.indexOfNode(edge.fromNodeId());
                int toIndex = projectionData.indexOfNode(edge.toNodeId());
                if (fromIndex < 0 || toIndex < 0) {
                    continue;
                }
                ProjectedSkillPoint a = projectedPoints.get(fromIndex);
                ProjectedSkillPoint b = projectedPoints.get(toIndex);
                float lineDepth = (a.depthSortZ() + b.depthSortZ()) * 0.5f;
                float lineDepthAlpha = Math.min(a.depthAlpha(), b.depthAlpha());
                boolean requirementEdge = isRequirementEdge(edge, selectedNode, selectedRequirements);
                int baseColor = requirementEdge ? 0xFFFF6969 : 0xF1B5E7FF;
                int color = scaleAlpha(baseColor, progress * lineDepthAlpha);
                projectedEdges.add(new ProjectedEdgeRender(a, b, color, lineDepth));
            }
            projectedEdges.sort(Comparator.comparingDouble(ProjectedEdgeRender::depthSortZ));

            for (ProjectedEdgeRender edge : projectedEdges) {
                drawProjectedLine(graphics, edge.from(), edge.to(), edge.color());
            }

            List<Integer> drawOrder = new ArrayList<>(projectionData.nodes().size());
            for (int index = 0; index < projectionData.nodes().size(); index++) {
                drawOrder.add(index);
            }
            drawOrder.sort(Comparator.comparingDouble(index -> projectedPoints.get(index).depthSortZ()));

            for (int index : drawOrder) {
                SkillTreeProjectionData.ProjectedNode node = projectionData.nodeByIndex(index);
                ProjectedSkillPoint point = projectedPoints.get(index);
                drawProjectedNode(graphics, point, node.status(), node.type(), node.iconKey(), progress, zoomScale, holdProgress, celebrationStrength, index == selectedNodeIndex);
            }

            renderProjectionSparkles(
                    graphics,
                    centerX,
                    centerY,
                    Math.max(baseScale * 0.7f, treeRadiusX * 0.94f) * pulse,
                    Math.max(baseScale * 0.5f, treeRadiusY * 0.94f) * pulse,
                    depthLift,
                    ticksActive,
                    Math.min(1.0f, progress + 0.12f)
            );
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            // Note: restoreExpectedRenderState left in BookSceneRenderer as it's shared.
        }
    }

    public BookSceneRenderer.SkillProjectionNodeHit pickSkillProjectionNode(
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
        if (layout == null || alpha <= 0.01f || projectionData == null || projectionData.nodes().isEmpty()) {
            return null;
        }
        float progress = clamp(alpha, 0.0f, 1.0f);
        float centerX = skillTreeProjectionCenterX(layout, cameraOffsetX);
        float centerY = skillTreeProjectionCenterY(layout, cameraOffsetY);
        float baseScale = skillTreeProjectionScale(layout, projectionData.nodes().size(), zoomScale);
        float depthLift = skillTreeProjectionDepthLift(progress);
        float wobble = (float) Math.sin((ticksActive * 0.07f) + (skillName == null ? 0.0f : skillName.hashCode() * 0.01f));

        int bestIndex = -1;
        float bestDistanceSq = Float.MAX_VALUE;
        BookSceneRenderer.ScreenPoint bestPoint = null;

        for (int index = 0; index < projectionData.nodes().size(); index++) {
            SkillTreeProjectionData.ProjectedNode node = projectionData.nodeByIndex(index);
            ProjectedSkillPoint point = projectSkillPoint(centerX, centerY, baseScale, depthLift, wobble, rotationYawDeg, rotationPitchDeg, node, index);
            float dx = (float) mouseX - point.x();
            float dy = (float) mouseY - point.y();
            float distanceSq = (dx * dx) + (dy * dy);
            float hitRadius = projectedNodeRadiusPx(zoomScale, 0.0f, 0.0f) + 4.0f;

            if (distanceSq <= (hitRadius * hitRadius) && distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestIndex = index;
                bestPoint = new BookSceneRenderer.ScreenPoint(point.x(), point.y());
            }
        }

        if (bestIndex < 0 || bestPoint == null) {
            return null;
        }
        SkillTreeProjectionData.ProjectedNode node = projectionData.nodeByIndex(bestIndex);
        return new BookSceneRenderer.SkillProjectionNodeHit(bestIndex, bestPoint, node != null && node.unlocked());
    }

    public BookSceneRenderer.ScreenPoint projectSkillProjectionNodeScreenPoint(
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
        if (layout == null || projectionData == null || nodeIndex < 0) {
            return null;
        }
        SkillTreeProjectionData.ProjectedNode node = projectionData.nodeByIndex(nodeIndex);
        if (node == null) {
            return null;
        }
        float progress = clamp(alpha, 0.0f, 1.0f);
        float centerX = skillTreeProjectionCenterX(layout, cameraOffsetX);
        float centerY = skillTreeProjectionCenterY(layout, cameraOffsetY);
        float baseScale = skillTreeProjectionScale(layout, projectionData.nodes().size(), zoomScale);
        float depthLift = skillTreeProjectionDepthLift(progress);
        float wobble = (float) Math.sin((ticksActive * 0.07f) + (skillName == null ? 0.0f : skillName.hashCode() * 0.01f));
        ProjectedSkillPoint point = projectSkillPoint(centerX, centerY, baseScale, depthLift, wobble, rotationYawDeg, rotationPitchDeg, node, nodeIndex);
        return new BookSceneRenderer.ScreenPoint(point.x(), point.y());
    }

    private static ProjectedSkillPoint projectSkillPoint(
            float centerX,
            float centerY,
            float baseScale,
            float depthLift,
            float wobble,
            float rotationYawDeg,
            float rotationPitchDeg,
            SkillTreeProjectionData.ProjectedNode node,
            int nodeIndex
    ) {
        float x = node.layoutX() * baseScale;
        float y = node.layoutY() * baseScale;
        float z = ((nodeIndex % 7) - 3) * 8.0f * depthLift + (wobble * 4.0f);

        float yaw = (float) Math.toRadians(rotationYawDeg);
        float pitch = (float) Math.toRadians(rotationPitchDeg);

        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float xYaw = (x * cosYaw) + (z * sinYaw);
        float zYaw = (-x * sinYaw) + (z * cosYaw);

        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);
        float yPitch = (y * cosPitch) - (zYaw * sinPitch);
        float zPitch = (y * sinPitch) + (zYaw * cosPitch);

        float perspective = perspectiveScale((BookSceneRenderer.GUI_Z + 36.0f) + zPitch);
        float centerDistanceSq = (xYaw * xYaw) + (yPitch * yPitch);
        float centerMax = Math.max(40.0f, baseScale * 1.35f);
        float centerDistanceNorm = clamp(centerDistanceSq / (centerMax * centerMax), 0.0f, 1.0f);
        float centerBias = 1.0f - centerDistanceNorm;

        float depthAlpha = clamp(0.58f + (((zPitch + 96.0f) / 192.0f) * 0.50f) + (centerBias * 0.32f), 0.52f, 1.0f);

        return new ProjectedSkillPoint(
                centerX + (xYaw * perspective),
                centerY + (yPitch * perspective),
                depthAlpha,
                zPitch - (centerBias * 22.0f)
        );
    }

    private static void drawProjectedLine(GuiGraphics graphics, ProjectedSkillPoint from, ProjectedSkillPoint to, int color) {
        int x0 = Math.round(from.x());
        int y0 = Math.round(from.y());
        int x1 = Math.round(to.x());
        int y1 = Math.round(to.y());
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void drawProjectedNode(
            GuiGraphics graphics,
            ProjectedSkillPoint point,
            String status,
            String type,
            String iconKey,
            float alpha,
            float zoomScale,
            float holdProgress,
            float celebrationStrength,
            boolean focused
    ) {
        boolean unlocked = "UNLOCKED".equalsIgnoreCase(status);
        boolean available = "AVAILABLE".equalsIgnoreCase(status);
        float typeScale = ("MANIFESTATION".equalsIgnoreCase(type) || "CORE".equalsIgnoreCase(type)) ? 1.5f : 1.0f;
        float focusedScale = focused ? 1.42f : 1.0f;

        int radius = Math.max(5, Math.round(projectedNodeRadiusPx(zoomScale, holdProgress, celebrationStrength) * focusedScale * typeScale));
        int x = Math.round(point.x()) - (radius / 2);
        int y = Math.round(point.y()) - (radius / 2);

        ResourceLocation sprite;
        if ("TECHNIQUE".equalsIgnoreCase(type)) {
            sprite = unlocked ? ResourceLocation.withDefaultNamespace("advancements/goal_frame_obtained") : ResourceLocation.withDefaultNamespace("advancements/goal_frame_unobtained");
        } else if ("MANIFESTATION".equalsIgnoreCase(type) || "AXIOM".equalsIgnoreCase(type) || "CORE".equalsIgnoreCase(type)) {
            sprite = unlocked ? ResourceLocation.withDefaultNamespace("advancements/challenge_frame_obtained") : ResourceLocation.withDefaultNamespace("advancements/challenge_frame_unobtained");
        } else {
            sprite = unlocked ? ResourceLocation.withDefaultNamespace("advancements/task_frame_obtained") : ResourceLocation.withDefaultNamespace("advancements/task_frame_unobtained");
        }

        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (!unlocked && available) {
            r = 0.8f; g = 1.0f; b = 0.8f;
        }

        int alphaInt = Math.min(255, Math.max(0, Math.round(alpha * 255.0f)));
        graphics.fill(x - 2, y - 2, x + radius + 2, y + radius + 2, (alphaInt << 24) | 0x1A1A1A);

        graphics.setColor(r, g, b, alpha);
        graphics.blitSprite(sprite, x - 2, y - 2, radius + 4, radius + 4);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
        if (iconKey != null && !iconKey.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(iconKey);
            if (id != null) {
                stack = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).map(net.minecraft.world.item.ItemStack::new).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        if (stack.isEmpty()) {
            stack = switch (type == null ? "" : type.toLowerCase(java.util.Locale.ROOT)) {
                case "core" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                case "technique" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER);
                case "manifestation" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
                case "axiom" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
                default -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            };
        }

        int iconSize = Math.max(4, Math.min(16, radius - 4));
        int iconX = x + (radius - iconSize) / 2;
        int iconY = y + (radius - iconSize) / 2;

        graphics.pose().pushPose();
        float scale = iconSize / 16.0f;
        graphics.pose().translate(iconX, iconY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        RenderSystem.enableBlend();
        graphics.renderItem(stack, 0, 0);
        RenderSystem.disableBlend();

        graphics.pose().popPose();
    }

    private static float projectedNodeRadiusPx(float zoomScale, float holdProgress, float celebrationStrength) {
        float zoomNodeScale = 0.72f + (clamp(zoomScale, 1.0f, 5.0f) * 0.48f);
        return (8.5f * zoomNodeScale) + (holdProgress * 3.0f) + (celebrationStrength * 3.0f);
    }

    private static void renderProjectionSparkles(
            GuiGraphics graphics,
            float centerX,
            float centerY,
            float orbitRadiusX,
            float orbitRadiusY,
            float depthLift,
            int ticksActive,
            float alpha
    ) {
        int sparkleCount = 42;
        for (int index = 0; index < sparkleCount; index++) {
            double angle = (index * 0.72d) + (ticksActive * 0.045d);
            float radius = 0.20f + ((index % 9) * 0.05f);
            float x = (float) Math.cos(angle) * orbitRadiusX * radius;
            float y = (float) Math.sin(angle * 1.18d) * orbitRadiusY * radius * 0.96f;
            float z = (float) Math.sin(angle * 0.73d) * orbitRadiusX * 0.10f * depthLift;
            float perspective = perspectiveScale((BookSceneRenderer.GUI_Z + 48.0f) + z);
            int px = Math.round(centerX + (x * perspective));
            int py = Math.round(centerY - 14.0f + (y * perspective));
            float pulse = 0.55f + (0.45f * (float) Math.sin((ticksActive * 0.12f) + (index * 0.66f)));
            int color = scaleAlpha(0xFFC2F0FF, alpha * pulse);
            graphics.fill(px, py, px + 2, py + 2, color);
        }
    }

    private static void drawProjectionHalo(
            GuiGraphics graphics,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            int ticksActive,
            float alpha
    ) {
        int ringColorOuter = scaleAlpha(0x7E9BD7FF, alpha);
        int ringColorInner = scaleAlpha(0xA6D8FFFF, alpha);
        float t = ticksActive * 0.06f;
        for (int i = 0; i < 64; i++) {
            double a = (Math.PI * 2.0 * i / 64.0) + t;
            int px = Math.round(centerX + (float) Math.cos(a) * radiusX);
            int py = Math.round(centerY + (float) Math.sin(a) * radiusY);
            graphics.fill(px, py, px + 2, py + 2, ringColorOuter);
        }
        for (int i = 0; i < 48; i++) {
            double a = (Math.PI * 2.0 * i / 48.0) - (t * 1.2f);
            int px = Math.round(centerX + (float) Math.cos(a) * (radiusX * 0.74f));
            int py = Math.round(centerY + (float) Math.sin(a) * (radiusY * 0.74f));
            graphics.fill(px, py, px + 2, py + 2, ringColorInner);
        }
    }

    static float skillTreeProjectionCenterX(BookLayout layout, float cameraOffsetX) {
        return layout.bookX() + (layout.bookWidth() / 2.0f) + cameraOffsetX;
    }

    static float skillTreeProjectionCenterY(BookLayout layout, float cameraOffsetY) {
        return layout.bookY() + (layout.bookHeight() / 2.0f) + BookSceneRenderer.MODEL_Y_OFFSET + BookSceneRenderer.SKILLTREE_BOOK_Y_OFFSET - 184.0f + cameraOffsetY;
    }

    private static float skillTreeProjectionScale(BookLayout layout, int nodeCount, float zoomScale) {
        float sizeBoost = nodeCount > 20 ? 3.70f : 4.10f;
        return (BookSceneRenderer.MODEL_SCALE * BookSceneRenderer.SKILLTREE_BOOK_SCALE * sizeBoost * zoomScale) * (layout.bookWidth() / 520.0f);
    }

    private static float skillTreeProjectionDepthLift(float progress) {
        return 1.35f + (0.95f * progress);
    }

    private static float projectionTreeRadiusX(SkillTreeProjectionData data, float baseScale) {
        float max = 0.0f;
        for (SkillTreeProjectionData.ProjectedNode node : data.nodes()) {
            max = Math.max(max, Math.abs(node.layoutX()));
        }
        return Math.max(baseScale * 0.9f, baseScale * max * 1.06f);
    }

    private static float projectionTreeRadiusY(SkillTreeProjectionData data, float baseScale) {
        float max = 0.0f;
        for (SkillTreeProjectionData.ProjectedNode node : data.nodes()) {
            max = Math.max(max, Math.abs(node.layoutY()));
        }
        return Math.max(baseScale * 0.55f, baseScale * max * 1.08f);
    }

    private static Set<String> requiredNodeIdSet(SkillTreeProjectionData.ProjectedNode node) {
        if (node == null || node.unlocked() || node.requires().isEmpty()) return Set.of();
        Set<String> required = new HashSet<>();
        for (String id : node.requires()) {
            if (id == null || id.isBlank()) continue;
            required.add(id);
            int split = id.indexOf('_');
            if (split > 0 && split < id.length() - 1) required.add(id.substring(split + 1));
        }
        return required;
    }

    private static boolean isRequirementEdge(SkillTreeProjectionData.ProjectedEdge edge, SkillTreeProjectionData.ProjectedNode selectedNode, Set<String> requiredNodeIds) {
        if (selectedNode == null || requiredNodeIds.isEmpty() || selectedNode.unlocked()) return false;
        String selectedId = selectedNode.id();
        String from = edge.fromNodeId();
        String to = edge.toNodeId();
        return (selectedId.equals(from) && requiredNodeIds.contains(to)) || (selectedId.equals(to) && requiredNodeIds.contains(from));
    }

    private static int scaleAlpha(int argb, float alphaScale) {
        int sourceA = (argb >>> 24) & 0xFF;
        int scaledA = Math.min(255, Math.max(0, Math.round(sourceA * clamp(alphaScale, 0.0f, 1.0f))));
        return (scaledA << 24) | (argb & 0x00FFFFFF);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float from, float to, float progress) {
        return from + ((to - from) * progress);
    }

    private static float perspectiveScale(float z) {
        return BookSceneRenderer.PAGE_PICK_CAMERA_DEPTH / Math.max(1.0f, BookSceneRenderer.PAGE_PICK_CAMERA_DEPTH - z);
    }

    private record ProjectedSkillPoint(float x, float y, float depthAlpha, float depthSortZ) {}

    private record ProjectedEdgeRender(ProjectedSkillPoint from, ProjectedSkillPoint to, int color, float depthSortZ) {}
}
