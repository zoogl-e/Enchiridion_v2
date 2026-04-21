package net.zoogle.enchiridion.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookTextBlock;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public final class PageCanvasRenderer {
    private static final boolean DEBUG_INTERACTIVE_TEXT_BOUNDS = false;
    public static final int PAGE_MARGIN_X = 10;
    public static final int PAGE_MARGIN_Y = 10;
    public static final int RIGHT_PAGE_INSET = 8;
    public static final int LINE_SPACING = 2;
    public static final int TITLE_LINE_HEIGHT = 11;
    public static final int SUBTITLE_LINE_HEIGHT = 10;
    public static final int LEVEL_LINE_HEIGHT = 18;
    public static final int BODY_LINE_HEIGHT = 9;
    public static final int SECTION_LINE_HEIGHT = 10;
    public static final int BLOCK_SPACING = 6;
    private static final ArcaneTextRenderer.TextRenderMode TEXT_RENDER_MODE = ArcaneTextRenderer.TextRenderMode.ENCHANTED_TRANSLATING;

    private final Font font = Minecraft.getInstance().font;
    private final ArcaneTextRenderer arcaneTextRenderer = new ArcaneTextRenderer();

    private record RenderResult(int y, boolean overflowed) {}
    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height) {
        renderPage(graphics, page, x, y, width, height, 1.0f, 0, null, 0.0f);
    }

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height, float textAlpha) {
        renderPage(graphics, page, x, y, width, height, textAlpha, 0, null, 0.0f);
    }

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset) {
        renderPage(graphics, page, x, y, width, height, textAlpha, horizontalInset, null, 0.0f);
    }

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset, Integer pageNumber) {
        renderPage(graphics, page, x, y, width, height, textAlpha, horizontalInset, pageNumber, 0.0f);
    }

    public void renderPage(GuiGraphics graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset, Integer pageNumber, float glitchStrength) {
        graphics.fill(x, y, x + width, y + height, 0x10FFFFFF);

        int contentX = x + PAGE_MARGIN_X + horizontalInset;
        int contentY = y + PAGE_MARGIN_Y;
        int contentWidth = Math.max(0, width - (PAGE_MARGIN_X * 2) - horizontalInset);
        int contentBottom = y + height - PAGE_MARGIN_Y;
        contentY = adjustedContentY(page, contentY, contentBottom);

        int cursorY = contentY;
        List<BookTextBlock> blocks = page.blocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BookTextBlock block = blocks.get(blockIndex);
            RenderResult result = renderBlock(graphics, block, contentX, cursorY, contentWidth, contentBottom, textAlpha, glitchStrength);
            cursorY = result.y();
            if (result.overflowed() || blockIndex < blocks.size() - 1 && cursorY + BLOCK_SPACING >= contentBottom) {
                break;
            }
            cursorY += BLOCK_SPACING;
            if (cursorY >= contentBottom) {
                break;
            }
        }
        renderPageElements(graphics, page.elements(), textAlpha, null);
        drawPageNumber(graphics, x, y, width, height, page, textAlpha, pageNumber);
    }

    private RenderResult renderBlock(GuiGraphics graphics, BookTextBlock block, int x, int y, int width, int bottomY, float textAlpha) {
        return renderBlock(graphics, block, x, y, width, bottomY, textAlpha, 0.0f);
    }

    private RenderResult renderBlock(GuiGraphics graphics, BookTextBlock block, int x, int y, int width, int bottomY, float textAlpha, float glitchStrength) {
        if (width <= 0 || y >= bottomY) {
            return new RenderResult(y, true);
        }

        int color = applyAlpha(colorFor(block.kind()), textAlpha);
        int lineHeight = lineHeightFor(block.kind());
        List<String> lines = wrapPlainText(block.text().getString(), width, font::width);

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (y + lineHeight > bottomY) {
                return new RenderResult(y, true);
            }
            renderLine(graphics, line, x, y, width, color, block.kind(), textAlpha, lineIndex, glitchStrength);
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
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, 1.0f, 0, null, 0.0f);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height, float textAlpha) {
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, textAlpha, 0, null, 0.0f);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset) {
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, textAlpha, horizontalInset, null, 0.0f);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset, Integer pageNumber) {
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, textAlpha, horizontalInset, pageNumber, 0.0f);
    }

    public void renderPageTextOnlyToGraphics2D(Graphics2D graphics, BookPage page, int x, int y, int width, int height, float textAlpha, int horizontalInset, Integer pageNumber, float glitchStrength) {
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, textAlpha, horizontalInset, pageNumber, glitchStrength, null);
    }

    public void renderPageTextOnlyToGraphics2D(
            Graphics2D graphics,
            BookPage page,
            int x,
            int y,
            int width,
            int height,
            float textAlpha,
            int horizontalInset,
            Integer pageNumber,
            float glitchStrength,
            BookPageElement.InteractiveTextElement hoveredInteractiveElement
    ) {
        int contentX = x + PAGE_MARGIN_X + horizontalInset;
        int contentY = y + PAGE_MARGIN_Y;
        int contentWidth = Math.max(0, width - (PAGE_MARGIN_X * 2) - horizontalInset);
        int contentBottom = y + height - PAGE_MARGIN_Y;
        contentY = adjustedContentY(page, contentY, contentBottom);

        int cursorY = contentY;
        List<BookTextBlock> blocks = page.blocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BookTextBlock block = blocks.get(blockIndex);
            RenderResult result = renderBlockToGraphics2D(graphics, block, contentX, cursorY, contentWidth, contentBottom, textAlpha, glitchStrength);
            cursorY = result.y();
            if (result.overflowed() || blockIndex < blocks.size() - 1 && cursorY + BLOCK_SPACING >= contentBottom) {
                break;
            }
            cursorY += BLOCK_SPACING;
            if (cursorY >= contentBottom) {
                break;
            }
        }
        renderPageElementsToGraphics2D(graphics, page.elements(), textAlpha, hoveredInteractiveElement);
        drawPageNumber(graphics, x, y, width, height, page, textAlpha, pageNumber);
    }

    private void renderPageElements(GuiGraphics graphics, List<BookPageElement> elements, float textAlpha, BookPageElement.InteractiveTextElement hoveredInteractiveElement) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        for (BookPageElement element : elements) {
            renderPageElement(graphics, element, textAlpha, hoveredInteractiveElement);
        }
    }

    private void renderPageElementsToGraphics2D(Graphics2D graphics, List<BookPageElement> elements, float textAlpha, BookPageElement.InteractiveTextElement hoveredInteractiveElement) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        for (BookPageElement element : elements) {
            renderPageElementToGraphics2D(graphics, element, textAlpha, hoveredInteractiveElement);
        }
    }

    private void renderPageElement(GuiGraphics graphics, BookPageElement element, float textAlpha, BookPageElement.InteractiveTextElement hoveredInteractiveElement) {
        switch (element) {
            case BookPageElement.TextElement text ->
                    renderLine(graphics, text.text().getString(), text.x(), text.y(), text.width(), applyAlpha(colorFor(text.kind()), textAlpha), text.kind(), textAlpha, 0, 0.0f);
            case BookPageElement.DecorationElement decoration ->
                    renderLine(graphics, decoration.text().getString(), decoration.x(), decoration.y(), decoration.width(), applyAlpha(colorFor(decoration.kind()), textAlpha), decoration.kind(), textAlpha, 0, 0.0f);
            case BookPageElement.InteractiveTextElement interactive ->
                    renderInteractiveTextElement(graphics, interactive, textAlpha, hoveredInteractiveElement != null && hoveredInteractiveElement.equals(interactive));
            case BookPageElement.BoxElement box -> renderBox(graphics, box, textAlpha);
            case BookPageElement.ImageElement ignored -> {
            }
            case BookPageElement.WidgetElement widget ->
                    renderLine(graphics, widget.label().getString(), widget.x(), widget.y(), widget.width(), applyAlpha(0xFF5A3F29, textAlpha), BookTextBlock.Kind.BODY, textAlpha, 0, 0.0f);
        }
    }

    private void renderPageElementToGraphics2D(Graphics2D graphics, BookPageElement element, float textAlpha, BookPageElement.InteractiveTextElement hoveredInteractiveElement) {
        switch (element) {
            case BookPageElement.TextElement text ->
                    renderLine(graphics, text.text().getString(), text.x(), text.y() + graphics.getFontMetrics().getAscent(), text.width(), applyAlpha(colorFor(text.kind()), textAlpha), text.kind(), textAlpha, 0, 0.0f);
            case BookPageElement.DecorationElement decoration ->
                    renderLine(graphics, decoration.text().getString(), decoration.x(), decoration.y() + graphics.getFontMetrics().getAscent(), decoration.width(), applyAlpha(colorFor(decoration.kind()), textAlpha), decoration.kind(), textAlpha, 0, 0.0f);
            case BookPageElement.InteractiveTextElement interactive ->
                    renderInteractiveTextElementToGraphics2D(graphics, interactive, textAlpha, hoveredInteractiveElement != null && hoveredInteractiveElement.equals(interactive));
            case BookPageElement.BoxElement box -> {
                graphics.setColor(new Color(applyAlpha(box.fillColor(), textAlpha), true));
                graphics.fillRect(box.x(), box.y(), box.width(), box.height());
                graphics.setColor(new Color(applyAlpha(box.borderColor(), textAlpha), true));
                graphics.drawRect(box.x(), box.y(), Math.max(1, box.width() - 1), Math.max(1, box.height() - 1));
            }
            case BookPageElement.ImageElement ignored -> {
            }
            case BookPageElement.WidgetElement widget ->
                    renderLine(graphics, widget.label().getString(), widget.x(), widget.y() + graphics.getFontMetrics().getAscent(), widget.width(), applyAlpha(0xFF5A3F29, textAlpha), BookTextBlock.Kind.BODY, textAlpha, 0, 0.0f);
        }
    }

    private void renderInteractiveTextElement(GuiGraphics graphics, BookPageElement.InteractiveTextElement element, float textAlpha, boolean hovered) {
        int defaultInk = applyAlpha(colorFor(element.kind()), textAlpha);
        graphics.drawString(font, element.text(), element.x(), element.y(), defaultInk, false);
        if (hovered) {
            int ink = applyAlpha(0xFF6C49CC, textAlpha);
            int support = applyAlpha(0xB0D7C89E, textAlpha);
            int underlineY = element.y() + Math.max(1, element.height() - 2);
            int leftInset = Math.min(3, Math.max(1, element.width() / 8));
            int rightInset = leftInset;
            int underlineX0 = element.x() + leftInset;
            int underlineX1 = element.x() + element.width() - rightInset;

            graphics.drawString(font, element.text(), element.x(), element.y(), ink, false);
            if (underlineX1 > underlineX0) {
                graphics.fill(underlineX0, underlineY, underlineX1, underlineY + 1, ink);
            }
            if (underlineX1 - underlineX0 > 6) {
                graphics.fill(underlineX0 + 2, underlineY + 2, underlineX1 - 2, underlineY + 3, support);
            }
            graphics.drawString(font, "\u2058", element.x() + element.width() + 2, element.y(), support, false);
            graphics.drawString(font, "\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(element.y(), underlineY - 5), support, false);
        }
    }

    private void renderInteractiveTextElementToGraphics2D(Graphics2D graphics, BookPageElement.InteractiveTextElement element, float textAlpha, boolean hovered) {
        int defaultInk = applyAlpha(colorFor(element.kind()), textAlpha);
        int baselineY = element.y() + graphics.getFontMetrics().getAscent();
        graphics.setColor(new Color(defaultInk, true));
        graphics.drawString(element.text().getString(), element.x(), baselineY);
        if (hovered) {
            int ink = applyAlpha(0xFF6C49CC, textAlpha);
            int support = applyAlpha(0xB0D7C89E, textAlpha);
            int underlineY = element.y() + Math.max(1, element.height() - 2);
            int leftInset = Math.min(3, Math.max(1, element.width() / 8));
            int rightInset = leftInset;
            int underlineX0 = element.x() + leftInset;
            int underlineX1 = element.x() + element.width() - rightInset;

            graphics.setColor(new Color(ink, true));
            graphics.drawString(element.text().getString(), element.x(), baselineY);
            if (underlineX1 > underlineX0) {
                graphics.fillRect(underlineX0, underlineY, underlineX1 - underlineX0, 1);
            }
            if (underlineX1 - underlineX0 > 6) {
                graphics.setColor(new Color(support, true));
                graphics.fillRect(underlineX0 + 2, underlineY + 2, Math.max(1, (underlineX1 - underlineX0) - 4), 1);
            }
            graphics.setColor(new Color(support, true));
            graphics.drawString("\u2058", element.x() + element.width() + 2, baselineY);
            graphics.drawString("\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(element.y() + graphics.getFontMetrics().getAscent() - 3, underlineY - 2));
        }
        if (DEBUG_INTERACTIVE_TEXT_BOUNDS) {
            graphics.setColor(new Color(hovered ? 0xCCAA33FF : 0xAA33CC66, true));
            graphics.drawRect(element.x(), element.y(), Math.max(1, element.width() - 1), Math.max(1, element.height() - 1));
        }
    }

    private void renderBox(GuiGraphics graphics, BookPageElement.BoxElement box, float textAlpha) {
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), applyAlpha(box.fillColor(), textAlpha));
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + 1, applyAlpha(box.borderColor(), textAlpha));
        graphics.fill(box.x(), box.y() + box.height() - 1, box.x() + box.width(), box.y() + box.height(), applyAlpha(box.borderColor(), textAlpha));
        graphics.fill(box.x(), box.y(), box.x() + 1, box.y() + box.height(), applyAlpha(box.borderColor(), textAlpha));
        graphics.fill(box.x() + box.width() - 1, box.y(), box.x() + box.width(), box.y() + box.height(), applyAlpha(box.borderColor(), textAlpha));
    }

    private RenderResult renderBlockToGraphics2D(Graphics2D graphics, BookTextBlock block, int x, int y, int width, int bottomY, float textAlpha) {
        return renderBlockToGraphics2D(graphics, block, x, y, width, bottomY, textAlpha, 0.0f);
    }

    private RenderResult renderBlockToGraphics2D(Graphics2D graphics, BookTextBlock block, int x, int y, int width, int bottomY, float textAlpha, float glitchStrength) {
        if (width <= 0 || y >= bottomY) {
            return new RenderResult(y, true);
        }

        int lineHeight = lineHeightFor(block.kind());
        int color = applyAlpha(colorFor(block.kind()), textAlpha);

        List<String> lines = wrapPlainText(block.text().getString(), width, text -> graphics.getFontMetrics().stringWidth(text));
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (y + lineHeight > bottomY) {
                return new RenderResult(y, true);
            }
            renderLine(graphics, line, x, y + lineHeight - 2, width, color, block.kind(), textAlpha, lineIndex, glitchStrength);
            y += lineHeight;
        }
        return new RenderResult(y, false);
    }

    private void renderLine(GuiGraphics graphics, String line, int x, int y, int width, int color, BookTextBlock.Kind kind, float textAlpha, int lineIndex) {
        renderLine(graphics, line, x, y, width, color, kind, textAlpha, lineIndex, 0.0f);
    }

    private void renderLine(GuiGraphics graphics, String line, int x, int y, int width, int color, BookTextBlock.Kind kind, float textAlpha, int lineIndex, float glitchStrength) {
        if (kind == BookTextBlock.Kind.LEVEL) {
            renderLargeLevelLine(graphics, line, x, y, width, color);
            return;
        }
        int drawX = alignedX(line, x, width, kind, font::width);
        if (TEXT_RENDER_MODE == ArcaneTextRenderer.TextRenderMode.ENCHANTED_TRANSLATING) {
            arcaneTextRenderer.renderGuiLine(
                    graphics,
                    font,
                    line,
                    drawX,
                    y,
                    color,
                    kind,
                    textAlpha,
                    ArcaneTextRenderer.lineSeed(line, kind, lineIndex),
                    glitchStrength
            );
            return;
        }

        graphics.drawString(font, line, drawX, y, color, false);
    }

    private void renderLine(Graphics2D graphics, String line, int x, int baselineY, int width, int color, BookTextBlock.Kind kind, float textAlpha, int lineIndex) {
        renderLine(graphics, line, x, baselineY, width, color, kind, textAlpha, lineIndex, 0.0f);
    }

    private void renderLine(Graphics2D graphics, String line, int x, int baselineY, int width, int color, BookTextBlock.Kind kind, float textAlpha, int lineIndex, float glitchStrength) {
        if (kind == BookTextBlock.Kind.LEVEL) {
            renderLargeLevelLine(graphics, line, x, baselineY, width, color);
            return;
        }
        int drawX = alignedX(line, x, width, kind, text -> graphics.getFontMetrics().stringWidth(text));
        if (TEXT_RENDER_MODE == ArcaneTextRenderer.TextRenderMode.ENCHANTED_TRANSLATING) {
            arcaneTextRenderer.renderGraphicsLine(
                    graphics,
                    line,
                    drawX,
                    baselineY,
                    color,
                    kind,
                    textAlpha,
                    ArcaneTextRenderer.lineSeed(line, kind, lineIndex),
                    glitchStrength
            );
            return;
        }

        graphics.setColor(new Color(color, true));
        graphics.drawString(line, drawX, baselineY);
    }

    private void renderLargeLevelLine(GuiGraphics graphics, String line, int x, int y, int width, int color) {
        float scale = 2.0f;
        int scaledWidth = Math.round(font.width(line) * scale);
        float drawX = x + Math.max(0, (width - scaledWidth) / 2.0f);
        graphics.pose().pushPose();
        graphics.pose().translate(drawX, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, line, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void renderLargeLevelLine(Graphics2D graphics, String line, int x, int baselineY, int width, int color) {
        java.awt.Font originalFont = graphics.getFont();
        java.awt.Font largeFont = originalFont.deriveFont(originalFont.getSize2D() * 2.0f);
        graphics.setFont(largeFont);
        graphics.setColor(new Color(color, true));
        int textWidth = graphics.getFontMetrics().stringWidth(line);
        int drawX = x + Math.max(0, (width - textWidth) / 2);
        graphics.drawString(line, drawX, baselineY);
        graphics.setFont(originalFont);
    }

    private List<String> wrapPlainText(String text, int width, java.util.function.ToIntFunction<String> widthMeasure) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        String[] paragraphs = text.split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.trim().split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (widthMeasure.applyAsInt(candidate) <= width) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }

                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(word);
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private static int lineHeightFor(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> TITLE_LINE_HEIGHT;
            case SUBTITLE -> SUBTITLE_LINE_HEIGHT;
            case LEVEL -> LEVEL_LINE_HEIGHT;
            case SECTION -> SECTION_LINE_HEIGHT;
            case BODY -> BODY_LINE_HEIGHT;
        } + LINE_SPACING;
    }

    private static int colorFor(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> 0xFF3A2314;
            case SUBTITLE -> 0xFF5A3F29;
            case LEVEL -> 0xFF2C1B10;
            case SECTION -> 0xFF6E542B;
            case BODY -> 0xFF2A2118;
        };
    }

    private static int applyAlpha(int color, float alpha) {
        int clampedAlpha = Math.clamp(Math.round(Math.clamp(alpha, 0.0f, 1.0f) * 255.0f), 0, 255);
        return (color & 0x00FFFFFF) | (clampedAlpha << 24);
    }

    private static int alignedX(String line, int x, int width, BookTextBlock.Kind kind, java.util.function.ToIntFunction<String> widthMeasure) {
        if (kind == BookTextBlock.Kind.TITLE) {
            return x + Math.max(0, (width - widthMeasure.applyAsInt(line)) / 2);
        }
        return x;
    }

    private static int adjustedContentY(BookPage page, int defaultContentY, int contentBottom) {
        if (!page.blocks().isEmpty() && page.blocks().stream().allMatch(block -> block.kind() == BookTextBlock.Kind.TITLE)) {
            int centeredHeight = 0;
            for (int index = 0; index < page.blocks().size(); index++) {
                if (index > 0) {
                    centeredHeight += BLOCK_SPACING;
                }
                centeredHeight += lineHeightFor(BookTextBlock.Kind.TITLE);
            }
            return defaultContentY + Math.max(0, ((contentBottom - defaultContentY) - centeredHeight) / 2);
        }
        return defaultContentY;
    }

    public Component footerText(int spreadIndex) {
        return Component.literal("Spread " + (spreadIndex + 1));
    }

    private void drawPageNumber(GuiGraphics graphics, int x, int y, int width, int height, BookPage page, float textAlpha, Integer pageNumber) {
        if (pageNumber == null || page.blocks().isEmpty()) {
            return;
        }
        String pageLabel = String.valueOf(pageNumber);
        int color = applyAlpha(0xFF6F5A44, textAlpha * 0.92f);
        int footerY = y + height - PAGE_MARGIN_Y + 1;
        int footerX = x + (width / 2) - (font.width(pageLabel) / 2);
        graphics.drawString(font, pageLabel, footerX, footerY, color, false);
    }

    private void drawPageNumber(Graphics2D graphics, int x, int y, int width, int height, BookPage page, float textAlpha, Integer pageNumber) {
        if (pageNumber == null || page.blocks().isEmpty()) {
            return;
        }
        String pageLabel = String.valueOf(pageNumber);
        graphics.setColor(new Color(applyAlpha(0xFF6F5A44, textAlpha * 0.92f), true));
        int footerY = y + height - PAGE_MARGIN_Y + BODY_LINE_HEIGHT - 2;
        int footerX = x + (width / 2) - (graphics.getFontMetrics().stringWidth(pageLabel) / 2);
        graphics.drawString(pageLabel, footerX, footerY);
    }
}
