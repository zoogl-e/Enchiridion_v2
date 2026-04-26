package net.zoogle.enchiridion.client.ui;

import org.jetbrains.annotations.Nullable;

import net.zoogle.enchiridion.client.render.BookSceneRenderer;

final class InfoPanelLayoutService {
    /**
     * Parchment stats panel to the right of the focused reel card (mockup), clamped so it fits before the next card or viewport edge.
     */
    PanelRect resolveReelArchetypePanelRect(
            @Nullable BookSceneRenderer.ScreenRect focusedCardRect,
            @Nullable Float nextCardLeftEdge,
            BookLayout layout,
            int viewportWidth,
            int viewportHeight
    ) {
        int panelHeight = 182;
        int preferredWidth = 280;
        int gap = 14;
        if (focusedCardRect == null || focusedCardRect.width() < 4.0f) {
            return reelPanelBookFallback(layout, viewportWidth, panelHeight, preferredWidth);
        }
        int panelX = Math.round(focusedCardRect.x() + focusedCardRect.width() + gap);
        int panelY = Math.max(20, Math.round(focusedCardRect.y()));
        int maxRight = nextCardLeftEdge != null
                ? (int) Math.floor(nextCardLeftEdge - 8.0f)
                : viewportWidth - 22;
        int panelWidth = Math.min(preferredWidth, Math.max(120, maxRight - panelX));
        if (panelWidth < 120) {
            panelX = Math.max(8, Math.min(panelX, viewportWidth - 142));
            panelWidth = Math.min(preferredWidth, Math.max(120, viewportWidth - panelX - 22));
        }
        panelY = Math.min(panelY, viewportHeight - panelHeight - 12);
        return new PanelRect(panelX, panelY, panelWidth, panelHeight);
    }

    private static PanelRect reelPanelBookFallback(BookLayout layout, int viewportWidth, int panelHeight, int preferredWidth) {
        int bookRight = layout.bookX() + layout.bookWidth();
        int panelX = Math.min(bookRight + 20, viewportWidth - preferredWidth - 22);
        panelX = Math.max(12, panelX);
        int panelY = Math.max(24, layout.bookY() + 28);
        int panelWidth = Math.min(preferredWidth, Math.max(120, viewportWidth - panelX - 22));
        return new PanelRect(panelX, panelY, panelWidth, panelHeight);
    }

    PanelRect resolvePanelRect(BookLayout layout, BookSceneRenderer.ScreenRect anchorRect, int viewportWidth, int viewportHeight) {
        int preferredWidth = 240;
        int panelHeight = 182;
        int panelX = anchorRect == null
                ? Math.min(viewportWidth - (preferredWidth + 22), layout.bookX() + layout.bookWidth() + 74)
                : Math.min(viewportWidth - (preferredWidth + 22), Math.round(anchorRect.x() + anchorRect.width() + 52.0f));
        int panelY = anchorRect == null
                ? layout.bookY() + 46
                : Math.max(24, Math.round(anchorRect.y() + 8.0f));
        int panelWidth = Math.min(preferredWidth, viewportWidth - panelX - 22);
        return new PanelRect(panelX, panelY, panelWidth, panelHeight);
    }

    /**
     * Static reel HUD panel placed between the reel and lowered book.
     * Navigation hints are rendered inside this panel's bottom strip.
     */
    PanelRect resolveStaticReelArchetypePanelRect(int viewportWidth, int viewportHeight) {
        int panelWidth = Math.min(440, Math.max(200, viewportWidth - 100));
        int panelHeight = 160;
        int panelX = (viewportWidth / 2) - (panelWidth / 2);
        int panelY = viewportHeight - panelHeight - 24;
        panelX = Math.max(24, Math.min(panelX, viewportWidth - panelWidth - 24));
        panelY = Math.max(viewportHeight / 2, panelY);
        return new PanelRect(panelX, panelY, panelWidth, panelHeight);
    }

    record PanelRect(int x, int y, int width, int height) {
        boolean visible() {
            return width >= 120;
        }
    }
}
