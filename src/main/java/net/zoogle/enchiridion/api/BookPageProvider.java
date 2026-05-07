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

    /**
     * Legacy compatibility hook for providers that modeled the front section as a fake spread index.
     *
     * @deprecated Section-aware providers should keep spread indexes as interior indexes and expose
     * front content through {@link #hasSpecialSection(BookContext, BookSection)} and
     * {@link #getSpecialSectionSpread(BookContext, BookSection)}.
     */
    @Deprecated
    default boolean isFrontSpread(BookContext context, int spreadIndex) {
        return false;
    }

    /**
     * Legacy compatibility hook for providers that modeled the back section as a fake spread index.
     *
     * @deprecated Section-aware providers should keep spread indexes as interior indexes and expose
     * back content through {@link #hasSpecialSection(BookContext, BookSection)} and
     * {@link #getSpecialSectionSpread(BookContext, BookSection)}.
     */
    @Deprecated
    default boolean isBackSpread(BookContext context, int spreadIndex) {
        return false;
    }

    /**
     * Resolves the logical section for a spread index.
     *
     * <p>Spread indexes are interior indexes by default. Section-aware providers should not represent
     * front/back sections as fake spread indexes; LevelRPG uses {@link #hasSpecialSection(BookContext, BookSection)}
     * and {@link #getSpecialSectionSpread(BookContext, BookSection)} for {@link BookSection#FRONT_SPECIAL}
     * and {@link BookSection#BACK_SPECIAL}.
     *
     * <p>Legacy providers may still override this method, or the deprecated fake-spread hooks above, to
     * preserve old behavior.
     */
    default BookSection getSectionForSpread(BookContext context, int spreadIndex) {
        return BookSection.INTERIOR;
    }

    /**
     * Future-facing special section presence check.
     * Defaults to false so existing providers remain behaviorally unchanged.
     */
    default boolean hasSpecialSection(BookContext context, BookSection section) {
        return false;
    }

    /**
     * Future-facing special section content hook.
     * Defaults to an empty spread so existing providers remain behaviorally unchanged.
     */
    default BookSpread getSpecialSectionSpread(BookContext context, BookSection section) {
        return BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    /**
     * Future-facing interaction regions for special sections.
     * Defaults to no regions so existing providers remain behaviorally unchanged.
     */
    default List<BookInteractiveRegion> specialSectionInteractiveRegions(BookContext context, BookSection section) {
        return List.of();
    }

    /**
     * Future-facing tracked interactions for special sections (e.g. front cover card anchors).
     */
    default List<BookTrackedRegion> specialSectionTrackedInteractiveRegions(BookContext context, BookSection section) {
        return List.of();
    }
}
