package net.zoogle.enchiridion.client.levelrpg.journal.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookRegionAction;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.levelrpg.model.JournalCharacterStat;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPageStyleSystem;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalTextRole;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.util.List;

public final class JournalElementFactory {
    private JournalElementFactory() {}

    public static PageInteractiveTextLayout pageTextLayout(
            String stableId,
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int contentY,
            int contentWidth,
            Component tooltip,
            BookRegionAction action
    ) {
        int drawX = alignedTextX(text, contentX, contentWidth, kind);
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(text));
        int textHeight = JournalLayoutMetrics.lineHeightFor(kind);
        return new PageInteractiveTextLayout(
                stableId,
                BookPageElement.InteractiveVisualStyle.MANUSCRIPT_LINK,
                kind,
                text,
                drawX,
                contentY,
                textWidth,
                textHeight,
                1.0f,
                tooltip,
                action,
                true
        );
    }

    public static PageInteractiveTextLayout centeredPageTextLayout(
            String stableId,
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int contentY,
            int contentWidth,
            Component tooltip,
            BookRegionAction action
    ) {
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(text));
        int drawX = contentX + Math.max(0, (contentWidth - textWidth) / 2);
        int textHeight = JournalLayoutMetrics.lineHeightFor(kind);
        return new PageInteractiveTextLayout(
                stableId,
                BookPageElement.InteractiveVisualStyle.MANUSCRIPT_LINK,
                kind,
                text,
                drawX,
                contentY,
                textWidth,
                textHeight,
                1.0f,
                tooltip,
                action,
                true
        );
    }

    public static BookPageElement.TextElement textElement(
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int contentY,
            int contentWidth
    ) {
        int drawX = alignedTextX(text, contentX, contentWidth, kind);
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(text));
        return new BookPageElement.TextElement(
                kind,
                Component.literal(text),
                drawX,
                contentY,
                textWidth,
                JournalLayoutMetrics.lineHeightFor(kind),
                1.0f
        );
    }

    public static BookPageElement.TextElement centeredTextElement(
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int contentY,
            int contentWidth
    ) {
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(text));
        int drawX = contentX + Math.max(0, (contentWidth - textWidth) / 2);
        return new BookPageElement.TextElement(
                kind,
                Component.literal(text),
                drawX,
                contentY,
                textWidth,
                JournalLayoutMetrics.lineHeightFor(kind),
                1.0f
        );
    }

    public static BookPageElement.TextElement centeredRoleTextElement(
            JournalTextRole role,
            String text,
            int x,
            int y,
            int width,
            int height,
            float scale
    ) {
        return new BookPageElement.TextElement(
                JournalPageStyleSystem.kindFor(role),
                Component.literal(text),
                x,
                y,
                width,
                height,
                scale
        );
    }

    public static BookPageElement.TextElement centeredRoleTextElement(
            JournalTextRole role,
            String text,
            BookPageSide pageSide,
            int y
    ) {
        JournalLayoutMetrics.PageContentRect rect = JournalLayoutMetrics.pageContentRect(pageSide);
        return centeredRoleTextElement(role, text, rect.contentX(), y, rect.contentWidth());
    }

    public static BookPageElement.TextElement centeredRoleTextElement(
            JournalTextRole role,
            String text,
            int contentX,
            int y,
            int contentWidth
    ) {
        return centeredTextElement(JournalPageStyleSystem.kindFor(role), text, contentX, y, contentWidth);
    }

    public static PageInteractiveTextLayout centeredRoleInteractiveTextLayout(
            String stableId,
            JournalTextRole role,
            String text,
            int x,
            int y,
            int width,
            int height,
            float scale,
            Component tooltip,
            BookRegionAction action
    ) {
        return new PageInteractiveTextLayout(
                stableId,
                BookPageElement.InteractiveVisualStyle.MANUSCRIPT_LINK,
                JournalPageStyleSystem.kindFor(role),
                text,
                x,
                y,
                width,
                height,
                scale,
                tooltip,
                action,
                true
        );
    }

    public static BookPageElement.DecorationElement decorationElement(
            BookTextBlock.Kind kind,
            String text,
            int x,
            int y
    ) {
        int textWidth = Math.max(1, Minecraft.getInstance().font.width(text));
        return new BookPageElement.DecorationElement(
                kind,
                Component.literal(text),
                x,
                y,
                textWidth,
                JournalLayoutMetrics.lineHeightFor(kind)
        );
    }

    public static BookPageElement.BoxElement boxElement(
            int x,
            int y,
            int width,
            int height,
            int fillColor,
            int borderColor
    ) {
        return new BookPageElement.BoxElement(x, y, width, height, fillColor, borderColor, BookPageElement.PanelVisualStyle.PANEL);
    }

    public static BookPageElement.BoxElement emphasisBoxElement(
            int x,
            int y,
            int width,
            int height,
            int fillColor,
            int borderColor
    ) {
        return new BookPageElement.BoxElement(x, y, width, height, fillColor, borderColor, BookPageElement.PanelVisualStyle.EMPHASIS);
    }

    public static int appendTextElements(
            List<BookPageElement> elements,
            BookTextBlock.Kind kind,
            String text,
            int contentX,
            int startY,
            int contentWidth,
            boolean interactive,
            Component tooltip,
            BookRegionAction action
    ) {
        List<JournalPaginationEngine.MeasuredLine> lines = kind == BookTextBlock.Kind.BODY
                ? JournalPaginationEngine.wrapBodyText(text, contentWidth)
                : JournalPaginationEngine.wrapStaticText(text, contentWidth);
        int cursorY = startY;
        for (JournalPaginationEngine.MeasuredLine line : lines) {
            if (line.text().isBlank()) {
                cursorY += JournalLayoutMetrics.lineHeightFor(kind);
                continue;
            }
            int drawX = alignedTextX(line.text(), contentX, contentWidth, kind);
            int textWidth = Math.max(1, Minecraft.getInstance().font.width(line.text()));
            if (interactive && action != null) {
                elements.add(toInteractiveTextElement(new PageInteractiveTextLayout(
                        "text:" + kind + ":" + drawX + ":" + cursorY + ":" + line.text(),
                        BookPageElement.InteractiveVisualStyle.MANUSCRIPT_LINK,
                        kind,
                        line.text(),
                        drawX,
                        cursorY,
                        textWidth,
                        JournalLayoutMetrics.lineHeightFor(kind),
                        1.0f,
                        tooltip,
                        action,
                        true
                )));
            } else {
                elements.add(new BookPageElement.TextElement(kind, Component.literal(line.text()), drawX, cursorY, textWidth, JournalLayoutMetrics.lineHeightFor(kind), 1.0f));
            }
            cursorY += JournalLayoutMetrics.lineHeightFor(kind);
        }
        return cursorY;
    }

    public static int appendMeasuredLines(
            List<BookPageElement> elements,
            BookTextBlock.Kind kind,
            List<JournalPaginationEngine.MeasuredLine> lines,
            int startInclusive,
            int endExclusive,
            int contentX,
            int startY,
            int contentWidth
    ) {
        int cursorY = startY;
        for (int index = startInclusive; index < endExclusive; index++) {
            JournalPaginationEngine.MeasuredLine line = lines.get(index);
            if (line.text().isBlank()) {
                cursorY += JournalLayoutMetrics.lineHeightFor(kind);
                continue;
            }
            int drawX = alignedTextX(line.text(), contentX, contentWidth, kind);
            int textWidth = Math.max(1, Minecraft.getInstance().font.width(line.text()));
            elements.add(new BookPageElement.TextElement(
                    kind,
                    Component.literal(line.text()),
                    drawX,
                    cursorY,
                    textWidth,
                    JournalLayoutMetrics.lineHeightFor(kind),
                    1.0f
            ));
            cursorY += JournalLayoutMetrics.lineHeightFor(kind);
        }
        return cursorY;
    }

    public static LedgerRowLayout ledgerRowLayout(BookPageSide pageSide, JournalCharacterStat stat, int rowY, int targetPageIndex) {
        String label = stat.name();
        String valueText = stat.value() + " (Pot. " + stat.masteryValue() + ")";
        JournalLayoutMetrics.PageContentRect contentRect = JournalLayoutMetrics.pageContentRect(pageSide);
        int rowHeight = ledgerRowHeight();
        int labelHeight = rowHeight;
        int labelY = rowY;
        int labelX = contentRect.contentX() + 2;
        int labelWidth = Math.max(1, Minecraft.getInstance().font.width(label));
        int valueWidth = Math.max(1, Minecraft.getInstance().font.width(valueText));
        int valueX = contentRect.contentX() + contentRect.contentWidth() - valueWidth - 2;
        int dotsX = labelX + labelWidth + 8;
        int dotWidth = Math.max(0, valueX - dotsX - 6);
        int dotCount = Math.max(2, dotWidth / Math.max(1, Minecraft.getInstance().font.width(".")));
        return new LedgerRowLayout(
                label,
                labelX,
                labelY,
                labelWidth,
                labelHeight,
                dotsX,
                valueX,
                rowY,
                rowHeight,
                ".".repeat(dotCount),
                valueText,
                targetPageIndex
        );
    }

    public static int ledgerRowHeight() {
        return Math.max(
                JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY),
                JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.SUBTITLE)
        );
    }

    public static BookPageElement.InteractiveTextElement ledgerLabel(LedgerRowLayout rowLayout) {
        return toInteractiveTextElement(new PageInteractiveTextLayout(
                "ledger:" + rowLayout.labelText(),
                BookPageElement.InteractiveVisualStyle.MANUSCRIPT_LINK,
                BookTextBlock.Kind.SUBTITLE,
                rowLayout.labelText(),
                rowLayout.labelX(),
                rowLayout.labelY(),
                rowLayout.labelWidth(),
                rowLayout.labelHeight(),
                1.0f,
                Component.literal("Open " + rowLayout.labelText() + " entry"),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.jumpToJournalPage(bookContext, rowLayout.targetPageIndex()),
                true
        ));
    }

    public static BookPageElement.InteractiveTextElement toInteractiveTextElement(PageInteractiveTextLayout layout) {
        return new BookPageElement.InteractiveTextElement(
                layout.stableId(),
                layout.kind(),
                Component.literal(layout.text()),
                layout.x(),
                layout.y(),
                layout.width(),
                layout.height(),
                layout.scale(),
                layout.tooltip(),
                layout.action(),
                layout.visualStyle(),
                layout.enabled()
        );
    }

    public static BookPageElement.ButtonElement buttonElement(
            String stableId,
            String label,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            BookRegionAction action
    ) {
        return new BookPageElement.ButtonElement(
                stableId,
                Component.literal(label),
                x,
                y,
                width,
                height,
                tooltip,
                action,
                BookPageElement.InteractiveVisualStyle.BUTTON,
                true
        );
    }

    private static int alignedTextX(String text, int contentX, int contentWidth, BookTextBlock.Kind kind) {
        if (kind == BookTextBlock.Kind.TITLE) {
            int textWidth = Minecraft.getInstance().font.width(text);
            return contentX + Math.max(0, (contentWidth - textWidth) / 2);
        }
        return contentX;
    }

    public record LedgerRowLayout(
            String labelText,
            int labelX,
            int labelY,
            int labelWidth,
            int labelHeight,
            int dotsX,
            int valueX,
            int rowY,
            int rowHeight,
            String dotsText,
            String valueText,
            int targetPageIndex
    ) {}

    public record PageInteractiveTextLayout(
            String stableId,
            BookPageElement.InteractiveVisualStyle visualStyle,
            BookTextBlock.Kind kind,
            String text,
            int x,
            int y,
            int width,
            int height,
            float scale,
            Component tooltip,
            BookRegionAction action,
            boolean enabled
    ) {}
}
