package net.zoogle.enchiridion.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.Enchiridion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class BookPageTexturePipeline {
    private static final int DYNAMIC_TEXTURE_SIZE = 512;
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "textures/gui/enchiridion.png");
    private static final String LEFT_TEXTURE_PREFIX = Enchiridion.MODID + "_magic_text_left";
    private static final String RIGHT_TEXTURE_PREFIX = Enchiridion.MODID + "_magic_text_right";
    private static final float UV_SPACE_SIZE = 16.0f;

    // Dedicated readable plane UVs from the model
    private static final float LEFT_U0 = 11.7875f;
    private static final float LEFT_V0 = 10.84375f;
    private static final float LEFT_U1 = 16.0f;
    private static final float LEFT_V1 = 16.0f;
    private static final float RIGHT_U0 = 11.53125f;
    private static final float RIGHT_V0 = 10.84375f;
    private static final float RIGHT_U1 = 16.0f;
    private static final float RIGHT_V1 = 16.0f;

    // Hook for future magical polish pass (glow/intensity)
    private static final int MAGIC_TEXT_PANEL_ALPHA = 0;

    private final PageCanvasRenderer pageCanvasRenderer = new PageCanvasRenderer();
    private final BufferedImage transparentCanvas;
    private final DynamicTexture leftDynamicTexture;
    private final DynamicTexture rightDynamicTexture;
    private final ResourceLocation leftTextureLocation;
    private final ResourceLocation rightTextureLocation;

    private int lastSpreadIndex = Integer.MIN_VALUE;
    private int lastTextAlpha = Integer.MIN_VALUE;
    private long lastAnimationBucket = Long.MIN_VALUE;
    private int lastGlitchStrength = Integer.MIN_VALUE;
    private int lastFocusedPageSide = Integer.MIN_VALUE;
    private int lastInteractiveStateKey = Integer.MIN_VALUE;

    BookPageTexturePipeline() {
        this.transparentCanvas = new BufferedImage(DYNAMIC_TEXTURE_SIZE, DYNAMIC_TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        this.leftDynamicTexture = new DynamicTexture(transparentCanvas.getWidth(), transparentCanvas.getHeight(), false);
        this.rightDynamicTexture = new DynamicTexture(transparentCanvas.getWidth(), transparentCanvas.getHeight(), false);
        this.leftTextureLocation = Minecraft.getInstance().getTextureManager().register(LEFT_TEXTURE_PREFIX, leftDynamicTexture);
        this.rightTextureLocation = Minecraft.getInstance().getTextureManager().register(RIGHT_TEXTURE_PREFIX, rightDynamicTexture);
    }

    static RenderRegionSize renderRegionSize(BookPageSide pageSide) {
        float u0 = pageSide == BookPageSide.LEFT ? LEFT_U0 : RIGHT_U0;
        float v0 = pageSide == BookPageSide.LEFT ? LEFT_V0 : RIGHT_V0;
        float u1 = pageSide == BookPageSide.LEFT ? LEFT_U1 : RIGHT_U1;
        float v1 = pageSide == BookPageSide.LEFT ? LEFT_V1 : RIGHT_V1;
        int regionX = uvToPixelX(u0, DYNAMIC_TEXTURE_SIZE);
        int regionY = uvToPixelY(v0, DYNAMIC_TEXTURE_SIZE);
        int regionW = Math.max(4, uvToPixelX(u1, DYNAMIC_TEXTURE_SIZE) - regionX);
        int regionH = Math.max(4, uvToPixelY(v1, DYNAMIC_TEXTURE_SIZE) - regionY);
        return new RenderRegionSize(regionW, regionH);
    }

    static LocalTexturePoint localPointForTextureUv(BookPageSide pageSide, float texU, float texV) {
        float u0 = pageSide == BookPageSide.LEFT ? LEFT_U0 / UV_SPACE_SIZE : RIGHT_U0 / UV_SPACE_SIZE;
        float v0 = pageSide == BookPageSide.LEFT ? LEFT_V0 / UV_SPACE_SIZE : RIGHT_V0 / UV_SPACE_SIZE;
        float u1 = pageSide == BookPageSide.LEFT ? LEFT_U1 / UV_SPACE_SIZE : RIGHT_U1 / UV_SPACE_SIZE;
        float v1 = pageSide == BookPageSide.LEFT ? LEFT_V1 / UV_SPACE_SIZE : RIGHT_V1 / UV_SPACE_SIZE;
        float minU = Math.min(u0, u1);
        float maxU = Math.max(u0, u1);
        float minV = Math.min(v0, v1);
        float maxV = Math.max(v0, v1);
        if (texU < minU - 0.0001f || texU > maxU + 0.0001f || texV < minV - 0.0001f || texV > maxV + 0.0001f) {
            return null;
        }
        RenderRegionSize size = renderRegionSize(pageSide);
        float normalizedU = (texU - minU) / Math.max(0.0001f, maxU - minU);
        float normalizedV = (texV - minV) / Math.max(0.0001f, maxV - minV);
        return new LocalTexturePoint(normalizedU * size.width(), normalizedV * size.height());
    }

    public TextureSet textureSetFor(
            BookSpread spread,
            int spreadIndex,
            float textAlpha,
            float glitchStrength,
            BookPageSide focusedPageSide,
            java.util.List<PageInteractiveNode> leftInteractiveNodes,
            java.util.List<PageInteractiveNode> rightInteractiveNodes,
            PageInteractiveNode leftHoveredInteractiveNode,
            PageInteractiveNode rightHoveredInteractiveNode
    ) {
        int textAlphaKey = Math.clamp(Math.round(Math.clamp(textAlpha, 0.0f, 1.0f) * 255.0f), 0, 255);
        int glitchStrengthKey = Math.clamp(Math.round(Math.clamp(glitchStrength, 0.0f, 1.0f) * 255.0f), 0, 255);
        int focusedPageSideKey = focusedPageSide == null ? -1 : focusedPageSide.ordinal();
        int interactiveStateKey = 31 * interactiveNodeListKey(leftInteractiveNodes) + interactiveNodeListKey(rightInteractiveNodes);
        interactiveStateKey = 31 * interactiveStateKey + interactiveNodeKey(leftHoveredInteractiveNode);
        interactiveStateKey = 31 * interactiveStateKey + interactiveNodeKey(rightHoveredInteractiveNode);
        long animationBucket = currentAnimationBucket();
        if (spreadIndex != lastSpreadIndex || textAlphaKey != lastTextAlpha || animationBucket != lastAnimationBucket || glitchStrengthKey != lastGlitchStrength || focusedPageSideKey != lastFocusedPageSide || interactiveStateKey != lastInteractiveStateKey) {
            updateMagicPlaneTextures(
                    spread,
                    spreadIndex,
                    textAlpha,
                    glitchStrength,
                    focusedPageSide,
                    leftInteractiveNodes,
                    rightInteractiveNodes,
                    leftHoveredInteractiveNode,
                    rightHoveredInteractiveNode
            );
            lastSpreadIndex = spreadIndex;
            lastTextAlpha = textAlphaKey;
            lastAnimationBucket = animationBucket;
            lastGlitchStrength = glitchStrengthKey;
            lastFocusedPageSide = focusedPageSideKey;
            lastInteractiveStateKey = interactiveStateKey;
        }
        return new TextureSet(BASE_TEXTURE, leftTextureLocation, rightTextureLocation);
    }

    private void updateMagicPlaneTextures(
            BookSpread spread,
            int spreadIndex,
            float textAlpha,
            float glitchStrength,
            BookPageSide focusedPageSide,
            java.util.List<PageInteractiveNode> leftInteractiveNodes,
            java.util.List<PageInteractiveNode> rightInteractiveNodes,
            PageInteractiveNode leftHoveredInteractiveNode,
            PageInteractiveNode rightHoveredInteractiveNode
    ) {
        int leftPageNumber = (spreadIndex * 2) + 1;
        int rightPageNumber = leftPageNumber + 1;
        float leftFocusStrength = focusedPageSide == BookPageSide.LEFT ? 1.0f : 0.0f;
        float rightFocusStrength = focusedPageSide == BookPageSide.RIGHT ? 1.0f : 0.0f;
        updatePlaneTexture(leftDynamicTexture, spread.left(), LEFT_U0, LEFT_V0, LEFT_U1, LEFT_V1, textAlpha, focusGlitchStrength(glitchStrength, leftFocusStrength), leftFocusStrength, 0, true, leftPageNumber, leftInteractiveNodes, leftHoveredInteractiveNode);
        updatePlaneTexture(rightDynamicTexture, spread.right(), RIGHT_U0, RIGHT_V0, RIGHT_U1, RIGHT_V1, textAlpha, focusGlitchStrength(glitchStrength, rightFocusStrength), rightFocusStrength, PageCanvasRenderer.RIGHT_PAGE_INSET, false, rightPageNumber, rightInteractiveNodes, rightHoveredInteractiveNode);
    }

    private static float focusGlitchStrength(float baseGlitchStrength, float focusStrength) {
        return baseGlitchStrength * (0.22f + (focusStrength * 1.15f));
    }

    private void updatePlaneTexture(
            DynamicTexture targetTexture,
            BookPage page,
            float u0,
            float v0,
            float u1,
            float v1,
            float textAlpha,
            float glitchStrength,
            float focusStrength,
            int horizontalInset,
            boolean mirrorHorizontally,
            Integer pageNumber,
            java.util.List<PageInteractiveNode> interactiveNodes,
            PageInteractiveNode hoveredInteractiveNode
    ) {
        BufferedImage composed = copyImage(transparentCanvas);
        Graphics2D graphics = composed.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(new Font("Serif", Font.PLAIN, 14));

        int regionX = uvToPixelX(u0, composed.getWidth());
        int regionY = uvToPixelY(v0, composed.getHeight());
        int regionW = Math.max(4, uvToPixelX(u1, composed.getWidth()) - regionX);
        int regionH = Math.max(4, uvToPixelY(v1, composed.getHeight()) - regionY);
        boolean hasFocusedSide = focusStrength > 0.01f || glitchStrength > 0.01f;
        float effectiveTextAlpha = focusStrength > 0.01f ? textAlpha : (hasFocusedSide ? textAlpha * 0.38f : textAlpha);

        if (MAGIC_TEXT_PANEL_ALPHA > 0) {
            graphics.setColor(new Color(230, 220, 200, MAGIC_TEXT_PANEL_ALPHA));
            graphics.fillRect(regionX, regionY, regionW, regionH);
        }
        BufferedImage pageImage = new BufferedImage(regionW, regionH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pageGraphics = pageImage.createGraphics();
        pageGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        pageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pageGraphics.setFont(new Font("Serif", Font.PLAIN, 14));
        pageCanvasRenderer.renderPageTextOnlyToGraphics2D(
                pageGraphics,
                page,
                0,
                0,
                regionW,
                regionH,
                effectiveTextAlpha,
                horizontalInset,
                pageNumber,
                glitchStrength,
                interactiveNodes,
                hoveredInteractiveNode
        );
        pageGraphics.dispose();

        if (mirrorHorizontally) {
            graphics.drawImage(
                    pageImage,
                    regionX + regionW,
                    regionY,
                    regionX,
                    regionY + regionH,
                    0,
                    0,
                    pageImage.getWidth(),
                    pageImage.getHeight(),
                    null
            );
        } else {
            graphics.drawImage(pageImage, regionX, regionY, null);
        }
        graphics.dispose();

        uploadBufferedImage(targetTexture, composed);
    }

    private void uploadBufferedImage(DynamicTexture targetTexture, BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            out.flush();
            try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
                NativeImage pixels = NativeImage.read(in);
                targetTexture.setPixels(pixels);
                targetTexture.upload();
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to upload dynamic magical text texture", exception);
        }
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static int uvToPixelX(float uv, int width) {
        return Math.round((uv / UV_SPACE_SIZE) * width);
    }

    private static int uvToPixelY(float uv, int height) {
        return Math.round((uv / UV_SPACE_SIZE) * height);
    }

    private static long currentAnimationBucket() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0L;
        }
        return minecraft.level.getGameTime() / 3L;
    }

    private static int interactiveNodeKey(PageInteractiveNode node) {
        if (node == null) {
            return 0;
        }
        int key = 1;
        key = 31 * key + node.stableId().hashCode();
        key = 31 * key + node.localX();
        key = 31 * key + node.localY();
        key = 31 * key + node.localWidth();
        key = 31 * key + node.localHeight();
        key = 31 * key + node.visualType().ordinal();
        return key;
    }

    private static int interactiveNodeListKey(java.util.List<PageInteractiveNode> nodes) {
        int key = 1;
        if (nodes == null) {
            return key;
        }
        for (PageInteractiveNode node : nodes) {
            key = 31 * key + interactiveNodeKey(node);
        }
        return key;
    }

    record TextureSet(ResourceLocation baseTexture, ResourceLocation magicLeftTexture, ResourceLocation magicRightTexture) {}
    record RenderRegionSize(int width, int height) {}
    record LocalTexturePoint(float localX, float localY) {}
}
