package net.zoogle.enchiridion.client.render.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public final class RadarChartElementRenderer {
    private static final java.awt.Font PAGE_TEXTURE_FONT = new java.awt.Font("Serif", java.awt.Font.PLAIN, 14);

    private RadarChartElementRenderer() {}

    public static void renderRadarChart(GuiGraphics graphics, BookPageElement.RadarChartElement chart, float textAlpha) {
        List<Float> values = chart.values();
        int n = values.size();
        if (n < 3) return;
        int cx = chart.x() + chart.width() / 2;
        int cy = chart.y() + chart.height() / 2;
        int radius = Math.min(chart.width(), chart.height()) / 2 - 2;
        if (radius < 4) return;
        int axisColor = applyAlpha(chart.axisColor(), textAlpha);
        int strokeColor = applyAlpha(chart.strokeColor(), textAlpha);
        int fillColor = applyAlpha(chart.fillColor(), textAlpha);
        int gridColor = applyAlpha(chart.gridColor(), textAlpha);
        int masteryFill = applyAlpha(chart.masteryFillColor(), textAlpha);
        int nextRingStroke = applyAlpha(chart.nextMasteryRingColor(), textAlpha);
        int[] outerXs = new int[n], outerYs = new int[n];
        int[] dataXs = new int[n], dataYs = new int[n];
        int[] nextRingXs = new int[n], nextRingYs = new int[n];
        int[] masteryXs = new int[n], masteryYs = new int[n];
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / n;
            outerXs[i] = cx + (int) Math.round(radius * Math.cos(angle));
            outerYs[i] = cy + (int) Math.round(radius * Math.sin(angle));
        }
        buildRadarLayerPolygon(n, cx, cy, radius, values, dataXs, dataYs);
        List<Float> nextRing = chart.nextMasteryRingValues();
        List<Float> masteryLevel = chart.masteryLevelValues();
        if (nextRing.size() == n) {
            buildRadarLayerPolygon(n, cx, cy, radius, nextRing, nextRingXs, nextRingYs);
        }
        if (masteryLevel.size() == n) {
            buildRadarLayerPolygon(n, cx, cy, radius, masteryLevel, masteryXs, masteryYs);
        }
        for (int ring = 1; ring <= 5; ring++) {
            int r = Math.max(2, radius * ring / 5);
            drawEllipseGui(graphics, cx, cy, r, gridColor);
        }
        for (int i = 0; i < n; i++) {
            PageCanvasRenderer.drawLineGui(graphics, cx, cy, outerXs[i], outerYs[i], axisColor);
        }
        fillPolygonGui(graphics, dataXs, dataYs, n, fillColor);
        if (nextRing.size() == n) {
            drawPolygonOutlineGui(graphics, nextRingXs, nextRingYs, n, nextRingStroke);
        }
        if (masteryLevel.size() == n) {
            drawPolygonOutlineGui(graphics, masteryXs, masteryYs, n, masteryFill);
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            PageCanvasRenderer.drawLineGui(graphics, dataXs[i], dataYs[i], dataXs[j], dataYs[j], strokeColor);
        }
        for (int i = 0; i < n; i++) {
            graphics.fill(dataXs[i] - 1, dataYs[i] - 1, dataXs[i] + 2, dataYs[i] + 2, strokeColor);
        }
    }

    public static void renderRadarChart(Graphics2D graphics, BookPageElement.RadarChartElement chart, float textAlpha) {
        List<Float> values = chart.values();
        List<String> labels = chart.labels();
        int n = values.size();
        if (n < 3) return;
        int cx = chart.x() + chart.width() / 2;
        int cy = chart.y() + chart.height() / 2;
        int radius = Math.min(chart.width(), chart.height()) / 2 - 2;
        if (radius < 4) return;
        for (int ring = 1; ring <= 5; ring++) {
            int r = Math.max(2, radius * ring / 5);
            graphics.setColor(new Color(applyAlpha(chart.gridColor(), textAlpha), true));
            graphics.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
        graphics.setColor(new Color(applyAlpha(chart.axisColor(), textAlpha), true));
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / n;
            int ax = cx + (int) Math.round(radius * Math.cos(angle));
            int ay = cy + (int) Math.round(radius * Math.sin(angle));
            graphics.drawLine(cx, cy, ax, ay);
        }
        int[] mainXs = new int[n], mainYs = new int[n];
        buildRadarLayerPolygon(n, cx, cy, radius, values, mainXs, mainYs);
        List<Float> nextRingList = chart.nextMasteryRingValues();
        List<Float> masteryLevel = chart.masteryLevelValues();
        int[] nextRingXs = new int[n], nextRingYs = new int[n];
        int[] masteryXs = new int[n], masteryYs = new int[n];
        if (nextRingList.size() == n) {
            buildRadarLayerPolygon(n, cx, cy, radius, nextRingList, nextRingXs, nextRingYs);
        }
        if (masteryLevel.size() == n) {
            buildRadarLayerPolygon(n, cx, cy, radius, masteryLevel, masteryXs, masteryYs);
        }
        graphics.setColor(new Color(applyAlpha(chart.fillColor(), textAlpha), true));
        graphics.fillPolygon(mainXs, mainYs, n);
        if (nextRingList.size() == n) {
            graphics.setColor(new Color(applyAlpha(chart.nextMasteryRingColor(), textAlpha), true));
            graphics.drawPolygon(nextRingXs, nextRingYs, n);
        }
        if (masteryLevel.size() == n) {
            graphics.setColor(new Color(applyAlpha(chart.masteryFillColor(), textAlpha), true));
            graphics.drawPolygon(masteryXs, masteryYs, n);
        }
        graphics.setColor(new Color(applyAlpha(chart.strokeColor(), textAlpha), true));
        graphics.drawPolygon(mainXs, mainYs, n);
        for (int i = 0; i < n; i++) {
            graphics.fillOval(mainXs[i] - 1, mainYs[i] - 1, 3, 3);
        }
        if (!labels.isEmpty()) {
            java.awt.Font labelFont = PAGE_TEXTURE_FONT.deriveFont(java.awt.Font.PLAIN, 11.0f);
            java.awt.FontMetrics fm = graphics.getFontMetrics(labelFont);
            java.awt.Font prevFont = graphics.getFont();
            graphics.setFont(labelFont);
            int labelOffset = radius + 6;
            for (int i = 0; i < Math.min(n, labels.size()); i++) {
                double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / n;
                int lx = cx + (int) Math.round(labelOffset * Math.cos(angle));
                int ly = cy + (int) Math.round(labelOffset * Math.sin(angle));
                String label = labels.get(i);
                int labelW = fm.stringWidth(label);
                graphics.setColor(new Color(applyAlpha(chart.axisColor(), textAlpha * 1.2f), true));
                graphics.drawString(label, lx - labelW / 2, ly + fm.getAscent() / 2);
            }
            graphics.setFont(prevFont);
        }
    }

    private static void drawPolygonOutlineGui(GuiGraphics graphics, int[] xs, int[] ys, int n, int color) {
        if (n < 2) {
            return;
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            PageCanvasRenderer.drawLineGui(graphics, xs[i], ys[i], xs[j], ys[j], color);
        }
    }

    private static void buildRadarLayerPolygon(
            int n,
            int cx,
            int cy,
            int radius,
            List<Float> valueRing,
            int[] xs,
            int[] ys) {
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / n;
            float v = 0.0f;
            if (valueRing != null && i < valueRing.size()) {
                v = valueRing.get(i);
            }
            v = Math.max(0.0f, Math.min(1.0f, v));
            int r = Math.max(1, (int) Math.round(radius * v));
            xs[i] = cx + (int) Math.round(r * Math.cos(angle));
            ys[i] = cy + (int) Math.round(r * Math.sin(angle));
        }
    }

    private static void drawEllipseGui(GuiGraphics graphics, int cx, int cy, int r, int color) {
        int x = 0, y = r, d = 3 - 2 * r;
        while (x <= y) {
            for (int[] pt : new int[][]{{cx+x,cy+y},{cx-x,cy+y},{cx+x,cy-y},{cx-x,cy-y},{cx+y,cy+x},{cx-y,cy+x},{cx+y,cy-x},{cx-y,cy-x}}) {
                graphics.fill(pt[0], pt[1], pt[0] + 1, pt[1] + 1, color);
            }
            if (d < 0) d += 4 * x + 6;
            else { d += 4 * (x - y) + 10; y--; }
            x++;
        }
    }

    private static void fillPolygonGui(GuiGraphics graphics, int[] xs, int[] ys, int n, int color) {
        if (n < 3) return;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) { minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]); }
        for (int y = minY; y <= maxY; y++) {
            java.util.List<Integer> xi = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                int y0 = ys[i], y1 = ys[j];
                if ((y0 <= y && y1 > y) || (y1 <= y && y0 > y)) {
                    xi.add(xs[i] + (y - y0) * (xs[j] - xs[i]) / (y1 - y0));
                }
            }
            java.util.Collections.sort(xi);
            for (int k = 0; k + 1 < xi.size(); k += 2) {
                if (xi.get(k + 1) > xi.get(k)) {
                    graphics.fill(xi.get(k), y, xi.get(k + 1) + 1, y + 1, color);
                }
            }
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
