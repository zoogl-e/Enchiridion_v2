package net.zoogle.enchiridion.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.ui.BookDebugSettings;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public final class PageCanvasRenderer {
    private static final int ROW_TEXT_VISUAL_BIAS_Y = -1;
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
    private static final int BUTTON_HORIZONTAL_PADDING = 8;
    private static final int BUTTON_VERTICAL_PADDING = 4;
    private static final int BUTTON_ACCENT_SPACING = 3;
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

        PageContentMetrics.ContentRect contentRect = PageContentMetrics.forInset(horizontalInset);
        int contentX = x + contentRect.contentX();
        int contentY = y + PAGE_MARGIN_Y;
        int contentWidth = Math.max(0, contentRect.contentWidth());
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
        renderPageTextOnlyToGraphics2D(graphics, page, x, y, width, height, textAlpha, horizontalInset, pageNumber, glitchStrength, null, null);
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
            List<PageInteractiveNode> interactiveNodes,
            PageInteractiveNode hoveredInteractiveNode
    ) {
        PageContentMetrics.ContentRect contentRect = PageContentMetrics.forInset(horizontalInset);
        int contentX = x + contentRect.contentX();
        int contentY = y + PAGE_MARGIN_Y;
        int contentWidth = Math.max(0, contentRect.contentWidth());
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
        renderPageElementsToGraphics2D(graphics, page.elements(), textAlpha, hoveredInteractiveNode);
        renderRuntimeInteractiveNodesToGraphics2D(graphics, interactiveNodes, textAlpha, hoveredInteractiveNode);
        drawPageNumber(graphics, x, y, width, height, page, textAlpha, pageNumber);
    }

    private void renderPageElements(GuiGraphics graphics, List<BookPageElement> elements, float textAlpha, PageInteractiveNode hoveredInteractiveNode) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        for (BookPageElement element : elements) {
            renderPageElement(graphics, element, textAlpha, hoveredInteractiveNode);
        }
    }

    private void renderPageElementsToGraphics2D(Graphics2D graphics, List<BookPageElement> elements, float textAlpha, PageInteractiveNode hoveredInteractiveNode) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        for (BookPageElement element : elements) {
            renderPageElementToGraphics2D(graphics, element, textAlpha, hoveredInteractiveNode);
        }
    }

    private void renderPageElement(GuiGraphics graphics, BookPageElement element, float textAlpha, PageInteractiveNode hoveredInteractiveNode) {
        switch (element) {
            case BookPageElement.TextElement text ->
                    renderScaledElementLine(graphics, text.text().getString(), text.x(), text.y(), applyAlpha(colorFor(text.kind()), textAlpha), text.kind(), textAlpha, 0, 0.0f, text.scale());
            case BookPageElement.DecorationElement decoration ->
                    renderLine(graphics, decoration.text().getString(), decoration.x(), decoration.y(), decoration.width(), applyAlpha(colorFor(decoration.kind()), textAlpha), decoration.kind(), textAlpha, 0, 0.0f);
            case BookPageElement.InteractiveTextElement interactive ->
                    renderInteractiveTextElement(graphics, interactive, textAlpha, isHovered(interactive, hoveredInteractiveNode));
            case BookPageElement.ButtonElement button ->
                    renderButtonElement(graphics, button, textAlpha, isHovered(button, hoveredInteractiveNode));
            case BookPageElement.BoxElement box -> renderBox(graphics, box, textAlpha);
            case BookPageElement.ProgressBarElement progressBar -> renderProgressBar(graphics, progressBar, textAlpha);
            case BookPageElement.ImageElement ignored -> {
            }
            case BookPageElement.WidgetElement widget ->
                    renderLine(graphics, widget.label().getString(), widget.x(), widget.y(), widget.width(), applyAlpha(0xFF5A3F29, textAlpha), BookTextBlock.Kind.BODY, textAlpha, 0, 0.0f);
        }
    }

    private void renderPageElementToGraphics2D(Graphics2D graphics, BookPageElement element, float textAlpha, PageInteractiveNode hoveredInteractiveNode) {
        switch (element) {
            case BookPageElement.TextElement text ->
                    renderScaledElementLine(graphics, text.text().getString(), text.x(), text.y() + graphics.getFontMetrics().getAscent(), applyAlpha(colorFor(text.kind()), textAlpha), text.kind(), textAlpha, 0, 0.0f, text.scale());
            case BookPageElement.DecorationElement decoration ->
                    renderLine(graphics, decoration.text().getString(), decoration.x(), decoration.y() + graphics.getFontMetrics().getAscent(), decoration.width(), applyAlpha(colorFor(decoration.kind()), textAlpha), decoration.kind(), textAlpha, 0, 0.0f);
            case BookPageElement.InteractiveTextElement interactive ->
                    renderInteractiveTextElementToGraphics2D(graphics, interactive, textAlpha, isHovered(interactive, hoveredInteractiveNode));
            case BookPageElement.ButtonElement button ->
                    renderButtonElementToGraphics2D(graphics, button, textAlpha, isHovered(button, hoveredInteractiveNode));
            case BookPageElement.BoxElement box -> renderBox(graphics, box, textAlpha);
            case BookPageElement.ProgressBarElement progressBar -> renderProgressBar(graphics, progressBar, textAlpha);
            case BookPageElement.ImageElement ignored -> {
            }
            case BookPageElement.WidgetElement widget ->
                    renderLine(graphics, widget.label().getString(), widget.x(), widget.y() + graphics.getFontMetrics().getAscent(), widget.width(), applyAlpha(0xFF5A3F29, textAlpha), BookTextBlock.Kind.BODY, textAlpha, 0, 0.0f);
        }
    }

    private void renderRuntimeInteractiveNodesToGraphics2D(Graphics2D graphics, List<PageInteractiveNode> nodes, float textAlpha, PageInteractiveNode hoveredInteractiveNode) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (PageInteractiveNode node : nodes) {
            if (!node.isPageNativeVisible() || node.interactiveElement() != null || node.label() == null) {
                continue;
            }
            boolean hovered = hoveredInteractiveNode != null && hoveredInteractiveNode.stableId().equals(node.stableId());
            switch (node.visualType()) {
                case INLINE_LINK -> renderRuntimeInlineLinkToGraphics2D(graphics, node, textAlpha, hovered);
                case BUTTON -> renderRuntimeButtonToGraphics2D(graphics, node, textAlpha, hovered);
                case HOTSPOT -> {
                }
            }
        }
    }

    private void renderRuntimeInlineLinkToGraphics2D(Graphics2D graphics, PageInteractiveNode node, float textAlpha, boolean hovered) {
        BookPageElement.InteractiveTextElement textElement = new BookPageElement.InteractiveTextElement(
                node.stableId(),
                BookTextBlock.Kind.SUBTITLE,
                node.label(),
                node.localX(),
                node.localY(),
                node.localWidth(),
                node.localHeight(),
                1.0f,
                node.tooltip(),
                node.action(),
                BookPageElement.InteractiveVisualStyle.MANUSCRIPT_LINK,
                node.enabled()
        );
        renderInteractiveTextElementToGraphics2D(graphics, textElement, textAlpha, hovered);
    }

    private void renderRuntimeButtonToGraphics2D(Graphics2D graphics, PageInteractiveNode node, float textAlpha, boolean hovered) {
        BookPageElement.ButtonElement buttonElement = new BookPageElement.ButtonElement(
                node.stableId(),
                node.label(),
                node.localX(),
                node.localY(),
                node.localWidth(),
                node.localHeight(),
                node.tooltip(),
                node.action(),
                BookPageElement.InteractiveVisualStyle.BUTTON,
                node.enabled()
        );
        renderButtonElementToGraphics2D(graphics, buttonElement, textAlpha, hovered);
    }

    private static boolean isHovered(BookPageElement.InteractiveElement element, PageInteractiveNode hoveredInteractiveNode) {
        return hoveredInteractiveNode != null && hoveredInteractiveNode.stableId().equals(element.stableId());
    }

    private void renderInteractiveTextElement(GuiGraphics graphics, BookPageElement.InteractiveTextElement element, float textAlpha, boolean hovered) {
        if (Math.abs(element.scale() - 1.0f) > 0.001f) {
            renderScaledInteractiveTextElement(graphics, element, textAlpha, hovered);
            return;
        }
        boolean titleLink = element.kind() == BookTextBlock.Kind.TITLE;
        int defaultInk = applyAlpha(titleLink ? 0xFF6C49CC : colorFor(element.kind()), textAlpha);
        int textY = interactiveTextDrawY(element);
        int textHeight = interactiveGlyphHeight(element.kind());
        renderLine(graphics, element.text().getString(), element.x(), textY, element.width(), defaultInk, element.kind(), textAlpha, 0, 0.0f);
        if (titleLink) {
            int aura = applyAlpha(hovered ? 0xC9E9D9FF : 0x88C8B4FF, textAlpha);
            graphics.drawString(font, "\u2058", element.x() - 6, textY, aura, false);
            graphics.drawString(font, "\u2058", element.x() + element.width() + 2, textY, aura, false);
        }
        if (hovered) {
            int ink = applyAlpha(0xFF6C49CC, textAlpha);
            int support = applyAlpha(0xB0D7C89E, textAlpha);
            renderLine(graphics, element.text().getString(), element.x(), textY, element.width(), ink, element.kind(), textAlpha, 0, 0.0f);
            if (titleLink) {
                graphics.drawString(font, "\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(0, textY - 6), support, false);
            } else {
                int underlineY = textY + Math.max(1, textHeight - 2);
                int leftInset = Math.min(3, Math.max(1, element.width() / 8));
                int rightInset = leftInset;
                int underlineX0 = element.x() + leftInset;
                int underlineX1 = element.x() + element.width() - rightInset;
                if (underlineX1 > underlineX0) {
                    graphics.fill(underlineX0, underlineY, underlineX1, underlineY + 1, ink);
                }
                if (underlineX1 - underlineX0 > 6) {
                    graphics.fill(underlineX0 + 2, underlineY + 2, underlineX1 - 2, underlineY + 3, support);
                }
                graphics.drawString(font, "\u2058", element.x() + element.width() + 2, textY, support, false);
                graphics.drawString(font, "\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(textY, underlineY - 5), support, false);
            }
        }
    }

    private void renderButtonElement(GuiGraphics graphics, BookPageElement.ButtonElement element, float textAlpha, boolean hovered) {
        int fill = applyAlpha(hovered ? 0xB09A7B4A : 0x906E5635, textAlpha);
        int border = applyAlpha(hovered ? 0xFFE6D3A1 : 0xD7C08C5D, textAlpha);
        int inner = applyAlpha(hovered ? 0x28FFF3D0 : 0x18120F0A, textAlpha);
        int accentColor = applyAlpha(hovered ? 0xFFE8DBB8 : 0xBDAF8A63, textAlpha);
        int labelColor = applyAlpha(hovered ? 0xFFFFF5D8 : 0xFFE8D6B7, textAlpha);
        int topHighlight = applyAlpha(hovered ? 0x66FFF3D5 : 0x33F1E2BF, textAlpha);
        int bottomShadow = applyAlpha(hovered ? 0x22000000 : 0x16000000, textAlpha);
        graphics.fill(element.x(), element.y(), element.x() + element.width(), element.y() + element.height(), fill);
        graphics.fill(element.x() + 1, element.y() + 1, element.x() + element.width() - 1, element.y() + element.height() - 1, inner);
        if (element.width() > 6) {
            graphics.fill(element.x() + 3, element.y() + 2, element.x() + element.width() - 3, element.y() + 3, topHighlight);
        }
        if (element.width() > 6 && element.height() > 4) {
            graphics.fill(element.x() + 2, element.y() + element.height() - 3, element.x() + element.width() - 2, element.y() + element.height() - 2, bottomShadow);
        }
        graphics.fill(element.x(), element.y(), element.x() + element.width(), element.y() + 1, border);
        graphics.fill(element.x(), element.y() + element.height() - 1, element.x() + element.width(), element.y() + element.height(), border);
        graphics.fill(element.x(), element.y(), element.x() + 1, element.y() + element.height(), border);
        graphics.fill(element.x() + element.width() - 1, element.y(), element.x() + element.width(), element.y() + element.height(), border);
        String accent = "\u2058";
        int accentWidth = font.width(accent);
        int textWidth = font.width(element.label());
        int spacing = BUTTON_ACCENT_SPACING;
        int clusterWidth = textWidth + (accentWidth * 2) + (spacing * 2);
        int availableWidth = Math.max(0, element.width() - (BUTTON_HORIZONTAL_PADDING * 2));
        boolean drawAccents = clusterWidth <= availableWidth;
        int textY = element.y() + Math.max(BUTTON_VERTICAL_PADDING / 2, (element.height() - font.lineHeight) / 2);
        if (drawAccents) {
            int clusterX = element.x() + Math.max(0, (element.width() - clusterWidth) / 2);
            graphics.drawString(font, accent, clusterX, textY, accentColor, false);
            graphics.drawString(font, element.label(), clusterX + accentWidth + spacing, textY, labelColor, false);
            graphics.drawString(font, accent, clusterX + accentWidth + spacing + textWidth + spacing, textY, accentColor, false);
        } else {
            int textX = element.x() + Math.max(0, (element.width() - textWidth) / 2);
            graphics.drawString(font, element.label(), textX, textY, labelColor, false);
        }
    }

    private void renderInteractiveTextElementToGraphics2D(Graphics2D graphics, BookPageElement.InteractiveTextElement element, float textAlpha, boolean hovered) {
        if (Math.abs(element.scale() - 1.0f) > 0.001f) {
            renderScaledInteractiveTextElement(graphics, element, textAlpha, hovered);
            return;
        }
        boolean titleLink = element.kind() == BookTextBlock.Kind.TITLE;
        int defaultInk = applyAlpha(titleLink ? 0xFF6C49CC : colorFor(element.kind()), textAlpha);
        int textY = interactiveTextDrawY(element);
        int baselineY = textY + graphics.getFontMetrics().getAscent();
        int textHeight = interactiveGlyphHeight(element.kind());
        renderLine(graphics, element.text().getString(), element.x(), baselineY, element.width(), defaultInk, element.kind(), textAlpha, 0, 0.0f);
        if (titleLink) {
            int aura = applyAlpha(hovered ? 0xC9E9D9FF : 0x88C8B4FF, textAlpha);
            graphics.setColor(new Color(aura, true));
            graphics.drawString("\u2058", element.x() - 6, baselineY);
            graphics.drawString("\u2058", element.x() + element.width() + 2, baselineY);
        }
        if (hovered) {
            int ink = applyAlpha(0xFF6C49CC, textAlpha);
            int support = applyAlpha(0xB0D7C89E, textAlpha);
            renderLine(graphics, element.text().getString(), element.x(), baselineY, element.width(), ink, element.kind(), textAlpha, 0, 0.0f);
            if (titleLink) {
                graphics.setColor(new Color(support, true));
                graphics.drawString("\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(graphics.getFontMetrics().getAscent(), baselineY - 6));
            } else {
                int underlineY = textY + Math.max(1, textHeight - 2);
                int leftInset = Math.min(3, Math.max(1, element.width() / 8));
                int rightInset = leftInset;
                int underlineX0 = element.x() + leftInset;
                int underlineX1 = element.x() + element.width() - rightInset;
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
        }
        if (BookDebugSettings.interactiveTextBoundsDebug()) {
            graphics.setColor(new Color(hovered ? 0xCCAA33FF : 0xAA33CC66, true));
            graphics.drawRect(element.x(), element.y(), Math.max(1, element.width() - 1), Math.max(1, element.height() - 1));
        }
    }

    private void renderButtonElementToGraphics2D(Graphics2D graphics, BookPageElement.ButtonElement element, float textAlpha, boolean hovered) {
        int fill = applyAlpha(hovered ? 0xB09A7B4A : 0x906E5635, textAlpha);
        int border = applyAlpha(hovered ? 0xFFE6D3A1 : 0xD7C08C5D, textAlpha);
        int inner = applyAlpha(hovered ? 0x28FFF3D0 : 0x18120F0A, textAlpha);
        int accentColor = applyAlpha(hovered ? 0xFFE8DBB8 : 0xBDAF8A63, textAlpha);
        int labelColor = applyAlpha(hovered ? 0xFFFFF5D8 : 0xFFE8D6B7, textAlpha);
        int topHighlight = applyAlpha(hovered ? 0x66FFF3D5 : 0x33F1E2BF, textAlpha);
        int bottomShadow = applyAlpha(hovered ? 0x22000000 : 0x16000000, textAlpha);
        graphics.setColor(new Color(fill, true));
        graphics.fillRect(element.x(), element.y(), element.width(), element.height());
        graphics.setColor(new Color(inner, true));
        graphics.fillRect(element.x() + 1, element.y() + 1, Math.max(1, element.width() - 2), Math.max(1, element.height() - 2));
        if (element.width() > 6) {
            graphics.setColor(new Color(topHighlight, true));
            graphics.fillRect(element.x() + 3, element.y() + 2, Math.max(1, element.width() - 6), 1);
        }
        if (element.width() > 6 && element.height() > 4) {
            graphics.setColor(new Color(bottomShadow, true));
            graphics.fillRect(element.x() + 2, element.y() + element.height() - 3, Math.max(1, element.width() - 4), 1);
        }
        graphics.setColor(new Color(border, true));
        graphics.drawRect(element.x(), element.y(), Math.max(1, element.width() - 1), Math.max(1, element.height() - 1));
        java.awt.FontMetrics metrics = graphics.getFontMetrics();
        int contentTop = element.y() + Math.max(1, BUTTON_VERTICAL_PADDING / 2);
        int contentHeight = Math.max(1, element.height() - Math.max(2, BUTTON_VERTICAL_PADDING));
        int baselineY = contentTop + ((contentHeight - metrics.getHeight()) / 2) + metrics.getAscent();
        String accent = "\u2058";
        int accentWidth = metrics.stringWidth(accent);
        int textWidth = metrics.stringWidth(element.label().getString());
        int spacing = BUTTON_ACCENT_SPACING;
        int clusterWidth = textWidth + (accentWidth * 2) + (spacing * 2);
        int availableWidth = Math.max(0, element.width() - (BUTTON_HORIZONTAL_PADDING * 2));
        boolean drawAccents = clusterWidth <= availableWidth;
        if (drawAccents) {
            int clusterX = element.x() + Math.max(0, (element.width() - clusterWidth) / 2);
            graphics.setColor(new Color(accentColor, true));
            graphics.drawString(accent, clusterX, baselineY);
            graphics.setColor(new Color(labelColor, true));
            graphics.drawString(element.label().getString(), clusterX + accentWidth + spacing, baselineY);
            graphics.setColor(new Color(accentColor, true));
            graphics.drawString(accent, clusterX + accentWidth + spacing + textWidth + spacing, baselineY);
        } else {
            int textX = element.x() + Math.max(0, (element.width() - textWidth) / 2);
            graphics.setColor(new Color(labelColor, true));
            graphics.drawString(element.label().getString(), textX, baselineY);
        }
    }

    private void renderBox(GuiGraphics graphics, BookPageElement.BoxElement box, float textAlpha) {
        int fill = applyAlpha(box.fillColor(), textAlpha);
        int border = applyAlpha(box.borderColor(), textAlpha);
        int inset = applyAlpha(box.visualStyle() == BookPageElement.PanelVisualStyle.EMPHASIS ? 0x30FFF1D4 : 0x22FFF6E3, textAlpha);
        int topGlow = applyAlpha(box.visualStyle() == BookPageElement.PanelVisualStyle.EMPHASIS ? 0x7AD8BC84 : 0x4CCBAE74, textAlpha);
        int shadow = applyAlpha(box.visualStyle() == BookPageElement.PanelVisualStyle.EMPHASIS ? 0x18000000 : 0x10000000, textAlpha);
        if (((fill >>> 24) & 0xFF) > 0) {
            graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), fill);
        }
        if (((fill >>> 24) & 0xFF) > 0 && box.width() > 2 && box.height() > 2) {
            graphics.fill(box.x() + 1, box.y() + 1, box.x() + box.width() - 1, box.y() + box.height() - 1, inset);
        }
        if (((fill >>> 24) & 0xFF) > 0 && box.width() > 4 && box.height() > 4) {
            graphics.fill(box.x() + 2, box.y() + box.height() - 3, box.x() + box.width() - 2, box.y() + box.height() - 2, shadow);
        }
        if (((border >>> 24) & 0xFF) > 0) {
            graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + 1, border);
            graphics.fill(box.x(), box.y() + box.height() - 1, box.x() + box.width(), box.y() + box.height(), border);
            graphics.fill(box.x(), box.y(), box.x() + 1, box.y() + box.height(), border);
            graphics.fill(box.x() + box.width() - 1, box.y(), box.x() + box.width(), box.y() + box.height(), border);
        }
        if (((fill >>> 24) & 0xFF) > 0 && box.width() > 8) {
            graphics.fill(box.x() + 3, box.y() + 2, box.x() + box.width() - 3, box.y() + 3, topGlow);
        }
    }

    private void renderBox(Graphics2D graphics, BookPageElement.BoxElement box, float textAlpha) {
        int fill = applyAlpha(box.fillColor(), textAlpha);
        int border = applyAlpha(box.borderColor(), textAlpha);
        int inset = applyAlpha(box.visualStyle() == BookPageElement.PanelVisualStyle.EMPHASIS ? 0x30FFF1D4 : 0x22FFF6E3, textAlpha);
        int topGlow = applyAlpha(box.visualStyle() == BookPageElement.PanelVisualStyle.EMPHASIS ? 0x7AD8BC84 : 0x4CCBAE74, textAlpha);
        int shadow = applyAlpha(box.visualStyle() == BookPageElement.PanelVisualStyle.EMPHASIS ? 0x18000000 : 0x10000000, textAlpha);
        if (((fill >>> 24) & 0xFF) > 0) {
            graphics.setColor(new Color(fill, true));
            graphics.fillRect(box.x(), box.y(), box.width(), box.height());
        }
        if (((fill >>> 24) & 0xFF) > 0 && box.width() > 2 && box.height() > 2) {
            graphics.setColor(new Color(inset, true));
            graphics.fillRect(box.x() + 1, box.y() + 1, Math.max(1, box.width() - 2), Math.max(1, box.height() - 2));
        }
        if (((fill >>> 24) & 0xFF) > 0 && box.width() > 4 && box.height() > 4) {
            graphics.setColor(new Color(shadow, true));
            graphics.fillRect(box.x() + 2, box.y() + box.height() - 3, Math.max(1, box.width() - 4), 1);
        }
        if (((border >>> 24) & 0xFF) > 0) {
            graphics.setColor(new Color(border, true));
            graphics.drawRect(box.x(), box.y(), Math.max(1, box.width() - 1), Math.max(1, box.height() - 1));
        }
        if (((fill >>> 24) & 0xFF) > 0 && box.width() > 8) {
            graphics.setColor(new Color(topGlow, true));
            graphics.fillRect(box.x() + 3, box.y() + 2, Math.max(1, box.width() - 6), 1);
        }
    }

    private void renderProgressBar(GuiGraphics graphics, BookPageElement.ProgressBarElement progressBar, float textAlpha) {
        int track = applyAlpha(progressBar.trackColor(), textAlpha);
        int fill = applyAlpha(progressBar.fillColor(), textAlpha);
        int border = applyAlpha(progressBar.borderColor(), textAlpha);
        int highlight = applyAlpha(0x55FFF0C9, textAlpha);
        graphics.fill(progressBar.x(), progressBar.y(), progressBar.x() + progressBar.width(), progressBar.y() + progressBar.height(), track);
        int innerX = progressBar.x() + 1;
        int innerY = progressBar.y() + 1;
        int innerWidth = Math.max(1, progressBar.width() - 2);
        int innerHeight = Math.max(1, progressBar.height() - 2);
        int fillWidth = Math.max(0, Math.min(innerWidth, Math.round(innerWidth * progressBar.progress())));
        if (fillWidth > 0) {
            graphics.fill(innerX, innerY, innerX + fillWidth, innerY + innerHeight, fill);
            if (fillWidth > 4) {
                graphics.fill(innerX + 1, innerY + 1, innerX + fillWidth - 1, innerY + 2, highlight);
            }
        }
        graphics.fill(progressBar.x(), progressBar.y(), progressBar.x() + progressBar.width(), progressBar.y() + 1, border);
        graphics.fill(progressBar.x(), progressBar.y() + progressBar.height() - 1, progressBar.x() + progressBar.width(), progressBar.y() + progressBar.height(), border);
        graphics.fill(progressBar.x(), progressBar.y(), progressBar.x() + 1, progressBar.y() + progressBar.height(), border);
        graphics.fill(progressBar.x() + progressBar.width() - 1, progressBar.y(), progressBar.x() + progressBar.width(), progressBar.y() + progressBar.height(), border);
    }

    private void renderProgressBar(Graphics2D graphics, BookPageElement.ProgressBarElement progressBar, float textAlpha) {
        int track = applyAlpha(progressBar.trackColor(), textAlpha);
        int fill = applyAlpha(progressBar.fillColor(), textAlpha);
        int border = applyAlpha(progressBar.borderColor(), textAlpha);
        int highlight = applyAlpha(0x55FFF0C9, textAlpha);
        graphics.setColor(new Color(track, true));
        graphics.fillRect(progressBar.x(), progressBar.y(), progressBar.width(), progressBar.height());
        int innerX = progressBar.x() + 1;
        int innerY = progressBar.y() + 1;
        int innerWidth = Math.max(1, progressBar.width() - 2);
        int innerHeight = Math.max(1, progressBar.height() - 2);
        int fillWidth = Math.max(0, Math.min(innerWidth, Math.round(innerWidth * progressBar.progress())));
        if (fillWidth > 0) {
            graphics.setColor(new Color(fill, true));
            graphics.fillRect(innerX, innerY, fillWidth, innerHeight);
            if (fillWidth > 4) {
                graphics.setColor(new Color(highlight, true));
                graphics.fillRect(innerX + 1, innerY + 1, Math.max(1, fillWidth - 2), 1);
            }
        }
        graphics.setColor(new Color(border, true));
        graphics.drawRect(progressBar.x(), progressBar.y(), Math.max(1, progressBar.width() - 1), Math.max(1, progressBar.height() - 1));
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

    private void renderScaledElementLine(GuiGraphics graphics, String line, int x, int y, int color, BookTextBlock.Kind kind, float textAlpha, int lineIndex, float glitchStrength, float scale) {
        float renderScale = effectiveRenderScale(kind, scale);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(renderScale, renderScale, 1.0f);
        graphics.drawString(font, line, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void renderScaledElementLine(Graphics2D graphics, String line, int x, int baselineY, int color, BookTextBlock.Kind kind, float textAlpha, int lineIndex, float glitchStrength, float scale) {
        java.awt.geom.AffineTransform originalTransform = graphics.getTransform();
        java.awt.Color originalColor = graphics.getColor();
        graphics.translate(x, baselineY);
        double renderScale = effectiveRenderScale(kind, scale);
        graphics.scale(renderScale, renderScale);
        graphics.setColor(new Color(color, true));
        graphics.drawString(line, 0, 0);
        graphics.setColor(originalColor);
        graphics.setTransform(originalTransform);
    }

    private void renderScaledInteractiveTextElement(GuiGraphics graphics, BookPageElement.InteractiveTextElement element, float textAlpha, boolean hovered) {
        boolean titleLink = element.kind() == BookTextBlock.Kind.TITLE;
        int defaultInk = applyAlpha(titleLink ? 0xFF6C49CC : colorFor(element.kind()), textAlpha);
        int support = applyAlpha(0xB0D7C89E, textAlpha);
        int textY = interactiveTextDrawY(element);
        renderScaledElementLine(graphics, element.text().getString(), element.x(), textY, defaultInk, element.kind(), textAlpha, 0, 0.0f, element.scale());
        float renderScale = effectiveRenderScale(element.kind(), element.scale());
        int logicalWidth = Math.max(1, Math.round(element.width() / renderScale));
        int logicalHeight = interactiveGlyphHeight(element.kind());
        if (titleLink) {
            graphics.drawString(font, "\u2058", element.x() - Math.round(6 * renderScale), textY, applyAlpha(hovered ? 0xC9E9D9FF : 0x88C8B4FF, textAlpha), false);
            graphics.drawString(font, "\u2058", element.x() + element.width() + 2, textY, applyAlpha(hovered ? 0xC9E9D9FF : 0x88C8B4FF, textAlpha), false);
        }
        if (hovered) {
            renderScaledElementLine(graphics, element.text().getString(), element.x(), textY, applyAlpha(0xFF6C49CC, textAlpha), element.kind(), textAlpha, 0, 0.0f, element.scale());
            if (titleLink) {
                graphics.drawString(font, "\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(0, textY - 6), support, false);
            } else {
                int underlineY = textY + Math.max(1, logicalHeight - 2);
                int leftInset = Math.min(3, Math.max(1, logicalWidth / 8));
                int underlineX0 = element.x() + Math.round(leftInset * renderScale);
                int underlineX1 = element.x() + element.width() - Math.round(leftInset * renderScale);
                if (underlineX1 > underlineX0) {
                    graphics.fill(underlineX0, underlineY, underlineX1, underlineY + 1, applyAlpha(0xFF6C49CC, textAlpha));
                }
                if (underlineX1 - underlineX0 > 6) {
                    graphics.fill(underlineX0 + 2, underlineY + 2, underlineX1 - 2, underlineY + 3, support);
                }
            }
        }
    }

    private void renderScaledInteractiveTextElement(Graphics2D graphics, BookPageElement.InteractiveTextElement element, float textAlpha, boolean hovered) {
        boolean titleLink = element.kind() == BookTextBlock.Kind.TITLE;
        int defaultInk = applyAlpha(titleLink ? 0xFF6C49CC : colorFor(element.kind()), textAlpha);
        int textY = interactiveTextDrawY(element);
        int baselineY = textY + graphics.getFontMetrics().getAscent();
        int support = applyAlpha(0xB0D7C89E, textAlpha);
        renderScaledElementLine(graphics, element.text().getString(), element.x(), baselineY, defaultInk, element.kind(), textAlpha, 0, 0.0f, element.scale());
        if (hovered) {
            renderScaledElementLine(graphics, element.text().getString(), element.x(), baselineY, applyAlpha(0xFF6C49CC, textAlpha), element.kind(), textAlpha, 0, 0.0f, element.scale());
        }
        if (titleLink) {
            graphics.setColor(new Color(applyAlpha(hovered ? 0xC9E9D9FF : 0x88C8B4FF, textAlpha), true));
            graphics.drawString("\u2058", element.x() - 6, baselineY);
            graphics.drawString("\u2058", element.x() + element.width() + 2, baselineY);
            if (hovered) {
                graphics.setColor(new Color(support, true));
                graphics.drawString("\u2058", element.x() + Math.max(0, (element.width() / 2) - 2), Math.max(graphics.getFontMetrics().getAscent(), baselineY - 6));
            }
        }
    }

    private float effectiveRenderScale(BookTextBlock.Kind kind, float scale) {
        return (kind == BookTextBlock.Kind.LEVEL ? 2.0f : 1.0f) * Math.max(0.1f, scale);
    }

    private int interactiveTextDrawY(BookPageElement.InteractiveTextElement element) {
        int centeredY = element.y() + Math.max(0, (element.height() - interactiveGlyphHeight(element.kind())) / 2);
        return Math.max(element.y(), centeredY + (element.kind() == BookTextBlock.Kind.SUBTITLE ? ROW_TEXT_VISUAL_BIAS_Y : 0));
    }

    private int interactiveGlyphHeight(BookTextBlock.Kind kind) {
        return Math.max(1, lineHeightFor(kind) - LINE_SPACING);
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
        int sourceAlpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.clamp(
                Math.round(sourceAlpha * Math.clamp(alpha, 0.0f, 1.0f)),
                0,
                255
        );
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
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
