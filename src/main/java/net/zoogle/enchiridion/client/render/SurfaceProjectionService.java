package net.zoogle.enchiridion.client.render;

import org.joml.Vector3f;

final class SurfaceProjectionService {
    private SurfaceProjectionService() {}

    static BookSceneRenderer.ReelCardProjection projectArchetypeReelCardProjection(
            BookSceneRenderer.ReelPresentationTransform presentation,
            ArchetypeReelSceneRenderer.PlacedCard card,
            float focusedTiltYaw,
            float focusedTiltPitch
    ) {
        float halfWidth = (59.0f / 16.0f) * 0.5f;
        float halfHeight = (80.0f / 16.0f) * 0.5f;
        float depth = 0.04f;
        ProjectedPoint topLeft = projectArchetypeReelPoint(presentation, card, -halfWidth, -halfHeight, depth, focusedTiltYaw, focusedTiltPitch);
        ProjectedPoint topRight = projectArchetypeReelPoint(presentation, card, halfWidth, -halfHeight, depth, focusedTiltYaw, focusedTiltPitch);
        ProjectedPoint bottomRight = projectArchetypeReelPoint(presentation, card, halfWidth, halfHeight, depth, focusedTiltYaw, focusedTiltPitch);
        ProjectedPoint bottomLeft = projectArchetypeReelPoint(presentation, card, -halfWidth, halfHeight, depth, focusedTiltYaw, focusedTiltPitch);
        return new BookSceneRenderer.ReelCardProjection(
                card.index(),
                new BookSceneRenderer.ScreenQuad(topLeft.screenPoint(), topRight.screenPoint(), bottomRight.screenPoint(), bottomLeft.screenPoint()),
                (topLeft.depth() + topRight.depth() + bottomRight.depth() + bottomLeft.depth()) / 4.0f,
                card.focused()
        );
    }

    private static ProjectedPoint projectArchetypeReelPoint(
            BookSceneRenderer.ReelPresentationTransform presentation,
            ArchetypeReelSceneRenderer.PlacedCard card,
            float localX,
            float localY,
            float localZ,
            float focusedTiltYaw,
            float focusedTiltPitch
    ) {
        float scale = presentation.scale() * card.scale();
        Vector3f point = new Vector3f(localX * scale, localY * scale, localZ * scale);
        point.rotateY((float) Math.toRadians(card.yawDegrees()));
        if (card.focused()) {
            point.rotateY((float) Math.toRadians(focusedTiltYaw));
            point.rotateX((float) Math.toRadians(focusedTiltPitch));
        }
        point.add(card.centerX() * presentation.scale(), card.centerY() * presentation.scale(), card.centerZ() * presentation.scale());
        float depth = presentation.centerZ() + point.z;
        float perspective = perspectiveScale(depth);
        return new ProjectedPoint(new BookSceneRenderer.ScreenPoint(
                presentation.centerX() + (point.x * perspective),
                presentation.centerY() + (point.y * perspective)
        ), depth);
    }

    private static float perspectiveScale(float z) {
        return BookSceneRenderer.PAGE_PICK_CAMERA_DEPTH / Math.max(1.0f, BookSceneRenderer.PAGE_PICK_CAMERA_DEPTH - z);
    }

    private record ProjectedPoint(BookSceneRenderer.ScreenPoint screenPoint, float depth) {}
}
