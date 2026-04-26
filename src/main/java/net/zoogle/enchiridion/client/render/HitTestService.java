package net.zoogle.enchiridion.client.render;

public final class HitTestService {
    private HitTestService() {}

    public static boolean containsPoint(BookSceneRenderer.ScreenQuad quad, float x, float y) {
        if (quad == null) {
            return false;
        }
        BookSceneRenderer.ScreenPoint point = new BookSceneRenderer.ScreenPoint(x, y);
        return pointInsideTriangle(point, quad.topLeft(), quad.topRight(), quad.bottomRight())
                || pointInsideTriangle(point, quad.topLeft(), quad.bottomRight(), quad.bottomLeft());
    }

    private static boolean pointInsideTriangle(
            BookSceneRenderer.ScreenPoint point,
            BookSceneRenderer.ScreenPoint a,
            BookSceneRenderer.ScreenPoint b,
            BookSceneRenderer.ScreenPoint c
    ) {
        float denominator = ((b.y() - c.y()) * (a.x() - c.x())) + ((c.x() - b.x()) * (a.y() - c.y()));
        if (Math.abs(denominator) < 0.0001f) {
            return false;
        }
        float alpha = (((b.y() - c.y()) * (point.x() - c.x())) + ((c.x() - b.x()) * (point.y() - c.y()))) / denominator;
        float beta = (((c.y() - a.y()) * (point.x() - c.x())) + ((a.x() - c.x()) * (point.y() - c.y()))) / denominator;
        float gamma = 1.0f - alpha - beta;
        float epsilon = 0.001f;
        return alpha >= -epsilon && beta >= -epsilon && gamma >= -epsilon;
    }
}
