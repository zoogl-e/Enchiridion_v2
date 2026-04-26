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
    /**
     * Asymptote for log-scaled radar radius: {@code log1p(level) / log1p(cap)} approaches 1 as level grows.
     * Tuned so low levels read clearly while very high totals do not consume the entire chart.
     */
    private static final int RADAR_LEVEL_LOG_CAP = 300;
    /**
     * Minimum vertex radius for the combined-level polygon so sparse builds (one discipline leveled)
     * still read as a polygon instead of a single ray from the center.
     */
    private static final float RADAR_MAIN_BASELINE = 0.07f;

    private JournalLayoutMetrics() {}

    /**
     * Maps a non-negative discipline level to 0–1 for radar geometry using a soft saturating curve.
     */
    static float radarLevelDisplayNorm(int level) {
        int v = Math.max(0, level);
        if (v == 0) {
            return 0.0f;
        }
        double cap = Math.log1p(RADAR_LEVEL_LOG_CAP);
        return Math.min(1.0f, (float) (Math.log1p(v) / cap));
    }

    /**
     * Vertex radius for the main (invested + mastery) polygon: lifts empty axes slightly so the mesh stays visible.
     */
    static float radarMainVertexRadius(int combinedLevel) {
        float n = radarLevelDisplayNorm(combinedLevel);
        return RADAR_MAIN_BASELINE + (1.0f - RADAR_MAIN_BASELINE) * n;
    }

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
