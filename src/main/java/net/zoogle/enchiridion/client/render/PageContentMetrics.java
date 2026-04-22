package net.zoogle.enchiridion.client.render;

import net.zoogle.enchiridion.api.BookPageSide;

public final class PageContentMetrics {
    private static final int TEXTURE_PAGE_TEXT_WIDTH = 115;
    private static final int PAGE_CONTENT_WIDTH = TEXTURE_PAGE_TEXT_WIDTH - 3;

    private PageContentMetrics() {}

    public static ContentRect forSide(BookPageSide pageSide) {
        return forInset(pageSide == BookPageSide.RIGHT ? PageCanvasRenderer.RIGHT_PAGE_INSET : 0);
    }

    public static ContentRect forInset(int horizontalInset) {
        return new ContentRect(
                PageCanvasRenderer.PAGE_MARGIN_X + Math.max(0, horizontalInset),
                Math.max(1, PAGE_CONTENT_WIDTH - Math.max(0, horizontalInset))
        );
    }

    public record ContentRect(int contentX, int contentWidth) {}
}
