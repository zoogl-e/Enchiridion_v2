package net.zoogle.enchiridion.client.render;

public record ReelPresentationProfile(
        /** Small screen-space nudge applied after viewport-centering the reel on X. */
        float horizontalBiasPixels,
        float anchorOffsetY,
        float centerZ,
        float scale
) {
    public static ReelPresentationProfile defaultProfile(float centerZ) {
        return new ReelPresentationProfile(0.0f, 0.0f, centerZ, 16.5f);
    }
}
