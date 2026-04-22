package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageContentMetrics;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

final class JournalLayoutMetrics {
    static final int TEXTURE_PAGE_TEXT_WIDTH = 115;
    static final int TEXTURE_PAGE_TEXT_HEIGHT = 145;
    static final int PAGE_BOTTOM_SAFE_BUFFER = 10;
    static final int PAGE_CONTENT_WIDTH = TEXTURE_PAGE_TEXT_WIDTH - 3;
    static final int PAGE_CONTENT_HEIGHT = TEXTURE_PAGE_TEXT_HEIGHT - PAGE_BOTTOM_SAFE_BUFFER;
    static final int BREAK_SEARCH_WINDOW = 3;
    static final int XP_BAR_SEGMENTS = 12;
    static final int SKILL_BUTTON_WIDTH = 78;
    static final int SKILL_BUTTON_HEIGHT = 14;
    static final int SKILL_BUTTON_FOOTER_RESERVE = SKILL_BUTTON_HEIGHT + PageCanvasRenderer.BLOCK_SPACING;

    private JournalLayoutMetrics() {}

    static int lineHeightFor(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> PageCanvasRenderer.TITLE_LINE_HEIGHT;
            case SUBTITLE -> PageCanvasRenderer.SUBTITLE_LINE_HEIGHT;
            case LEVEL -> PageCanvasRenderer.LEVEL_LINE_HEIGHT;
            case SECTION -> PageCanvasRenderer.SECTION_LINE_HEIGHT;
            case BODY -> PageCanvasRenderer.BODY_LINE_HEIGHT;
        } + PageCanvasRenderer.LINE_SPACING;
    }

    static int heightFor(BookTextBlock.Kind kind, int lineCount) {
        return lineCount * lineHeightFor(kind);
    }

    static PageContentRect pageContentRect(BookPageSide pageSide) {
        PageContentMetrics.ContentRect rect = PageContentMetrics.forSide(pageSide);
        return new PageContentRect(rect.contentX(), rect.contentWidth());
    }

    record PageContentRect(int contentX, int contentWidth) {}
}
