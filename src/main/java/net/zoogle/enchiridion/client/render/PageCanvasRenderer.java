package net.zoogle.enchiridion.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookTextBlock;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public final class PageCanvasRenderer {
    public static final int PAGE_MARGIN_X = 10;
    public static final int PAGE_MARGIN_Y = 10;
    public static final int RIGHT_PAGE_INSET = 8;
    public static final int LINE_SPACING = 2;
    public static final int TITLE_LINE_HEIGHT = 11;
    public static final int SUBTITLE_LINE_HEIGHT = 10;
    public static final int BODY_LINE_HEIGHT = 9;
    public static final int BLOCK_SPACING = 6;

    private final Font font = Minecraft.getInstance().font;

    private record RenderResult(int y, boolean overflowed) {}

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height) {
        renderPage(graphics, page, x, y, width, height, 1.0f, 0);
    }

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height, float textAlpha) {
        renderPage(graphics, page, x, y, width, height, textAlpha, 0);
    }

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset) {
        graphics.fill(x, y, x + width, y + height, 0x10FFFFFF);

        int contentX = x + PAGE_MARGIN_X + horizontalInset;
        int contentY = y + PAGE_MARGIN_Y;
        int contentWidth = Math.max(0, width - (PAGE_MARGIN_X * 2) - horizontalInset);
        int contentBottom = y + height - PAGE_MARGIN_Y;

        int cursorY = contentY;
        List<BookTextBlock> blocks = page.blocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BookTextBlock block = blocks.get(blockIndex);
            RenderResult result = renderBlock(graphics, block, contentX, cursorY, contentWidth, contentBottom, textAlpha);
            cursorY = result.y();
            if (result.overflowed() || blockIndex < blocks.size() - 1 && cursorY + BLOCK_SPACING >= contentBottom) {
                drawOverflowIndicator(graphics, contentX, contentBottom, contentWidth, textAlpha);
                break;
            }
            cursorY += BLOCK_SPACING;
            if (cursorY >= contentBottom) {
                if (blockIndex < blocks.size() - 1) {
                    drawOverflowIndicator(graphics, contentX, contentBottom, contentWidth, textAlpha);
                }
                break;
            }
        }
    }

    private RenderResult renderBlock(GuiGraphics graphics, BookTextBlock block, int x, int y, int width, int bottomY, float textAlpha) {
        if (width <= 0 || y >= bottomY) {
            return new RenderResult(y, true);
        }

        int color = applyAlpha(colorFor(block.kind()), textAlpha);

        List<net.minecraft.util.FormattedCharSequence> lines = font.split(block.text(), width);
        int lineHeight = lineHeightFor(block.kind());

        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (y + lineHeight > bottomY) {
                return new RenderResult(y, true);
            }
            graphics.drawString(font, line, x, y, color, false);
            y += lineHeight;
        }
        return new RenderResult(y, false);
    }

    public void renderPageToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height) {
        graphics.setColor(new Color(0x10, 0x10, 0x10, 28));
        graphics.fillRect(x, y, width, height);
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height) {
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, 1.0f, 0);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height, float textAlpha) {
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, textAlpha, 0);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset) {
        int contentX = x + PAGE_MARGIN_X + horizontalInset;
        int contentY = y + PAGE_MARGIN_Y;
        int contentWidth = Math.max(0, width - (PAGE_MARGIN_X * 2) - horizontalInset);
        int contentBottom = y + height - PAGE_MARGIN_Y;

        int cursorY = contentY;
        List<BookTextBlock> blocks = page.blocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BookTextBlock block = blocks.get(blockIndex);
            RenderResult result = renderBlockToGraphics2D(graphics, block, contentX, cursorY, contentWidth, contentBottom, textAlpha);
            cursorY = result.y();
            if (result.overflowed() || blockIndex < blocks.size() - 1 && cursorY + BLOCK_SPACING >= contentBottom) {
                drawOverflowIndicatorToGraphics2D(graphics, contentX, contentBottom, contentWidth, textAlpha);
                break;
            }
            cursorY += BLOCK_SPACING;
            if (cursorY >= contentBottom) {
                if (blockIndex < blocks.size() - 1) {
                    drawOverflowIndicatorToGraphics2D(graphics, contentX, contentBottom, contentWidth, textAlpha);
                }
                break;
            }
        }
    }

    private RenderResult renderBlockToGraphics2D(Graphics2D graphics, BookTextBlock block, int x, int y, int width, int bottomY, float textAlpha) {
        if (width <= 0 || y >= bottomY) {
            return new RenderResult(y, true);
        }

        graphics.setColor(new Color(applyAlpha(colorFor(block.kind()), textAlpha), true));
        int lineHeight = lineHeightFor(block.kind());

        for (String line : wrapPlainText(graphics, block.text().getString(), width)) {
            if (y + lineHeight > bottomY) {
                return new RenderResult(y, true);
            }
            graphics.drawString(line, x, y + lineHeight - 2);
            y += lineHeight;
        }
        return new RenderResult(y, false);
    }

    private List<String> wrapPlainText(Graphics2D graphics, String text, int width) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (graphics.getFontMetrics().stringWidth(candidate) <= width) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                lines.add(word);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static int lineHeightFor(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> TITLE_LINE_HEIGHT;
            case SUBTITLE -> SUBTITLE_LINE_HEIGHT;
            case BODY -> BODY_LINE_HEIGHT;
        } + LINE_SPACING;
    }

    private static int colorFor(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> 0xFF3A2314;
            case SUBTITLE -> 0xFF5A3F29;
            case BODY -> 0xFF2A2118;
        };
    }

    private static int applyAlpha(int color, float alpha) {
        int clampedAlpha = Math.clamp(Math.round(Math.clamp(alpha, 0.0f, 1.0f) * 255.0f), 0, 255);
        return (color & 0x00FFFFFF) | (clampedAlpha << 24);
    }

    private void drawOverflowIndicator(GuiGraphics graphics, int contentX, int contentBottom, int contentWidth, float textAlpha) {
        String marker = "...";
        int color = applyAlpha(colorFor(BookTextBlock.Kind.BODY), textAlpha);
        int markerY = contentBottom - lineHeightFor(BookTextBlock.Kind.BODY);
        int markerX = contentX + Math.max(0, contentWidth - font.width(marker));
        graphics.drawString(font, marker, markerX, markerY, color, false);
    }

    private void drawOverflowIndicatorToGraphics2D(Graphics2D graphics, int contentX, int contentBottom, int contentWidth, float textAlpha) {
        String marker = "...";
        graphics.setColor(new Color(applyAlpha(colorFor(BookTextBlock.Kind.BODY), textAlpha), true));
        int markerY = contentBottom - lineHeightFor(BookTextBlock.Kind.BODY) + BODY_LINE_HEIGHT - 2;
        int markerX = contentX + Math.max(0, contentWidth - graphics.getFontMetrics().stringWidth(marker));
        graphics.drawString(marker, markerX, markerY);
    }

    public Component footerText(int spreadIndex) {
        return Component.literal("Spread " + (spreadIndex + 1));
    }
}
