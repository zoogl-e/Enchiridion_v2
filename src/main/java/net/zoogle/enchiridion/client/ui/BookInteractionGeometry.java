package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

public interface BookInteractionGeometry {
    BookSceneRenderer.PageSurfaceBounds pageSurfaceBounds(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide
    );

    BookSceneRenderer.PageLocalPoint pageLocalPoint(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            double screenX,
            double screenY
    );

    BookSceneRenderer.ScreenPoint projectPagePoint(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            float localX,
            float localY
    );

    BookSceneRenderer.ScreenRect projectPageRect(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int localX,
            int localY,
            int localWidth,
            int localHeight
    );

    BookSceneRenderer.ScreenQuad projectPageQuad(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookPageSide pageSide,
            int localX,
            int localY,
            int localWidth,
            int localHeight
    );

    BookSceneRenderer.ScreenRect projectTrackedRegionRect(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookTrackedRegion.Anchor anchor
    );

    BookSceneRenderer.ScreenQuad projectTrackedRegionQuad(
            BookLayout layout,
            BookAnimState state,
            float animationProgress,
            float projectionProgress,
            float projectionFocusOffset,
            BookTrackedRegion.Anchor anchor
    );
}
