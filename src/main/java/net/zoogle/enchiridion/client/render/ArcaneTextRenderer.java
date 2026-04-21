package net.zoogle.enchiridion.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.zoogle.enchiridion.api.BookTextBlock;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Objects;

final class ArcaneTextRenderer {
    enum TextRenderMode {
        PLAIN,
        ENCHANTED_TRANSLATING
    }

    // Java2D is responsible for the page texture path, so this glyph set must stay inside broadly-supported characters.
    // True Minecraft SGA would require a font-backed render path instead of Graphics2D text drawing.
    private static final char[] ENCHANTMENT_GLYPHS = "@#$%&*+=?/\\|~^<>[]{}0123456789".toCharArray();
    private static final float TRANSITION_GLYPH_PRIMARY_ALPHA = 0.72f;
    private static final float TRANSITION_GLYPH_ECHO_ALPHA = 0.42f;
    private static final float TITLE_EFFECT_STRENGTH = 1.0f;
    private static final float SUBTITLE_EFFECT_STRENGTH = 0.82f;
    private static final float BODY_EFFECT_STRENGTH = 0.64f;
    private static final float TITLE_FLOAT_AMPLITUDE = 0.40f;
    private static final float SUBTITLE_FLOAT_AMPLITUDE = 0.28f;
    private static final float BODY_FLOAT_AMPLITUDE = 0.18f;

    public void renderGraphicsLine(
            Graphics2D graphics,
            String englishText,
            float x,
            float baselineY,
            int argbColor,
            BookTextBlock.Kind kind,
            float revealProgress,
            long lineSeed,
            float glitchStrength
    ) {
        Objects.requireNonNull(graphics, "graphics");
        if (englishText == null || englishText.isEmpty()) {
            return;
        }

        float clampedReveal = clamp(revealProgress, 0.0f, 1.0f);
        int baseAlpha = (argbColor >>> 24) & 0xFF;
        if (baseAlpha <= 0) {
            return;
        }

        float glitchPulse = 0.5f + (0.5f * (float) Math.sin((arcaneTime() * 0.85f) + ((lineSeed >>> 18) & 0xFF)));
        float corruptedReveal = clamp(clampedReveal - (glitchStrength * 0.28f * glitchPulse), 0.0f, 1.0f);
        String translatedDisplay = translatedDisplayText(englishText, corruptedReveal, kind, lineSeed);
        float effectStrength = effectStrength(kind);
        float time = arcaneTime();
        float translationVisibility = 1.0f - smoothstep(0.18f, 0.88f, corruptedReveal);
        float readableVisibility = smoothstep(0.08f, 0.82f, corruptedReveal);
        float lineFloatY = lineFloat(time, lineSeed, kind) * clampedReveal;

        if (translationVisibility > 0.001f) {
            int glyphPrimaryColor = withScaledAlpha(0xFF6B572E, baseAlpha / 255.0f * translationVisibility * TRANSITION_GLYPH_PRIMARY_ALPHA);
            graphics.setColor(new Color(glyphPrimaryColor, true));
            graphics.drawString(translatedDisplay, x, baselineY + lineFloatY);

            int glyphEchoColor = withScaledAlpha(0xFFB49B63, baseAlpha / 255.0f * translationVisibility * TRANSITION_GLYPH_ECHO_ALPHA);
            graphics.setColor(new Color(glyphEchoColor, true));
            graphics.drawString(translatedDisplay, x + 0.55f, baselineY + lineFloatY + 0.4f);
        }

        int readableBaseColor = withScaledAlpha(argbColor, readableVisibility);
        graphics.setColor(new Color(readableBaseColor, true));
        graphics.drawString(englishText, x, baselineY + lineFloatY);
        renderGraphicsOverlay(graphics, englishText, x, baselineY + lineFloatY, baseAlpha / 255.0f, kind, clamp(corruptedReveal + (glitchStrength * 0.08f), 0.0f, 1.0f), lineSeed, time, effectStrength);
    }

    public void renderGuiLine(
            GuiGraphics graphics,
            Font font,
            String englishText,
            float x,
            float y,
            int argbColor,
            BookTextBlock.Kind kind,
            float revealProgress,
            long lineSeed,
            float glitchStrength
    ) {
        if (englishText == null || englishText.isEmpty()) {
            return;
        }

        float clampedReveal = clamp(revealProgress, 0.0f, 1.0f);
        int baseAlpha = (argbColor >>> 24) & 0xFF;
        if (baseAlpha <= 0) {
            return;
        }

        float glitchPulse = 0.5f + (0.5f * (float) Math.sin((arcaneTime() * 0.85f) + ((lineSeed >>> 18) & 0xFF)));
        float corruptedReveal = clamp(clampedReveal - (glitchStrength * 0.28f * glitchPulse), 0.0f, 1.0f);
        String translatedDisplay = translatedDisplayText(englishText, corruptedReveal, kind, lineSeed);
        float effectStrength = effectStrength(kind);
        float time = arcaneTime();
        float translationVisibility = 1.0f - smoothstep(0.18f, 0.88f, corruptedReveal);
        float readableVisibility = smoothstep(0.08f, 0.82f, corruptedReveal);
        float lineFloatY = lineFloat(time, lineSeed, kind) * clampedReveal;

        if (translationVisibility > 0.001f) {
            int glyphPrimaryColor = withScaledAlpha(0xFF6B572E, baseAlpha / 255.0f * translationVisibility * TRANSITION_GLYPH_PRIMARY_ALPHA);
            graphics.drawString(font, translatedDisplay, Math.round(x), Math.round(y + lineFloatY), glyphPrimaryColor, false);

            int glyphEchoColor = withScaledAlpha(0xFFB49B63, baseAlpha / 255.0f * translationVisibility * TRANSITION_GLYPH_ECHO_ALPHA);
            graphics.drawString(font, translatedDisplay, Math.round(x + 1.0f), Math.round(y + lineFloatY + 1.0f), glyphEchoColor, false);
        }

        int readableBaseColor = withScaledAlpha(argbColor, readableVisibility);
        graphics.drawString(font, englishText, Math.round(x), Math.round(y + lineFloatY), readableBaseColor, false);
        renderGuiOverlay(graphics, font, englishText, x, y + lineFloatY, baseAlpha / 255.0f, kind, clamp(corruptedReveal + (glitchStrength * 0.08f), 0.0f, 1.0f), lineSeed, time, effectStrength);
    }

    private void renderGraphicsOverlay(
            Graphics2D graphics,
            String englishText,
            float x,
            float baselineY,
            float baseAlpha,
            BookTextBlock.Kind kind,
            float revealProgress,
            long lineSeed,
            float time,
            float effectStrength
    ) {
        if (revealProgress <= 0.05f) {
            return;
        }

        float glowAlpha = baseAlpha * revealProgress * effectStrength * overlayAlphaMultiplier(kind) * (0.22f + (0.10f * shimmerPulse(time, lineSeed)));
        float charX = x;
        for (int index = 0; index < englishText.length(); index++) {
            char character = englishText.charAt(index);
            String rendered = String.valueOf(character);
            long charSeed = mix64(lineSeed + (index * 0x9E3779B97F4A7C15L));
            float wobbleX = wobble(time, charSeed, 0.34f);
            float wobbleY = wobble(time * 0.91f, charSeed ^ 0x5DEECE66DL, 0.30f);
            float shimmer = 0.55f + (0.45f * shimmerSweep(time, index, charSeed));
            int spectralGlowColor = prismaticColor(kind, shimmer, 0.72f);
            int glowColor = withScaledAlpha(spectralGlowColor, glowAlpha * (0.24f + (0.18f * shimmer)));
            graphics.setColor(new Color(glowColor, true));
            graphics.drawString(rendered, charX + (wobbleX * 0.35f), baselineY + 0.45f + (wobbleY * 0.35f));
            int overlayColor = withScaledAlpha(prismaticColor(kind, shimmer, 1.0f), glowAlpha * (0.52f + (0.28f * shimmer)));
            graphics.setColor(new Color(overlayColor, true));
            graphics.drawString(rendered, charX + wobbleX, baselineY + wobbleY);
            charX += graphics.getFontMetrics().stringWidth(rendered);
        }
    }

    private void renderGuiOverlay(
            GuiGraphics graphics,
            Font font,
            String englishText,
            float x,
            float y,
            float baseAlpha,
            BookTextBlock.Kind kind,
            float revealProgress,
            long lineSeed,
            float time,
            float effectStrength
    ) {
        if (revealProgress <= 0.05f) {
            return;
        }

        float glowAlpha = baseAlpha * revealProgress * effectStrength * overlayAlphaMultiplier(kind) * (0.22f + (0.10f * shimmerPulse(time, lineSeed)));
        float charX = x;
        for (int index = 0; index < englishText.length(); index++) {
            char character = englishText.charAt(index);
            String rendered = String.valueOf(character);
            long charSeed = mix64(lineSeed + (index * 0x9E3779B97F4A7C15L));
            float wobbleX = wobble(time, charSeed, 0.34f);
            float wobbleY = wobble(time * 0.91f, charSeed ^ 0x5DEECE66DL, 0.30f);
            float shimmer = 0.55f + (0.45f * shimmerSweep(time, index, charSeed));
            int spectralGlowColor = prismaticColor(kind, shimmer, 0.72f);
            int glowColor = withScaledAlpha(spectralGlowColor, glowAlpha * (0.24f + (0.18f * shimmer)));
            graphics.drawString(font, rendered, Math.round(charX + (wobbleX * 0.35f)), Math.round(y + 0.45f + (wobbleY * 0.35f)), glowColor, false);
            int overlayColor = withScaledAlpha(prismaticColor(kind, shimmer, 1.0f), glowAlpha * (0.52f + (0.28f * shimmer)));
            graphics.drawString(font, rendered, Math.round(charX + wobbleX), Math.round(y + wobbleY), overlayColor, false);
            charX += font.width(rendered);
        }
    }

    private String translatedDisplayText(String englishText, float revealProgress, BookTextBlock.Kind kind, long lineSeed) {
        StringBuilder translated = new StringBuilder(englishText.length());
        int visibleCharacters = 0;
        for (int i = 0; i < englishText.length(); i++) {
            if (Character.isLetterOrDigit(englishText.charAt(i))) {
                visibleCharacters++;
            }
        }

        int visibleIndex = 0;
        for (int index = 0; index < englishText.length(); index++) {
            char character = englishText.charAt(index);
            if (!Character.isLetterOrDigit(character)) {
                translated.append(character);
                continue;
            }

            float resolveThreshold = resolveThreshold(visibleIndex, Math.max(visibleCharacters, 1), kind, lineSeed);
            translated.append(revealProgress >= resolveThreshold ? character : glyphFor(character, visibleIndex, lineSeed));
            visibleIndex++;
        }
        return translated.toString();
    }

    private float resolveThreshold(int visibleIndex, int visibleCharacters, BookTextBlock.Kind kind, long lineSeed) {
        float progression = visibleCharacters <= 1 ? 0.0f : (float) visibleIndex / (visibleCharacters - 1);
        float seededVariance = random01(lineSeed + (visibleIndex * 0x632BE59BD9B4E019L));
        float kindBias = switch (kind) {
            case TITLE -> 0.08f;
            case LEVEL -> 0.10f;
            case SUBTITLE -> 0.04f;
            case SECTION -> 0.02f;
            case BODY -> 0.0f;
        };
        return clamp((progression * 0.68f) + (seededVariance * 0.18f) + kindBias, 0.0f, 1.0f);
    }

    private char glyphFor(char originalCharacter, int visibleIndex, long lineSeed) {
        long glyphSeed = mix64(lineSeed + originalCharacter + (visibleIndex * 0x9E3779B97F4A7C15L));
        int glyphIndex = (int) Math.floorMod(glyphSeed, ENCHANTMENT_GLYPHS.length);
        return ENCHANTMENT_GLYPHS[glyphIndex];
    }

    private static float effectStrength(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> TITLE_EFFECT_STRENGTH;
            case LEVEL -> TITLE_EFFECT_STRENGTH * 1.08f;
            case SUBTITLE -> SUBTITLE_EFFECT_STRENGTH;
            case SECTION -> SUBTITLE_EFFECT_STRENGTH * 0.7f;
            case BODY -> BODY_EFFECT_STRENGTH;
        };
    }

    private static float lineFloat(float time, long lineSeed, BookTextBlock.Kind kind) {
        float amplitude = switch (kind) {
            case TITLE -> TITLE_FLOAT_AMPLITUDE;
            case LEVEL -> TITLE_FLOAT_AMPLITUDE * 0.9f;
            case SUBTITLE -> SUBTITLE_FLOAT_AMPLITUDE;
            case SECTION -> SUBTITLE_FLOAT_AMPLITUDE * 0.7f;
            case BODY -> BODY_FLOAT_AMPLITUDE;
        };
        float phase = ((lineSeed >>> 12) & 0xFFFF) / 65535.0f * (float) (Math.PI * 2.0);
        return (float) (Math.sin((time * 0.12f) + phase) * amplitude);
    }

    private static int overlayColor(BookTextBlock.Kind kind, float shimmer) {
        int warm = switch (kind) {
            case TITLE -> 0xFF8F54D8;
            case LEVEL -> 0xFFB066FF;
            case SUBTITLE -> 0xFFC88B2A;
            case SECTION -> 0xFFB8832E;
            case BODY -> 0xFF7C47C2;
        };
        int cool = switch (kind) {
            case SUBTITLE, SECTION -> 0xFFE0A446;
            default -> 0xFFB46BFF;
        };
        return lerpColor(warm, cool, 0.18f + (shimmer * 0.42f));
    }

    private static int prismaticColor(BookTextBlock.Kind kind, float shimmer, float intensity) {
        int base = overlayColor(kind, shimmer);
        int prismA = switch (kind) {
            case TITLE -> 0xFFD14BFF;
            case LEVEL -> 0xFFE06AFF;
            case SUBTITLE -> 0xFFFFC247;
            case SECTION -> 0xFFF0BE64;
            case BODY -> 0xFF9A4DFF;
        };
        int prismB = switch (kind) {
            case TITLE -> 0xFFE18CFF;
            case LEVEL -> 0xFFF0A2FF;
            case SUBTITLE -> 0xFFFF8A3D;
            case SECTION -> 0xFFFFA64C;
            case BODY -> 0xFFC873FF;
        };
        int prismBlend = lerpColor(prismA, prismB, shimmer);
        return lerpColor(base, prismBlend, clamp(intensity, 0.0f, 1.0f) * 0.72f);
    }

    private static float overlayAlphaMultiplier(BookTextBlock.Kind kind) {
        return switch (kind) {
            case TITLE -> 1.35f;
            case LEVEL -> 1.45f;
            case SUBTITLE -> 1.0f;
            case SECTION -> 0.95f;
            case BODY -> 1.0f;
        };
    }

    static long lineSeed(String text, BookTextBlock.Kind kind, int lineIndex) {
        long seed = 0xC6A4A7935BD1E995L;
        seed ^= text.hashCode();
        seed = mix64(seed + (kind.ordinal() * 0x9E3779B97F4A7C15L));
        return mix64(seed + lineIndex);
    }

    private static float wobble(float time, long seed, float amplitude) {
        float phase = ((seed >>> 8) & 0xFFFF) / 65535.0f * (float) (Math.PI * 2.0);
        return (float) (Math.sin((time * 0.29f) + phase) * amplitude);
    }

    private static float shimmerPulse(float time, long seed) {
        float phase = ((seed >>> 24) & 0xFFFF) / 65535.0f * (float) (Math.PI * 2.0);
        return 0.5f + (0.5f * (float) Math.sin((time * 0.18f) + phase));
    }

    private static float shimmerSweep(float time, int charIndex, long seed) {
        float phase = ((seed >>> 40) & 0xFFFF) / 65535.0f * (float) (Math.PI * 2.0);
        return 0.5f + (0.5f * (float) Math.sin((time * 0.35f) - (charIndex * 0.42f) + phase));
    }

    private static float random01(long seed) {
        return ((mix64(seed) >>> 40) & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static int withScaledAlpha(int argbColor, float alphaScale) {
        int alpha = (argbColor >>> 24) & 0xFF;
        int scaledAlpha = Math.clamp(Math.round(alpha * clamp(alphaScale, 0.0f, 1.0f)), 0, 255);
        return (argbColor & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    private static int lerpColor(int from, int to, float t) {
        float clamped = clamp(t, 0.0f, 1.0f);
        int a = lerpChannel((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, clamped);
        int r = lerpChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, clamped);
        int g = lerpChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, clamped);
        int b = lerpChannel(from & 0xFF, to & 0xFF, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.clamp(Math.round(from + ((to - from) * t)), 0, 255);
    }

    private static float arcaneTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0f;
        }
        return minecraft.level.getGameTime();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - (2.0f * t));
    }
}
