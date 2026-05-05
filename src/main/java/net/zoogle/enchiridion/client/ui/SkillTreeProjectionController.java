package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.zoogle.enchiridion.client.levelrpg.JournalSkillEntry;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.SkillTreeProjectionData;
import net.zoogle.enchiridion.client.levelrpg.SkillTreeProjectionState;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * Controller for the Skill Tree projection modal state and UI.
 * Handles state, camera logic, node interaction, and overlay rendering.
 */
public final class SkillTreeProjectionController {
    private final SkillTreeProjectionState state = new SkillTreeProjectionState();
    private SkillTreeProjectionData data = SkillTreeProjectionData.empty("");
    private String dataSkill = "";

    public void tick(BookScreenController controller, BookViewState viewState, int width, int height) {
        state.tick();
        refreshDataIfNeeded(controller);
        clampCameraToViewport(viewState, width, height);
    }

    public boolean isActive() {
        return state.active();
    }

    public float visualAlpha() {
        return state.visualAlpha();
    }

    public boolean open(String skillName, List<String> availableSkills) {
        boolean opened = state.open(availableSkills, skillName);
        if (opened) {
            dataSkill = "";
            data = SkillTreeProjectionData.empty(skillName);
        }
        return opened;
    }

    public void close() {
        state.close();
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            BookViewState viewState,
            BookScreenController controller,
            BookSceneRenderer sceneRenderer,
            int mouseX,
            int mouseY
    ) {
        if (!state.active()) {
            return;
        }

        sceneRenderer.renderSkillTreeProjection(
                graphics,
                viewState.layout(),
                state.focusedSkill(),
                data,
                state.visualAlpha(),
                state.ticksActive(),
                state.projectionYawDeg(),
                state.projectionPitchDeg(),
                state.zoomScale(),
                state.cameraOffsetX(),
                state.cameraOffsetY(),
                state.selectedNodeIndex(),
                0.0f,
                state.celebrationStrength()
        );

        renderHints(graphics, font, graphics.guiWidth(), graphics.guiHeight());
        renderDetails(graphics, font, graphics.guiWidth(), graphics.guiHeight(), controller.context(), sceneRenderer);

        int hoveredIndex = state.hoveredNodeIndex();
        if (hoveredIndex >= 0) {
            BookSceneRenderer.SkillProjectionNodeDescriptor descriptor = sceneRenderer.describeSkillProjectionNode(data, hoveredIndex);
            SkillTreeProjectionData.ProjectedNode node = data.nodeByIndex(hoveredIndex);
            if (descriptor != null && node != null) {
                renderAdvancementTooltip(graphics, font, node, descriptor, mouseX, mouseY, graphics.guiWidth(), graphics.guiHeight());
            }
        }
    }

    public boolean keyPressed(int keyCode) {
        if (!state.active()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            state.close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            return navigateSelectedNode(-1);
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            return navigateSelectedNode(1);
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int width, int height, BookViewState viewState, BookSceneRenderer sceneRenderer, net.zoogle.enchiridion.api.BookContext context) {
        if (!state.active()) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (tryUnlockSelectedNodeFromPanel(mouseX, mouseY, width, context)) {
                return true;
            }
            int hoveredNode = state.hoveredNodeIndex();
            if (hoveredNode >= 0) {
                state.selectNodeIndex(hoveredNode);
                state.setZoomScale(Math.max(state.zoomScale(), 2.85f));
                focusSelectedNode(viewState, sceneRenderer, width, height);
                return true;
            }
            state.selectNodeIndex(-1);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, int width, int height, BookViewState viewState) {
        if (!state.active()) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            state.rotateByDrag((float) dragX, (float) dragY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            applyPanDrag((float) dragX, (float) dragY, width, height, viewState);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, BookViewState viewState, int width, int height) {
        if (!state.active()) {
            return false;
        }
        float previousZoom = state.zoomScale();
        if (state.adjustZoom((float) scrollY)) {
            float newZoom = state.zoomScale();
            float ratio = newZoom / Math.max(0.001f, previousZoom);
            float centerX = BookSceneRenderer.skillTreeProjectionCenterX(viewState.layout(), state.cameraTargetOffsetX());
            float centerY = BookSceneRenderer.skillTreeProjectionCenterY(viewState.layout(), state.cameraTargetOffsetY());
            float dx = (float) mouseX - centerX;
            float dy = (float) mouseY - centerY;
            state.addCameraOffset(dx * (1.0f - ratio), dy * (1.0f - ratio));
            clampCameraToViewport(viewState, width, height);
            return true;
        }
        return false;
    }

    public void mouseReleased(int button) {
        if (state.active() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            state.endDragRotation();
        }
    }

    public void updateHover(BookViewState viewState, BookSceneRenderer sceneRenderer, int mouseX, int mouseY) {
        if (!state.active()) {
            return;
        }
        BookSceneRenderer.SkillProjectionNodeHit hit = sceneRenderer.pickSkillProjectionNode(
                viewState.layout(),
                state.focusedSkill(),
                data,
                state.visualAlpha(),
                state.ticksActive(),
                state.projectionYawDeg(),
                state.projectionPitchDeg(),
                state.zoomScale(),
                state.cameraOffsetX(),
                state.cameraOffsetY(),
                mouseX,
                mouseY
        );
        int hovered = hit == null ? -1 : hit.index();
        state.setHoveredNodeIndex(hovered);
        state.onHoverChanged(hovered);
    }

    private void refreshDataIfNeeded(BookScreenController controller) {
        if (!state.active()) {
            return;
        }
        String focusedSkill = state.focusedSkill();
        if (focusedSkill == null) {
            return;
        }
        if (focusedSkill.equalsIgnoreCase(dataSkill) && !data.nodes().isEmpty()) {
            return;
        }
        data = LevelRpgJournalInteractionBridge.projectionData(controller.context(), focusedSkill);
        dataSkill = focusedSkill;
    }


    public void focusSelectedNode(BookViewState viewState, BookSceneRenderer sceneRenderer, int width, int height) {
        int selected = state.selectedNodeIndex();
        if (selected < 0 || viewState.layout() == null) {
            return;
        }
        BookSceneRenderer.ScreenPoint point = sceneRenderer.projectSkillProjectionNodeScreenPoint(
                viewState.layout(),
                state.focusedSkill(),
                data,
                state.visualAlpha(),
                state.ticksActive(),
                state.projectionYawDeg(),
                state.projectionPitchDeg(),
                state.zoomScale(),
                0.0f,
                0.0f,
                selected
        );
        if (point == null) {
            return;
        }
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        state.setCameraOffset(centerX - point.x(), centerY - point.y());
        clampCameraToViewport(viewState, width, height);
    }

    private void clampCameraToViewport(BookViewState viewState, int width, int height) {
        if (!state.active() || viewState.layout() == null) {
            return;
        }
        float zoomT = clamp((state.zoomScale() - 1.0f) / 4.0f, 0.0f, 1.0f);
        float strictRadius = 68.0f;
        float looseRadius = Math.max(width, height) * 0.85f;
        float allowedRadius = strictRadius + ((looseRadius - strictRadius) * zoomT);
        float targetX = state.cameraTargetOffsetX();
        float targetY = state.cameraTargetOffsetY();
        float lengthSq = (targetX * targetX) + (targetY * targetY);
        float clampedTargetX = targetX;
        float clampedTargetY = targetY;
        if (lengthSq > (allowedRadius * allowedRadius)) {
            float length = (float) Math.sqrt(lengthSq);
            float scale = allowedRadius / Math.max(0.0001f, length);
            clampedTargetX = targetX * scale;
            clampedTargetY = targetY * scale;
        }
        state.setCameraOffset(clampedTargetX, clampedTargetY);
    }

    private void applyPanDrag(float dragX, float dragY, int width, int height, BookViewState viewState) {
        float zoomT = clamp((state.zoomScale() - 1.0f) / 4.0f, 0.0f, 1.0f);
        float strictRadius = 68.0f;
        float looseRadius = Math.max(width, height) * 0.85f;
        float allowedRadius = strictRadius + ((looseRadius - strictRadius) * zoomT);
        float targetX = state.cameraTargetOffsetX();
        float targetY = state.cameraTargetOffsetY();
        float distance = (float) Math.sqrt((targetX * targetX) + (targetY * targetY));
        float radialNorm = clamp(distance / Math.max(1.0f, allowedRadius), 0.0f, 1.0f);
        float lowZoomStrength = 1.0f - zoomT;
        float resistance = 1.0f - (0.82f * lowZoomStrength * radialNorm * radialNorm);
        float adjustedDragX = dragX * clamp(resistance, 0.14f, 1.0f);
        float adjustedDragY = dragY * clamp(resistance, 0.14f, 1.0f);
        state.addCameraOffsetImmediate(adjustedDragX, adjustedDragY);
        clampCameraToViewport(viewState, width, height);
    }

    private boolean navigateSelectedNode(int direction) {
        if (direction == 0 || data.nodes().isEmpty()) {
            return false;
        }
        int current = state.selectedNodeIndex();
        if (current < 0) {
            current = 0;
        } else {
            current = Math.floorMod(current + direction, data.nodes().size());
        }
        state.selectNodeIndex(current);
        // Note: focusSelectedNode should be called by the caller who has width/height
        return true;
    }

    private void renderHints(GuiGraphics graphics, Font font, int width, int height) {
        String label = state.focusedSkill().isBlank()
                ? "Skill Projection"
                : state.focusedSkill() + " Projection";
        int alpha = Math.min(255, Math.max(0, Math.round(state.visualAlpha() * 255.0f)));
        int titleColor = (alpha << 24) | 0xE8DCC6;
        int hintColor = (alpha << 24) | 0xBDAA86;
        int titleX = (width / 2) - (font.width(label) / 2);
        graphics.drawString(font, label, titleX, 12, titleColor, false);
        String hints = "A/D previous/next node  |  Esc close";
        String dragHint = "Left drag orbit  |  Right/Middle drag pan";
        String zoomHint = "Mouse wheel to zoom";
        int hintX = (width / 2) - (font.width(hints) / 2);
        int dragHintX = (width / 2) - (font.width(dragHint) / 2);
        int zoomHintX = (width / 2) - (font.width(zoomHint) / 2);
        graphics.drawString(font, hints, hintX, 24, hintColor, false);
        graphics.drawString(font, dragHint, dragHintX, 34, hintColor, false);
        graphics.drawString(font, zoomHint, zoomHintX, 44, hintColor, false);
    }

    private void renderDetails(GuiGraphics graphics, Font font, int width, int height, net.zoogle.enchiridion.api.BookContext context) {
        // Redundant method to be removed or merged
    }

    public void renderDetails(GuiGraphics graphics, Font font, int width, int height, net.zoogle.enchiridion.api.BookContext context, BookSceneRenderer sceneRenderer) {
        int nodeIndex = state.selectedNodeIndex();
        if (nodeIndex < 0) {
            return;
        }
        SkillTreeProjectionData.ProjectedNode projectedNode = data.nodeByIndex(nodeIndex);
        if (projectedNode == null) {
            return;
        }
        JournalSkillEntry skillEntry = LevelRpgJournalInteractionBridge.skillEntry(context, state.focusedSkill());
        BookSceneRenderer.SkillProjectionNodeDescriptor descriptor = sceneRenderer.describeSkillProjectionNode(data, nodeIndex);
        String subtitle = descriptor.subtitle();
        String requirement = descriptor.requirement();
        String effectText = projectedNode.description();
        String pointsText = "Points " + data.insight() + " ready | "
                + data.spentPoints() + " spent | "
                + data.earnedPoints() + " earned";
        int alpha = Math.min(255, Math.max(0, Math.round(state.visualAlpha() * 255.0f)));
        PanelRect panel = projectionPanelRect(width);
        int panelW = panel.w();
        int panelH = panel.h();
        int panelX = panel.x();
        int panelY = panel.y();
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, (alpha << 24) | 0x1A1310);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, (alpha << 24) | 0x8DAED5);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, (alpha << 24) | 0x8DAED5);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, (alpha << 24) | 0x8DAED5);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, (alpha << 24) | 0x8DAED5);
        graphics.drawString(font, descriptor.title(), panelX + 10, panelY + 10, (alpha << 24) | 0xF5EEE0, false);
        graphics.drawString(font, subtitle, panelX + 10, panelY + 24, (alpha << 24) | 0xA7DFFF, false);
        graphics.drawString(font, descriptor.tierLabel() + "  |  Cost " + projectedNode.cost(), panelX + 10, panelY + 38, (alpha << 24) | 0xDECBA7, false);
        graphics.drawString(font, pointsText, panelX + 10, panelY + 52, (alpha << 24) | 0xBDE0BC, false);
        graphics.drawString(font, "Status: " + projectionStatusLabel(projectedNode), panelX + 10, panelY + 66, (alpha << 24) | 0xE1CFB8, false);
        graphics.drawString(font, firstWrappedLine(font, requirement, panelW - 20), panelX + 10, panelY + 82, (alpha << 24) | 0xC8BBA8, false);
        graphics.drawString(font, firstWrappedLine(font, effectText, panelW - 20), panelX + 10, panelY + 96, (alpha << 24) | 0xB8D7E8, false);
        PanelRect button = unlockButtonRect(panel);
        boolean canUnlock = !projectedNode.unlocked();
        int buttonColor = canUnlock ? ((alpha << 24) | 0x2E6A4A) : ((alpha << 24) | 0x4D4D4D);
        graphics.fill(button.x(), button.y(), button.x() + button.w(), button.y() + button.h(), buttonColor);
        graphics.renderOutline(button.x(), button.y(), button.w(), button.h(), (alpha << 24) | 0xB8D8C2);
        String buttonLabel = projectedNode.unlocked() ? "Already Inscribed" : "Unlock Node";
        int labelX = button.x() + (button.w() - font.width(buttonLabel)) / 2;
        graphics.drawString(font, buttonLabel, labelX, button.y() + 5, (alpha << 24) | 0xE8F0EA, false);
        if (skillEntry != null && !skillEntry.pathForward().isBlank()) {
            graphics.drawString(font, firstWrappedLine(font, skillEntry.pathForward(), panelW - 20), panelX + 10, panelY + 136, (alpha << 24) | 0xADB5A8, false);
        }
        String status = state.statusMessage();
        if (!status.isBlank()) {
            int statusColor = state.statusMessageSuccess()
                    ? (alpha << 24) | 0x86E7AF
                    : (alpha << 24) | 0xE39090;
            graphics.drawString(font, truncate(font, status, panelW - 20), panelX + 10, panelY + panelH - 12, statusColor, false);
        }
    }

    private boolean tryUnlockSelectedNodeFromPanel(double mouseX, double mouseY) {
        // Obsolete, use the public version with context
        return false;
    }

    public boolean tryUnlockSelectedNodeFromPanel(double mouseX, double mouseY, int width, net.zoogle.enchiridion.api.BookContext context) {
        int selectedIndex = state.selectedNodeIndex();
        if (selectedIndex < 0) {
            return false;
        }
        PanelRect panel = projectionPanelRect(width);
        PanelRect button = unlockButtonRect(panel);
        if (!isPointInRect(mouseX, mouseY, button)) {
            return false;
        }
        SkillTreeProjectionData.ProjectedNode node = data.nodeByIndex(selectedIndex);
        if (node == null) {
            return false;
        }
        if (node.unlocked()) {
            state.setStatusMessage("Node is already inscribed.", false);
            return true;
        }
        boolean sent = LevelRpgJournalInteractionBridge.requestSkillProjectionNodeUnlock(
                context,
                state.focusedSkill(),
                node.id()
        );
        state.setStatusMessage(
                sent ? "Node inscription sent." : "Unable to inscribe node.",
                sent
        );
        if (sent) {
            state.triggerCelebration();
            dataSkill = "";
        }
        return true;
    }

    private void renderAdvancementTooltip(GuiGraphics graphics, Font font, SkillTreeProjectionData.ProjectedNode node, BookSceneRenderer.SkillProjectionNodeDescriptor descriptor, int mouseX, int mouseY, int width, int height) {
        String title = descriptor.title();
        String subtitle = descriptor.subtitle();
        String requirement = descriptor.requirement();
        String cost = "Cost " + node.cost();
        String desc = node.description() != null ? node.description() : "";
        String pointsText = "Points " + data.insight() + " ready | "
                + data.spentPoints() + " spent | "
                + data.earnedPoints() + " earned";
        int titleWidth = font.width(title);
        int subtitleWidth = font.width(subtitle);
        int reqWidth = font.width(requirement);
        int costWidth = font.width(cost);
        int pointsWidth = font.width(pointsText);
        List<FormattedCharSequence> descLines = desc.isBlank() ? List.of() : font.split(Component.literal(desc), 220);
        int textWidth = Math.max(Math.max(titleWidth, subtitleWidth), Math.max(reqWidth, Math.max(costWidth, pointsWidth)));
        for (FormattedCharSequence line : descLines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int tooltipWidth = textWidth + 36;
        int iconSize = 26;
        int titleHeight = 26;
        int bodyHeight = 6;
        if (!subtitle.isBlank()) bodyHeight += 12;
        if (!desc.isBlank()) bodyHeight += 12 * descLines.size();
        if (!requirement.isBlank()) bodyHeight += 12;
        bodyHeight += 12;
        bodyHeight += 12;
        int totalHeight = titleHeight + bodyHeight;
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + tooltipWidth > width) {
            x = mouseX - 12 - tooltipWidth;
        }
        if (y + totalHeight > height) {
            y = height - totalHeight;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.fill(x, y + titleHeight - 2, x + tooltipWidth, y + totalHeight, 0xEE000000);
        graphics.renderOutline(x, y + titleHeight - 2, tooltipWidth, bodyHeight + 2, 0xFF333333);
        boolean unlocked = node.unlocked();
        boolean available = "AVAILABLE".equalsIgnoreCase(node.status());
        int titleBgColor;
        if (unlocked) {
            titleBgColor = "TECHNIQUE".equalsIgnoreCase(node.type()) ? 0xFF006633 : "MANIFESTATION".equalsIgnoreCase(node.type()) ? 0xFF4A086D : 0xFF1B556D;
        } else if (available) {
            titleBgColor = 0xFF444444;
        } else {
            titleBgColor = 0xFF222222;
        }
        graphics.fill(x + iconSize - 2, y, x + tooltipWidth, y + titleHeight, titleBgColor);
        graphics.renderOutline(x + iconSize - 2, y, tooltipWidth - iconSize + 2, titleHeight, 0xFF111111);
        graphics.drawString(font, title, x + iconSize + 6, y + 9, unlocked ? 0xFFFFFF : 0xAAAAAA, true);
        int textY = y + titleHeight + 4;
        if (!subtitle.isBlank()) {
            graphics.drawString(font, subtitle, x + 6, textY, 0x55FFFF, false);
            textY += 12;
        }
        if (!requirement.isBlank()) {
            graphics.drawString(font, requirement, x + 6, textY, unlocked ? 0x55FF55 : 0xFF5555, false);
            textY += 12;
        }
        for (FormattedCharSequence line : descLines) {
            graphics.drawString(font, line, x + 6, textY, 0xAAAAAA, false);
            textY += 12;
        }
        graphics.drawString(font, cost, x + 6, textY, 0xFFFF55, false);
        textY += 12;
        graphics.drawString(font, pointsText, x + 6, textY, 0xBDE0BC, false);
        ResourceLocation frameSprite;
        String type = node.type();
        if ("TECHNIQUE".equalsIgnoreCase(type)) {
            frameSprite = unlocked ? ResourceLocation.withDefaultNamespace("advancements/goal_frame_obtained") : ResourceLocation.withDefaultNamespace("advancements/goal_frame_unobtained");
        } else if ("MANIFESTATION".equalsIgnoreCase(type) || "AXIOM".equalsIgnoreCase(type) || "CORE".equalsIgnoreCase(type)) {
            frameSprite = unlocked ? ResourceLocation.withDefaultNamespace("advancements/challenge_frame_obtained") : ResourceLocation.withDefaultNamespace("advancements/challenge_frame_unobtained");
        } else {
            frameSprite = unlocked ? ResourceLocation.withDefaultNamespace("advancements/task_frame_obtained") : ResourceLocation.withDefaultNamespace("advancements/task_frame_unobtained");
        }
        graphics.blitSprite(frameSprite, x, y, iconSize, iconSize);
        net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
        String iconKey = node.iconKey();
        if (iconKey != null && !iconKey.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(iconKey);
            if (id != null) {
                stack = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).map(net.minecraft.world.item.ItemStack::new).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        if (stack.isEmpty()) {
            stack = switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
                case "core" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                case "technique" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER);
                case "manifestation" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
                case "axiom" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
                default -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            };
        }
        graphics.renderItem(stack, x + 5, y + 5);
        graphics.pose().popPose();
    }

    private PanelRect projectionPanelRect(int width) {
        int panelW = 236;
        int panelH = 154;
        return new PanelRect(width - panelW - 14, 36, panelW, panelH);
    }

    private PanelRect unlockButtonRect(PanelRect panel) {
        return new PanelRect(panel.x() + 10, panel.y() + 112, panel.w() - 20, 14);
    }

    private boolean isPointInRect(double mouseX, double mouseY, PanelRect rect) {
        return mouseX >= rect.x()
                && mouseX <= rect.x() + rect.w()
                && mouseY >= rect.y()
                && mouseY <= rect.y() + rect.h();
    }

    private String projectionStatusLabel(SkillTreeProjectionData.ProjectedNode node) {
        String status = node.status();
        if ("UNLOCKED".equalsIgnoreCase(status)) return "Unlocked";
        if ("AVAILABLE".equalsIgnoreCase(status)) return "Available";
        if ("LOCKED_LEVEL".equalsIgnoreCase(status)) return "Blocked (level)";
        if ("LOCKED_PREREQUISITE".equalsIgnoreCase(status)) return "Blocked (prerequisites)";
        if ("LOCKED_POINTS".equalsIgnoreCase(status)) return "Blocked (points)";
        return "Blocked";
    }

    private String truncate(Font font, String text, int width) {
        if (text == null || text.isBlank()) return "";
        if (font.width(text) <= width) return text;
        String value = text;
        String ellipsis = "...";
        int limit = Math.max(0, width - font.width(ellipsis));
        while (!value.isEmpty() && font.width(value) > limit) {
            value = value.substring(0, value.length() - 1);
        }
        return value + ellipsis;
    }

    private String firstWrappedLine(Font font, String text, int width) {
        if (text == null || text.isBlank()) return "";
        return truncate(font, text, width);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PanelRect(int x, int y, int w, int h) {}
}
