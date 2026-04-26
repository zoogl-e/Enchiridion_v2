package net.zoogle.enchiridion.client.render;

import java.util.List;

final class HitTestEngine {
    /**
     * Non-focused cards are smaller and yawed away, making them hard to click at exact bounds.
     * Their hit quad is expanded outward from its centre by this factor (0.42 ≈ +42% in each dimension).
     */
    private static final float SIDE_CARD_HIT_INFLATION = 0.42f;

    private HitTestEngine() {}

    static Integer pickTopMostCardIndex(List<BookSceneRenderer.ReelCardProjection> cards, float x, float y) {
        BookSceneRenderer.ReelCardProjection best = null;
        for (BookSceneRenderer.ReelCardProjection card : cards) {
            BookSceneRenderer.ScreenQuad hitQuad = card.focused()
                    ? card.quad()
                    : inflateQuad(card.quad(), SIDE_CARD_HIT_INFLATION);
            if (!HitTestService.containsPoint(hitQuad, x, y)) {
                continue;
            }
            if (best == null || card.depthZ() > best.depthZ()) {
                best = card;
            }
        }
        return best == null ? null : best.index();
    }

    /** Scales all four corners of a quad outward from its centroid by {@code factor}. */
    private static BookSceneRenderer.ScreenQuad inflateQuad(BookSceneRenderer.ScreenQuad q, float factor) {
        float cx = (q.topLeft().x() + q.topRight().x() + q.bottomRight().x() + q.bottomLeft().x()) * 0.25f;
        float cy = (q.topLeft().y() + q.topRight().y() + q.bottomRight().y() + q.bottomLeft().y()) * 0.25f;
        return new BookSceneRenderer.ScreenQuad(
                inflate(q.topLeft(),     cx, cy, factor),
                inflate(q.topRight(),    cx, cy, factor),
                inflate(q.bottomRight(), cx, cy, factor),
                inflate(q.bottomLeft(),  cx, cy, factor)
        );
    }

    private static BookSceneRenderer.ScreenPoint inflate(BookSceneRenderer.ScreenPoint p, float cx, float cy, float factor) {
        return new BookSceneRenderer.ScreenPoint(
                cx + (p.x() - cx) * (1.0f + factor),
                cy + (p.y() - cy) * (1.0f + factor)
        );
    }
}
