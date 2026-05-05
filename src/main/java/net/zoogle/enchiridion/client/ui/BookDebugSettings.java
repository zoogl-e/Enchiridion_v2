package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPageStyleSystem;

public final class BookDebugSettings {
    private static boolean templateRegionsDebug = false;
    private static boolean measuredTextBoundsDebug = false;
    private static boolean interactionBoundsDebug = false;
    private static boolean debugLabels = false;

    private BookDebugSettings() {}

    public static boolean pageLocalInteractionDebug() {
        return interactionBoundsDebug;
    }

    public static boolean hoveredInteractiveBoundsDebug() {
        return interactionBoundsDebug;
    }

    public static boolean interactiveTextBoundsDebug() {
        return interactionBoundsDebug;
    }

    public static boolean templateRegionsDebug() {
        return templateRegionsDebug;
    }

    public static boolean measuredTextBoundsDebug() {
        return measuredTextBoundsDebug;
    }

    public static boolean interactionBoundsDebug() {
        return interactionBoundsDebug;
    }

    public static boolean debugLabels() {
        return debugLabels;
    }

    public static boolean templateLayoutDebug() {
        return templateRegionsDebug || measuredTextBoundsDebug;
    }

    public static boolean anyDebugEnabled() {
        return templateRegionsDebug
                || measuredTextBoundsDebug
                || interactionBoundsDebug
                || debugLabels;
    }

    public static void setAllDebugEnabled(boolean enabled) {
        templateRegionsDebug = enabled;
        measuredTextBoundsDebug = enabled;
        interactionBoundsDebug = enabled;
        debugLabels = enabled;
        JournalPageStyleSystem.setDebugTemplateLayoutEnabled(templateLayoutDebug());
    }

    public static void setTemplateRegionsDebug(boolean enabled) {
        templateRegionsDebug = enabled;
        JournalPageStyleSystem.setDebugTemplateLayoutEnabled(templateLayoutDebug());
    }

    public static void setMeasuredTextBoundsDebug(boolean enabled) {
        measuredTextBoundsDebug = enabled;
        JournalPageStyleSystem.setDebugTemplateLayoutEnabled(templateLayoutDebug());
    }

    public static void setInteractionBoundsDebug(boolean enabled) {
        interactionBoundsDebug = enabled;
    }

    public static void setDebugLabels(boolean enabled) {
        debugLabels = enabled;
    }
}
