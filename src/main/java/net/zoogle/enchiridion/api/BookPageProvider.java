package net.zoogle.enchiridion.api;

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
}
