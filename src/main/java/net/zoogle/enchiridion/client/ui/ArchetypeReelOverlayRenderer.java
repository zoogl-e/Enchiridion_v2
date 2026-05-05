package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.client.levelrpg.archetype.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.archetype.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.archetype.ReelFlowController;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

import java.util.List;

/**
 * Renderer for the Archetype Reel overlay elements (info panel and cover hints).
 */
public final class ArchetypeReelOverlayRenderer {

    public void renderTopmost(
            GuiGraphics graphics,
            Font font,
            ArchetypeReelState reelState,
            ReelFlowController flowController,
            BookViewState viewState,
            InfoPanelLayoutService.PanelRect panelRect
    ) {
        if (reelState.dragging()) {
            return;
        }

        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 640.0F);
        try {
            renderInfoPanel(graphics, font, reelState, flowController, viewState, panelRect);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private void renderInfoPanel(
            GuiGraphics graphics,
            Font font,
            ArchetypeReelState reelState,
            ReelFlowController flowController,
            BookViewState viewState,
            InfoPanelLayoutService.PanelRect panelRect
    ) {
        JournalArchetypeChoice focusedChoice = reelState.focusedChoice();
        if (focusedChoice == null || viewState.layout() == null || !panelRect.visible()) {
            return;
        }

        float alpha = reelState.globalPresentationAlpha();
        int px = panelRect.x();
        int py = panelRect.y();
        int pw = panelRect.width();
        int ph = panelRect.height();

        // Background layers
        graphics.fill(px, py, px + pw, py + ph, scaleColorAlpha(0xB819120D, alpha));
        graphics.fill(px + 2, py + 2, px + pw - 2, py + ph - 2, scaleColorAlpha(0xDCCBAF86, alpha));
        graphics.renderOutline(px, py, pw, ph, scaleColorAlpha(0xFF6D4A2C, alpha));
        graphics.renderOutline(px + 4, py + 4, pw - 8, ph - 8, scaleColorAlpha(0x446D4A2C, alpha));

        // Nav hint strip separator
        int hintStripTop = py + ph - 20;
        graphics.fill(px + 4, hintStripTop, px + pw - 4, hintStripTop + 1, scaleColorAlpha(0x446D4A2C, alpha));

        int pad = 12;
        int textX = px + pad;
        int textW = pw - pad * 2;
        int contentBottom = hintStripTop - 4;
        int y = py + pad;

        // Title — centered
        String title = focusedChoice.title();
        int titleX = px + (pw / 2) - (font.width(title) / 2);
        graphics.drawString(font, title, titleX, y, scaleColorAlpha(0xFF2A180F, alpha), false);
        y += 12;

        // Decorative rule under title
        graphics.fill(px + 16, y, px + pw - 16, y + 1, scaleColorAlpha(0x886D4A2C, alpha));
        y += 6;

        // Phase label — only during binding states
        String phaseLabel = switch (flowController.phase()) {
            case REQUESTING_BIND -> "Inscribing\u2026";
            case AWAITING_SYNC -> "Awaiting Seal";
            case FAILED -> "Binding Failed";
            default -> null;
        };

        if (phaseLabel != null) {
            int labelX = px + (pw / 2) - (font.width(phaseLabel) / 2);
            graphics.drawString(font, phaseLabel, labelX, y, scaleColorAlpha(0xFFB07030, alpha), false);
            y += 13;
        }

        // Description
        y = drawWrappedLines(
                graphics,
                font,
                focusedChoice.description().isBlank() ? "No inscription accompanies this archetype yet." : focusedChoice.description(),
                textX, y, textW, contentBottom - 22,
                scaleColorAlpha(0xFF3B271A, alpha)
        );

        // Disciplines section — only if space remains
        if (y + 22 <= contentBottom && !focusedChoice.startingDisciplines().isBlank()) {
            y += 6;
            graphics.drawString(font, "Disciplines", textX, y, scaleColorAlpha(0xFF8B6444, alpha), false);
            y += 12;
            drawWrappedLines(graphics, font, focusedChoice.startingDisciplines(), textX, y, textW, contentBottom, scaleColorAlpha(0xFF3B271A, alpha));
        }

        // Nav hints inside the panel's bottom strip
        if (flowController.phase() == ReelFlowController.FlowPhase.SELECTING) {
            int hintY = hintStripTop + 6;
            int hintColor = scaleColorAlpha(0xFF6F513C, alpha);
            int centerX = px + pw / 2;
            String leftHint = "\u00AB A";
            String rightHint = "D \u00BB";
            graphics.drawString(font, leftHint, centerX - font.width(leftHint) - 8, hintY, hintColor, false);
            graphics.drawString(font, rightHint, centerX + 8, hintY, hintColor, false);

            String dragHint = "Drag to rotate \u2022 release to settle";
            int dragHintX = centerX - (font.width(dragHint) / 2);
            graphics.drawString(font, dragHint, dragHintX, hintY - 11, hintColor, false);
        }
    }

    public void renderDropTargetHint(GuiGraphics graphics, ArchetypeReelState reelState, BookSceneRenderer.ScreenRect slot) {
        if (slot == null) {
            return;
        }

        int left = Math.round(slot.x());
        int top = Math.round(slot.y());
        int width = Math.max(1, Math.round(slot.width()));
        int height = Math.max(1, Math.round(slot.height()));

        int tint = reelState.dropTargetHovered() ? 0x4AA3E66A : 0x304A2D17;
        int border = reelState.dropTargetHovered() ? 0xFFD3F2A9 : 0xFFC18A62;

        graphics.fill(left, top, left + width, top + height, tint);
        graphics.renderOutline(left, top, width, height, border);
    }

    private int drawWrappedLines(GuiGraphics graphics, Font font, String text, int x, int y, int width, int maxY, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (y > maxY) {
                break;
            }
            graphics.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    private static int scaleColorAlpha(int argb, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(((argb >>> 24) & 0xFF) * alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
