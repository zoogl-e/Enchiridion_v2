package net.zoogle.enchiridion.client.render.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

public final class SkillTreeCanvasWidgetRenderer {
    private SkillTreeCanvasWidgetRenderer() {}

    public static void renderSkillTreeWidget(GuiGraphics graphics, BookPageElement.WidgetElement widget, float textAlpha) {
        Map<String, String> payload = parseWidgetPayload(widget.label().getString());
        boolean left = "L".equalsIgnoreCase(payload.getOrDefault("side", "L"));
        int unlocked = parseInt(payload.get("unlocked"), 3);
        int seed = parseInt(payload.get("seed"), 0);

        int panelFill = applyAlpha(0x06C4B79D, textAlpha);
        int panelBorder = applyAlpha(0x6A584229, textAlpha);
        graphics.fill(widget.x(), widget.y(), widget.x() + widget.width(), widget.y() + widget.height(), panelFill);
        graphics.fill(widget.x(), widget.y(), widget.x() + widget.width(), widget.y() + 1, panelBorder);
        graphics.fill(widget.x(), widget.y() + widget.height() - 1, widget.x() + widget.width(), widget.y() + widget.height(), panelBorder);
        graphics.fill(widget.x(), widget.y(), widget.x() + 1, widget.y() + widget.height(), panelBorder);
        graphics.fill(widget.x() + widget.width() - 1, widget.y(), widget.x() + widget.width(), widget.y() + widget.height(), panelBorder);

        int[][] nodes = skillTreeNodes(widget, left, seed);
        int[][] edges = skillTreeEdges(left);
        for (int[] edge : edges) {
            PageCanvasRenderer.drawLineGui(graphics, nodes[edge[0]][0], nodes[edge[0]][1], nodes[edge[1]][0], nodes[edge[1]][1], applyAlpha(0xCC4F3B24, textAlpha));
        }
        for (int i = 0; i < nodes.length; i++) {
            boolean isUnlocked = i < unlocked;
            drawSkillTreeNode(graphics, nodes[i][0], nodes[i][1], isUnlocked, textAlpha);
        }
    }

    public static void renderSkillTreeWidget(Graphics2D graphics, BookPageElement.WidgetElement widget, float textAlpha) {
        Map<String, String> payload = parseWidgetPayload(widget.label().getString());
        boolean left = "L".equalsIgnoreCase(payload.getOrDefault("side", "L"));
        int unlocked = parseInt(payload.get("unlocked"), 3);
        int seed = parseInt(payload.get("seed"), 0);

        graphics.setColor(new Color(applyAlpha(0x06C4B79D, textAlpha), true));
        graphics.fillRect(widget.x(), widget.y(), widget.width(), widget.height());
        graphics.setColor(new Color(applyAlpha(0x6A584229, textAlpha), true));
        graphics.drawRect(widget.x(), widget.y(), Math.max(1, widget.width() - 1), Math.max(1, widget.height() - 1));

        int[][] nodes = skillTreeNodes(widget, left, seed);
        int[][] edges = skillTreeEdges(left);
        graphics.setColor(new Color(applyAlpha(0xCC4F3B24, textAlpha), true));
        for (int[] edge : edges) {
            graphics.drawLine(nodes[edge[0]][0], nodes[edge[0]][1], nodes[edge[1]][0], nodes[edge[1]][1]);
        }
        for (int i = 0; i < nodes.length; i++) {
            boolean isUnlocked = i < unlocked;
            drawSkillTreeNode(graphics, nodes[i][0], nodes[i][1], isUnlocked, textAlpha);
        }
    }

    private static void drawSkillTreeNode(GuiGraphics graphics, int centerX, int centerY, boolean unlocked, float textAlpha) {
        int border = applyAlpha(unlocked ? 0xFFC0934D : 0xFF5A4B36, textAlpha);
        int fill = applyAlpha(unlocked ? 0xFF356547 : 0xFF2A2927, textAlpha);
        int glyph = applyAlpha(unlocked ? 0xFFE5F1E8 : 0xFF9A8F82, textAlpha);
        int x = centerX - 4;
        int y = centerY - 4;
        graphics.fill(x, y, x + 9, y + 9, border);
        graphics.fill(x + 1, y + 1, x + 8, y + 8, fill);
        graphics.fill(x + 4, y + 2, x + 5, y + 7, glyph);
        graphics.fill(x + 2, y + 4, x + 7, y + 5, glyph);
    }

    private static void drawSkillTreeNode(Graphics2D graphics, int centerX, int centerY, boolean unlocked, float textAlpha) {
        int border = applyAlpha(unlocked ? 0xFFC0934D : 0xFF5A4B36, textAlpha);
        int fill = applyAlpha(unlocked ? 0xFF356547 : 0xFF2A2927, textAlpha);
        int glyph = applyAlpha(unlocked ? 0xFFE5F1E8 : 0xFF9A8F82, textAlpha);
        int x = centerX - 4;
        int y = centerY - 4;
        graphics.setColor(new Color(border, true));
        graphics.fillRect(x, y, 9, 9);
        graphics.setColor(new Color(fill, true));
        graphics.fillRect(x + 1, y + 1, 7, 7);
        graphics.setColor(new Color(glyph, true));
        graphics.fillRect(x + 4, y + 2, 1, 5);
        graphics.fillRect(x + 2, y + 4, 5, 1);
    }

    private static int[][] skillTreeNodes(BookPageElement.WidgetElement widget, boolean left, int seed) {
        int x = widget.x();
        int y = widget.y();
        int jitter = Math.floorMod(seed, 3) - 1;
        if (left) {
            return new int[][]{
                    {x + 10, y + 12 + jitter},
                    {x + 26, y + 17},
                    {x + 42, y + 22 - jitter},
                    {x + 14, y + 32},
                    {x + 34, y + 38 + jitter},
                    {x + 20, y + 52},
                    {x + 46, y + 58},
                    {x + 30, y + 72}
            };
        }
        return new int[][]{
                {x + 8, y + 14},
                {x + 24, y + 19 - jitter},
                {x + 40, y + 13},
                {x + 12, y + 34 + jitter},
                {x + 30, y + 40},
                {x + 48, y + 36},
                {x + 18, y + 58},
                {x + 40, y + 66 - jitter}
        };
    }

    private static int[][] skillTreeEdges(boolean left) {
        if (left) {
            return new int[][]{
                    {0, 1}, {1, 2}, {0, 3}, {1, 4}, {2, 4}, {3, 5}, {4, 6}, {5, 7}, {6, 7}
            };
        }
        return new int[][]{
                {0, 1}, {1, 2}, {0, 3}, {1, 4}, {2, 5}, {3, 6}, {4, 6}, {4, 7}, {5, 7}
        };
    }

    private static Map<String, String> parseWidgetPayload(String payload) {
        Map<String, String> values = new HashMap<>();
        if (payload == null || payload.isBlank()) {
            return values;
        }
        for (String entry : payload.split(";")) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            values.put(parts[0].trim().toLowerCase(), parts[1].trim());
        }
        return values;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.clamp(
                Math.round(sourceAlpha * Math.clamp(alpha, 0.0f, 1.0f)),
                0,
                255
        );
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }
}
