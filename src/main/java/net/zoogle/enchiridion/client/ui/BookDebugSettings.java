package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.client.levelrpg.JournalPageStyleSystem;

public final class BookDebugSettings {
    private static boolean showGuiTextDebug = false;
    private static boolean pageLocalInteractionDebug = false;
    private static boolean hoveredInteractiveBoundsDebug = false;
    private static boolean interactiveTextBoundsDebug = false;

    private BookDebugSettings() {}

    public static boolean showGuiTextDebug() {
        return showGuiTextDebug;
    }

    public static boolean pageLocalInteractionDebug() {
        return pageLocalInteractionDebug;
    }

    public static boolean hoveredInteractiveBoundsDebug() {
        return hoveredInteractiveBoundsDebug;
    }

    public static boolean interactiveTextBoundsDebug() {
        return interactiveTextBoundsDebug;
    }

    public static boolean templateLayoutDebug() {
        return JournalPageStyleSystem.debugTemplateLayoutEnabled();
    }

    public static boolean anyDebugEnabled() {
        return showGuiTextDebug
                || pageLocalInteractionDebug
                || hoveredInteractiveBoundsDebug
                || interactiveTextBoundsDebug
                || templateLayoutDebug();
    }

    public static void setAllDebugEnabled(boolean enabled) {
        showGuiTextDebug = enabled;
        pageLocalInteractionDebug = enabled;
        hoveredInteractiveBoundsDebug = enabled;
        interactiveTextBoundsDebug = enabled;
        JournalPageStyleSystem.setDebugTemplateLayoutEnabled(enabled);
    }
}
