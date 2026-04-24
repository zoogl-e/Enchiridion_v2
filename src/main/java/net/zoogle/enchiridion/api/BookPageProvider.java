package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public interface BookPageProvider {
    BookSpread getSpread(BookContext context, int spreadIndex);

    default int spreadCount(BookContext context) {
        return 1;
    }

    default List<BookInteractiveRegion> interactiveRegions(BookContext context, int spreadIndex) {
        return List.of();
    }

    default List<BookTrackedRegion> trackedInteractiveRegions(BookContext context, int spreadIndex) {
        return List.of();
    }

    default List<BookPageTextDecoration> pageTextDecorations(BookContext context, int spreadIndex) {
        return List.of();
    }

    default List<BookPageInteractiveText> pageInteractiveText(BookContext context, int spreadIndex) {
        return List.of();
    }

    default @Nullable BookProjectionView projection(BookContext context, String focusId) {
        return null;
    }

    default @Nullable String nextProjectionFocus(BookContext context, String currentFocusId) {
        return null;
    }

    default @Nullable String previousProjectionFocus(BookContext context, String currentFocusId) {
        return null;
    }

    default int spreadForProjectionFocus(BookContext context, String focusId) {
        return -1;
    }

    default int pageIndexForProjectionFocus(BookContext context, String focusId) {
        return -1;
    }

    default int initialSpreadIndex(BookContext context) {
        return 0;
    }

    default Component displayTitle(BookContext context, Component defaultTitle) {
        return defaultTitle;
    }

    default BookFrontCoverCardState frontCoverCardState(BookContext context) {
        return showFrontCoverCard(context) ? BookFrontCoverCardState.visible(null) : BookFrontCoverCardState.hidden();
    }

    default boolean showFrontCoverCard(BookContext context) {
        return true;
    }

    default boolean isFrontSpread(BookContext context, int spreadIndex) {
        return false;
    }

    default boolean isBackSpread(BookContext context, int spreadIndex) {
        return false;
    }
}
