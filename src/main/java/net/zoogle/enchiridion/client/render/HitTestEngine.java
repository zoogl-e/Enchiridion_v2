package net.zoogle.enchiridion.client.render;

import java.util.List;

final class HitTestEngine {

    private HitTestEngine() {}

    static Integer pickTopMostCardIndex(List<BookSceneRenderer.ReelCardProjection> cards, float x, float y) {
        BookSceneRenderer.ReelCardProjection best = null;
        for (BookSceneRenderer.ReelCardProjection card : cards) {
            if (!HitTestService.containsPoint(card.quad(), x, y)) {
                continue;
            }
            if (best == null || card.depthZ() > best.depthZ()) {
                best = card;
            }
        }
        return best == null ? null : best.index();
    }
}
