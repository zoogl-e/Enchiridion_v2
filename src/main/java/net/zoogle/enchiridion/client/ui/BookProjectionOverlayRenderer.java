package net.zoogle.enchiridion.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.zoogle.enchiridion.api.BookProjectionView;

import java.util.List;

final class BookProjectionOverlayRenderer {
    private static final String[] PROJECTION_GLYPHS = {
            "\u2726", "\u2727", "\u2055", "\u2058", "\u2020", "\u2021", "\u203b", "\u2299",
            "\u27e1", "\u27e2", "\u27e3", "\u27ea", "\u27eb", "\u2303", "\u2234", "\u2235"
    };

    ProjectionButtonHit resolveProjectionButton(BookLayout layout, int screenWidth, int screenHeight, BookScreenController controller, int mouseX, int mouseY) {
        if (layout == null || !controller.isProjectionInteractive()) {
            return null;
        }
        BookProjectionView view = controller.projectionView();
        if (view == null || !view.hasPrimaryAction()) {
            return null;
        }
        PanelLayout panel = projectionPanelLayout(screenWidth, screenHeight);
        int buttonWidth = Math.min(120, panel.width() - 32);
        int buttonHeight = 16;
        int buttonX = panel.x() + (panel.width() - buttonWidth) / 2;
        int buttonY = panel.y() + panel.height() - 28;
        if (mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
            return new ProjectionButtonHit(view, buttonX, buttonY, buttonWidth, buttonHeight);
        }
        return null;
    }

    void renderProjectionOverlay(GuiGraphics graphics, Font font, int screenWidth, int screenHeight, BookScreenController controller, BookViewState viewState) {
        BookProjectionView view = controller.projectionView();
        if (view == null) {
            return;
        }

        float alpha = controller.projectionProgress();
        PanelLayout panel = projectionPanelLayout(screenWidth, screenHeight);
        renderProjectionBackdrop(graphics, panel, screenWidth, screenHeight, alpha);
        renderPanelBase(graphics, panel, alpha);
        renderGlyphDither(graphics, font, panel, alpha, view.focusId());
        renderProjectionOrnaments(graphics, font, panel, alpha);
        renderSkillGlyphIcon(graphics, font, panel, view, alpha);
        renderProjectionText(graphics, font, panel, view, alpha);
        drawCenteredText(graphics, font, Component.literal("Esc to return to the journal"), panel.x(), panel.width(), panel.y() + panel.height() - 14, withAlpha(0xFF5A4636, Math.max(alpha, 0.95f)));

        if (view.hasPrimaryAction()) {
            drawProjectionButton(graphics, font, panel, controller.projectionView(), viewState.hoveredProjectionButton(), alpha);
        }
    }

    private void renderProjectionBackdrop(GuiGraphics graphics, PanelLayout panel, int screenWidth, int screenHeight, float alpha) {
        graphics.fill(0, 0, screenWidth, screenHeight, withAlpha(0x8A000000, alpha));
        int left = panel.x();
        int top = panel.y();
        int right = panel.x() + panel.width();
        int bottom = panel.y() + panel.height();
        int halo = withAlpha(0x38110D18, Math.max(alpha, 0.85f));
        int glow = withAlpha(0x1E6B52C2, Math.max(alpha, 0.7f));
        int corner = withAlpha(0x500B0910, Math.max(alpha, 0.82f));

        graphics.fill(left - 24, top + 10, left - 12, bottom - 14, halo);
        graphics.fill(right + 12, top + 14, right + 24, bottom - 10, halo);
        graphics.fill(left + 22, top - 16, right - 26, top - 7, halo);
        graphics.fill(left + 26, bottom + 7, right - 20, bottom + 16, halo);
        graphics.fill(left - 18, top + 34, left - 12, bottom - 34, withAlpha(0x220B0910, Math.max(alpha, 0.72f)));
        graphics.fill(right + 12, top + 30, right + 18, bottom - 38, withAlpha(0x1C0B0910, Math.max(alpha, 0.68f)));
        graphics.fill(left - 12, top - 10, left + 10, top + 12, corner);
        graphics.fill(right - 10, top - 10, right + 12, top + 12, corner);
        graphics.fill(left - 12, bottom - 12, left + 10, bottom + 10, corner);
        graphics.fill(right - 10, bottom - 12, right + 12, bottom + 10, corner);
        graphics.fill(left - 8, top + 20, left - 2, bottom - 20, glow);
        graphics.fill(right + 2, top + 20, right + 8, bottom - 20, glow);
        graphics.fill(left + 26, top - 6, right - 26, top - 2, glow);
        graphics.fill(left + 26, bottom + 2, right - 26, bottom + 6, glow);
    }

    private void renderPanelBase(GuiGraphics graphics, PanelLayout panel, float alpha) {
        int base = withAlpha(0xF2E2D4BC, Math.max(alpha, 0.97f));
        int inner = withAlpha(0x34FFF5DE, Math.max(alpha, 0.45f));
        int edgeShade = withAlpha(0x246A5133, Math.max(alpha, 0.85f));
        int innerShade = withAlpha(0x1280674A, Math.max(alpha, 0.8f));
        int centerSupport = withAlpha(0x14917A56, Math.max(alpha, 0.75f));
        graphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), base);
        graphics.fill(panel.x() + 6, panel.y() + 6, panel.x() + panel.width() - 6, panel.y() + panel.height() - 6, inner);
        graphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + 7, edgeShade);
        graphics.fill(panel.x(), panel.y() + panel.height() - 7, panel.x() + panel.width(), panel.y() + panel.height(), edgeShade);
        graphics.fill(panel.x(), panel.y(), panel.x() + 7, panel.y() + panel.height(), edgeShade);
        graphics.fill(panel.x() + panel.width() - 7, panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), edgeShade);
        graphics.fill(panel.x() + 18, panel.y() + 18, panel.x() + panel.width() - 18, panel.y() + panel.height() - 28, innerShade);
        graphics.fill(panel.x() + 28, panel.y() + 52, panel.x() + panel.width() - 28, panel.y() + 53, centerSupport);
        graphics.fill(panel.x() + 34, panel.y() + 72, panel.x() + panel.width() - 34, panel.y() + 73, withAlpha(0x0E8E7350, alpha));
    }

    private void renderProjectionOrnaments(GuiGraphics graphics, Font font, PanelLayout panel, float alpha) {
        int accent = withAlpha(0xFF6F53C0, Math.max(alpha, 0.88f));
        int support = withAlpha(0xFF9C7B51, Math.max(alpha, 0.72f));
        renderCornerMark(graphics, font, panel.x() + 10, panel.y() + 8, false, accent, support);
        renderCornerMark(graphics, font, panel.x() + panel.width() - 18, panel.y() + 8, true, accent, support);
        renderCornerMark(graphics, font, panel.x() + 10, panel.y() + panel.height() - 22, false, support, withAlpha(0xFF6F53C0, Math.max(alpha, 0.6f)));
        renderCornerMark(graphics, font, panel.x() + panel.width() - 18, panel.y() + panel.height() - 22, true, support, withAlpha(0xFF6F53C0, Math.max(alpha, 0.6f)));
        renderArcaneDivider(graphics, font, panel, panel.y() + 38, accent, support);
        graphics.drawString(font, "\u2058", panel.x() + 18, panel.y() + 29, withAlpha(0xAA8E7350, Math.max(alpha, 0.7f)), false);
        graphics.drawString(font, "\u2234", panel.x() + panel.width() - 26, panel.y() + 30, withAlpha(0x88906E48, Math.max(alpha, 0.64f)), false);
        graphics.drawString(font, "\u2235", panel.x() + 16, panel.y() + panel.height() - 30, withAlpha(0x66906E48, Math.max(alpha, 0.58f)), false);
        graphics.drawString(font, "\u2055", panel.x() + panel.width() - 24, panel.y() + panel.height() - 30, withAlpha(0x88805CCB, Math.max(alpha, 0.62f)), false);
    }

    private void renderGlyphDither(GuiGraphics graphics, Font font, PanelLayout panel, float alpha, String seedText) {
        long timeBucket = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() / 6L : 0L;
        long baseSeed = (seedText == null ? 0L : seedText.hashCode()) ^ timeBucket;
        renderGlyphBand(graphics, font, panel.x() + 14, panel.y() + 12, panel.width() - 28, true, alpha, baseSeed ^ 0x11L);
        renderGlyphBand(graphics, font, panel.x() + 14, panel.y() + panel.height() - 18, panel.width() - 28, true, alpha, baseSeed ^ 0x21L);
        renderGlyphBandVertical(graphics, font, panel.x() + 8, panel.y() + 18, panel.height() - 40, alpha, baseSeed ^ 0x31L);
        renderGlyphBandVertical(graphics, font, panel.x() + panel.width() - 14, panel.y() + 18, panel.height() - 40, alpha, baseSeed ^ 0x41L);
        renderCornerCluster(graphics, font, panel.x() + 12, panel.y() + 12, alpha, baseSeed ^ 0x51L);
        renderCornerCluster(graphics, font, panel.x() + panel.width() - 32, panel.y() + 12, alpha, baseSeed ^ 0x61L);
        renderCornerCluster(graphics, font, panel.x() + 12, panel.y() + panel.height() - 34, alpha, baseSeed ^ 0x71L);
        renderCornerCluster(graphics, font, panel.x() + panel.width() - 32, panel.y() + panel.height() - 34, alpha, baseSeed ^ 0x81L);
    }

    private void renderSkillGlyphIcon(GuiGraphics graphics, Font font, PanelLayout panel, BookProjectionView view, float alpha) {
        String icon = skillGlyphIcon(view.focusId());
        int iconColor = withAlpha(0xFF5E43B8, Math.max(alpha, 0.94f));
        int support = withAlpha(0xFFC4A673, Math.max(alpha, 0.62f));
        graphics.drawString(font, "\u27ea", panel.x() + 18, panel.y() + 20, support, false);
        graphics.drawString(font, icon, panel.x() + 29, panel.y() + 19, iconColor, false);
        graphics.drawString(font, "\u2058", panel.x() + 36, panel.y() + 29, withAlpha(0x9A8E7350, Math.max(alpha, 0.62f)), false);
        graphics.drawString(font, "\u27eb", panel.x() + 61, panel.y() + 20, support, false);
    }

    private void renderProjectionText(GuiGraphics graphics, Font font, PanelLayout panel, BookProjectionView view, float alpha) {
        int textX = panel.x() + 18;
        int textWidth = panel.width() - 36;
        int cursorY = panel.y() + 15;
        drawProjectionTitle(graphics, font, panel, view, cursorY, alpha);
        cursorY += 22;
        drawLargeCenteredText(graphics, font, view.dominantValue().getString(), panel.x(), panel.width(), cursorY, withAlpha(0xFF22140F, Math.max(alpha, 0.99f)), 2.6f);
        cursorY += 28;
        drawCenteredText(graphics, font, view.secondaryValue(), panel.x(), panel.width(), cursorY, withAlpha(0xFF654718, Math.max(alpha, 0.98f)));
        cursorY += 14;
        cursorY = drawWrappedText(graphics, font, view.description(), textX, cursorY, textWidth, withAlpha(0xFF2A2018, Math.max(alpha, 0.98f)), 2);
        cursorY += 6;
        renderProjectionProgressBar(graphics, font, panel, view, cursorY, alpha);
        cursorY += 22;
        int statusColor = view.emphasizedStatus() ? withAlpha(0xFF5E2781, Math.max(alpha, 0.99f)) : withAlpha(0xFF4A3829, Math.max(alpha, 0.97f));
        drawCenteredText(graphics, font, view.statusLabel(), panel.x(), panel.width(), cursorY, statusColor);
    }

    private void renderProjectionProgressBar(GuiGraphics graphics, Font font, PanelLayout panel, BookProjectionView view, int y, float alpha) {
        int barX = panel.x() + 18;
        int barWidth = panel.width() - 36;
        int back = withAlpha(0x8864533A, Math.max(alpha, 0.86f));
        int interference = withAlpha(0x2C7E5FD1, Math.max(alpha, 0.75f));
        graphics.fill(barX, y, barX + barWidth, y + 10, back);
        for (int stripe = 0; stripe < barWidth; stripe += 14) {
            graphics.fill(barX + stripe, y + 1, Math.min(barX + stripe + 5, barX + barWidth), y + 9, interference);
        }
        drawProgressBar(graphics, barX, y + 1, barWidth, 8, view.progress(), alpha);
        drawCenteredText(graphics, font, view.progressLabel(), panel.x(), panel.width(), y + 14, withAlpha(0xFF38261B, Math.max(alpha, 0.98f)));
    }

    private void drawProjectionButton(GuiGraphics graphics, Font font, PanelLayout panel, BookProjectionView view, ProjectionButtonHit hoveredButton, float alpha) {
        ProjectionButtonHit button = hoveredButton;
        if (button == null) {
            int buttonWidth = Math.min(120, panel.width() - 32);
            int buttonHeight = 16;
            int buttonX = panel.x() + (panel.width() - buttonWidth) / 2;
            int buttonY = panel.y() + panel.height() - 28;
            button = new ProjectionButtonHit(view, buttonX, buttonY, buttonWidth, buttonHeight);
        }
        boolean hovered = hoveredButton != null;
        int base = withAlpha(hovered ? 0xDAE4D8F8 : 0xD8E4D7C4, Math.max(alpha, 0.98f));
        int inner = withAlpha(hovered ? 0x346A4FC4 : 0x1E8A7354, Math.max(alpha, 0.9f));
        int accent = withAlpha(hovered ? 0xFF7F5ED1 : 0xFF9E7C52, Math.max(alpha, 0.98f));
        graphics.fill(button.x(), button.y() + 2, button.x() + button.width(), button.y() + button.height() - 2, base);
        graphics.fill(button.x() + 4, button.y() + 5, button.x() + button.width() - 4, button.y() + button.height() - 5, inner);
        graphics.fill(button.x() + 10, button.y() + 2, button.x() + button.width() - 10, button.y() + 3, accent);
        graphics.fill(button.x() + 10, button.y() + button.height() - 3, button.x() + button.width() - 10, button.y() + button.height() - 2, accent);
        graphics.drawString(font, "\u27ea", button.x() + 6, button.y() + 4, accent, false);
        graphics.drawString(font, "\u27eb", button.x() + button.width() - 12, button.y() + 4, accent, false);
        drawCenteredText(graphics, font, button.view().primaryActionLabel(), button.x(), button.width(), button.y() + 4, withAlpha(0xFF24160F, Math.max(alpha, 0.99f)));
    }

    private void renderGlyphBand(GuiGraphics graphics, Font font, int x, int y, int width, boolean top, float alpha, long seedBase) {
        int step = 12;
        for (int offset = 0; offset < width; offset += step) {
            long seed = mix64(seedBase + offset);
            if (((seed >>> 8) & 3L) == 0L) {
                continue;
            }
            String glyph = PROJECTION_GLYPHS[Math.floorMod((int) (seed >>> 32), PROJECTION_GLYPHS.length)];
            int drawY = y + (top ? Math.floorMod((int) (seed >>> 18), 3) : -Math.floorMod((int) (seed >>> 18), 3));
            int color = withAlpha((offset % 24 == 0) ? 0xFF7657C8 : 0xFF8A744F, Math.max(alpha, 0.8f) * (((seed >>> 12) & 1L) == 0L ? 0.28f : 0.18f));
            graphics.drawString(font, glyph, x + offset, drawY, color, false);
        }
    }

    private void renderGlyphBandVertical(GuiGraphics graphics, Font font, int x, int y, int height, float alpha, long seedBase) {
        int step = 11;
        for (int offset = 0; offset < height; offset += step) {
            long seed = mix64(seedBase + offset);
            if (((seed >>> 9) & 3L) == 0L) {
                continue;
            }
            String glyph = PROJECTION_GLYPHS[Math.floorMod((int) (seed >>> 28), PROJECTION_GLYPHS.length)];
            int drawX = x + Math.floorMod((int) (seed >>> 16), 3);
            int color = withAlpha((offset % 22 == 0) ? 0xFF7657C8 : 0xFF8A744F, Math.max(alpha, 0.8f) * (((seed >>> 13) & 1L) == 0L ? 0.24f : 0.15f));
            graphics.drawString(font, glyph, drawX, y + offset, color, false);
        }
    }

    private void renderCornerCluster(GuiGraphics graphics, Font font, int x, int y, float alpha, long seedBase) {
        for (int index = 0; index < 5; index++) {
            long seed = mix64(seedBase + (index * 17L));
            String glyph = PROJECTION_GLYPHS[Math.floorMod((int) (seed >>> 24), PROJECTION_GLYPHS.length)];
            int drawX = x + Math.floorMod((int) seed, 12);
            int drawY = y + Math.floorMod((int) (seed >>> 12), 10);
            int color = withAlpha((index == 0) ? 0xFF7B5FD1 : 0xFF967A51, Math.max(alpha, 0.8f) * (index == 0 ? 0.32f : 0.18f));
            graphics.drawString(font, glyph, drawX, drawY, color, false);
        }
    }

    private void renderArcaneDivider(GuiGraphics graphics, Font font, PanelLayout panel, int y, int accent, int support) {
        int centerX = panel.x() + panel.width() / 2;
        graphics.fill(panel.x() + 52, y + 4, centerX - 26, y + 5, withAlpha(support, 0.72f));
        graphics.fill(centerX + 28, y + 4, panel.x() + panel.width() - 40, y + 5, withAlpha(support, 0.54f));
        graphics.drawString(font, "\u27ea \u2058", panel.x() + 20, y, withAlpha(accent, 0.92f), false);
        graphics.drawString(font, "\u2058 \u2726 \u2058", centerX - 14, y, accent, false);
        graphics.drawString(font, "\u2234", panel.x() + panel.width() - 22, y, withAlpha(support, 0.74f), false);
    }

    private void drawProjectionTitle(GuiGraphics graphics, Font font, PanelLayout panel, BookProjectionView view, int y, float alpha) {
        int titleColor = withAlpha(0xFF311043, Math.max(alpha, 0.995f));
        int haloColor = withAlpha(0xE0EEE0C0, Math.max(alpha, 0.74f));
        int support = withAlpha(0xB28E7350, Math.max(alpha, 0.72f));
        int titleWidth = font.width(view.title());
        int titleX = panel.x() + (panel.width() - titleWidth) / 2;
        graphics.fill(panel.x() + 54, y + 10, panel.x() + panel.width() - 54, y + 11, withAlpha(0x1A8B7251, alpha));
        graphics.drawString(font, "\u2058", titleX - 12, y, support, false);
        graphics.drawString(font, view.title(), titleX + 1, y + 1, haloColor, false);
        graphics.drawString(font, view.title(), titleX, y, titleColor, false);
        graphics.drawString(font, "\u2058", titleX + titleWidth + 6, y, support, false);
    }

    private void renderCornerMark(GuiGraphics graphics, Font font, int x, int y, boolean rightSide, int outer, int inner) {
        if (rightSide) {
            graphics.drawString(font, "\u27eb", x, y, outer, false);
            graphics.drawString(font, "\u2058", x - 8, y + 4, inner, false);
        } else {
            graphics.drawString(font, "\u27ea", x, y, outer, false);
            graphics.drawString(font, "\u2058", x + 8, y + 4, inner, false);
        }
    }

    private void drawCenteredText(GuiGraphics graphics, Font font, Component text, int x, int width, int y, int color) {
        int textWidth = font.width(text);
        graphics.drawString(font, text, x + (width - textWidth) / 2, y, color, false);
    }

    private void drawLargeCenteredText(GuiGraphics graphics, Font font, String text, int x, int width, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        float scaledWidth = font.width(text);
        float drawX = (x + (width / 2.0f)) / scale - (scaledWidth / 2.0f);
        float drawY = y / scale;
        graphics.drawString(font, text, Math.round(drawX), Math.round(drawY), color, false);
        graphics.pose().popPose();
    }

    private int drawWrappedText(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            graphics.drawString(font, lines.get(i), x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, float alpha) {
        int clampedProgress = Mth.floor(width * progress);
        graphics.fill(x, y, x + width, y + height, withAlpha(0xCC7D6A52, Math.max(alpha, 0.95f)));
        graphics.fill(x, y, x + clampedProgress, y + height, withAlpha(0xFF7A3CC2, Math.max(alpha, 0.99f)));
        graphics.fill(x, y, x + width, y + 1, withAlpha(0xFFF0D48A, Math.max(alpha, 0.98f)));
        graphics.fill(x, y + height - 1, x + width, y + height, withAlpha(0xFF5A4E3A, Math.max(alpha, 0.95f)));
    }

    private String skillGlyphIcon(String focusId) {
        return switch (focusId == null ? "" : focusId.toLowerCase()) {
            case "valor" -> "\u2020\u2058\u2726";
            case "vitality" -> "\u2756+\u2058";
            case "mining" -> "\u27e1\u2058\u2303";
            case "culinary" -> "\u2058\u2727\u2058";
            case "forging" -> "\u2021\u2058\u2303";
            case "artificing" -> "\u203b\u2058\u2299";
            case "magick" -> "\u2726\u2058\u27e3";
            case "exploration" -> "\u2303\u2058\u2727";
            default -> "\u2726\u2058\u27e1";
        };
    }

    private static int withAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private PanelLayout projectionPanelLayout(int screenWidth, int screenHeight) {
        int width = Math.min(240, screenWidth - 36);
        int height = 152;
        int x = (screenWidth - width) / 2;
        int y = Math.max(16, ((screenHeight - height) / 2) - 12);
        return new PanelLayout(x, y, width, height);
    }

    private record PanelLayout(int x, int y, int width, int height) {}
}
