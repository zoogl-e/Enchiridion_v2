package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookRegionAction;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.util.List;

final class JournalFlowLayout {
    private static final int CONTENT_X = PageCanvasRenderer.PAGE_MARGIN_X;
    private static final int CONTENT_Y = PageCanvasRenderer.PAGE_MARGIN_Y;
    private static final int CONTENT_W = JournalLayoutMetrics.PAGE_CONTENT_WIDTH;
    private static final int CONTENT_H = JournalLayoutMetrics.PAGE_CONTENT_HEIGHT;
    private static final int DEFAULT_SPACING = 7;
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 5;

    private final List<BookPageElement> elements;
    private final BookPageSide pageSide;
    private int cursorY;

    JournalFlowLayout(List<BookPageElement> elements) {
        this(elements, BookPageSide.LEFT);
    }

    JournalFlowLayout(List<BookPageElement> elements, BookPageSide pageSide) {
        this.elements = elements;
        this.pageSide = pageSide;
        this.cursorY = CONTENT_Y;
    }

    void addText(BookTextBlock.Kind kind, String text) {
        elements.add(JournalElementFactory.textElement(kind, text, CONTENT_X, cursorY, CONTENT_W));
        cursorY += JournalLayoutMetrics.lineHeightFor(kind);
    }

    void addCenteredText(BookTextBlock.Kind kind, String text) {
        elements.add(JournalElementFactory.centeredTextElement(kind, text, CONTENT_X, cursorY, CONTENT_W));
        cursorY += JournalLayoutMetrics.lineHeightFor(kind);
    }

    void addInteractiveTitle(String stableId, String text, Component tooltip, BookRegionAction action) {
        elements.add(JournalElementFactory.toInteractiveTextElement(JournalElementFactory.pageTextLayout(
                stableId,
                BookTextBlock.Kind.TITLE,
                text,
                CONTENT_X,
                cursorY,
                CONTENT_W,
                tooltip,
                action
        )));
        cursorY += JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.TITLE);
    }

    void addPanel(String header, List<String> bodyLines, boolean emphasis, int fillColor, int borderColor) {
        int panelHeight = panelHeight(bodyLines.size());
        int headerY = cursorY + PANEL_PADDING_Y;
        elements.add(JournalElementFactory.textElement(BookTextBlock.Kind.SUBTITLE, header, CONTENT_X + PANEL_PADDING_X, headerY, CONTENT_W - (PANEL_PADDING_X * 2)));
        int bodyY = headerY + JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.SUBTITLE) + 1;
        appendLines(bodyLines, CONTENT_X + PANEL_PADDING_X, bodyY, CONTENT_W - (PANEL_PADDING_X * 2));
        cursorY += panelHeight;
    }

    void addLedgerRow(JournalCharacterStat stat, int targetPageIndex) {
        JournalElementFactory.LedgerRowLayout rowLayout = JournalElementFactory.ledgerRowLayout(pageSide, stat, cursorY, targetPageIndex);
        elements.add(JournalElementFactory.ledgerLabel(rowLayout));
        elements.add(JournalElementFactory.decorationElement(BookTextBlock.Kind.BODY, rowLayout.dotsText(), rowLayout.dotsX(), rowLayout.labelY()));
        elements.add(JournalElementFactory.decorationElement(BookTextBlock.Kind.BODY, rowLayout.valueText(), rowLayout.valueX(), rowLayout.labelY()));
        cursorY += rowLayout.rowHeight();
    }

    void addCenteredButton(String stableId, String label, Component tooltip, BookRegionAction action) {
        int width = JournalLayoutMetrics.SKILL_BUTTON_WIDTH;
        int height = JournalLayoutMetrics.SKILL_BUTTON_HEIGHT;
        int x = CONTENT_X + Math.max(0, (CONTENT_W - width) / 2);
        elements.add(JournalElementFactory.buttonElement(stableId, label, x, cursorY, width, height, tooltip, action));
        cursorY += height;
    }

    void addSpacing(int spacing) {
        cursorY += Math.max(0, spacing);
    }

    int cursorY() {
        return cursorY;
    }

    int remainingHeight() {
        return (CONTENT_Y + CONTENT_H) - cursorY;
    }

    static int contentWidth() {
        return CONTENT_W;
    }

    private void appendLines(List<String> lines, int x, int y, int width) {
        int lineY = y;
        for (String line : lines) {
            if (!line.isBlank()) {
                elements.add(new BookPageElement.TextElement(
                        BookTextBlock.Kind.BODY,
                        Component.literal(line),
                        x,
                        lineY,
                        Math.max(1, Math.min(width, net.minecraft.client.Minecraft.getInstance().font.width(line))),
                        JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY)
                ));
            }
            lineY += JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY);
        }
    }

    private int panelHeight(int bodyLineCount) {
        return (PANEL_PADDING_Y * 2)
                + JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.SUBTITLE)
                + (Math.max(1, bodyLineCount) * JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY));
    }

    static int defaultSpacing() {
        return DEFAULT_SPACING;
    }
}
