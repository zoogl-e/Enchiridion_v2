package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.api.BookTemplateDebugProvider;

import java.util.List;

public final class LevelRpgJournalBookProvider implements BookPageProvider, BookTemplateDebugProvider {
    private static final LevelRpgJournalIntroFlowState INTRO_FLOW = LevelRpgJournalIntroFlowState.get();
    private static final LevelRpgJournalActionPolicy ACTION_POLICY = new LevelRpgJournalActionPolicy();
    private static final int FRONT_COVER_SPREAD_INDEX = 0;
    private static final int INTERIOR_SPREAD_OFFSET = 1;
    private static final int INTERIOR_PAGE_OFFSET = 2;

    @Override
    public int spreadCount(BookContext context) {
        return document(context).spreads().size() + INTERIOR_SPREAD_OFFSET;
    }

    @Override
    public BookSpread getSpread(BookContext context, int spreadIndex) {
        if (spreadIndex == FRONT_COVER_SPREAD_INDEX) {
            if (INTRO_FLOW.isUnbound(context)) {
                return BookSpread.of(BookPage.empty(), BookPage.empty());
            }
            String playerName = context != null && context.player() != null
                    ? context.player().getName().getString()
                    : "Wanderer";
            BookPage rightPage = BookPage.of(
                    BookTextBlock.title(Component.literal(playerName + "'s Legacy"))
            );
            return BookSpread.of(BookPage.empty(), rightPage);
        }
        List<BookSpread> spreads = document(context).spreads();
        int interiorIndex = spreadIndex - INTERIOR_SPREAD_OFFSET;
        return interiorIndex >= 0 && interiorIndex < spreads.size()
                ? spreads.get(interiorIndex)
                : BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    @Override
    public List<BookInteractiveRegion> interactiveRegions(BookContext context, int spreadIndex) {
        if (spreadIndex == FRONT_COVER_SPREAD_INDEX) {
            return List.of();
        }
        return document(context).interactiveRegionsBySpread().getOrDefault(spreadIndex - INTERIOR_SPREAD_OFFSET, List.of());
    }

    @Override
    public List<BookTrackedRegion> trackedInteractiveRegions(BookContext context, int spreadIndex) {
        if (!isFrontSpread(context, spreadIndex)) {
            return List.of();
        }
        BookTrackedRegion frontRegion = frontCoverTrackedRegion(context);
        return frontRegion != null ? List.of(frontRegion) : List.of();
    }

    @Override
    public BookProjectionView projection(BookContext context, String focusId) {
        if (focusId == null || focusId.isBlank()) {
            return null;
        }
        INTRO_FLOW.syncToTruth(context);
        return archetypeProjection(context, focusId);
    }

    @Override
    public String nextProjectionFocus(BookContext context, String currentFocusId) {
        return adjacentProjectionFocus(document(context).projectionFocusOrder(), currentFocusId, 1);
    }

    @Override
    public String previousProjectionFocus(BookContext context, String currentFocusId) {
        return adjacentProjectionFocus(document(context).projectionFocusOrder(), currentFocusId, -1);
    }

    @Override
    public int spreadForProjectionFocus(BookContext context, String focusId) {
        int interiorSpread = document(context).projectionSpreadByFocus().getOrDefault(focusId, -1);
        return interiorSpread < 0 ? -1 : interiorSpread + INTERIOR_SPREAD_OFFSET;
    }

    @Override
    public int pageIndexForProjectionFocus(BookContext context, String focusId) {
        int interiorPage = document(context).projectionPageByFocus().getOrDefault(focusId, -1);
        return interiorPage < 0 ? -1 : interiorPage + INTERIOR_PAGE_OFFSET;
    }

    @Override
    public String templatePurposeForPageIndex(BookContext context, int pageIndex) {
        return document(context).pagePurposeByPage().get(pageIndex - INTERIOR_PAGE_OFFSET);
    }

    @Override
    public String templatePageIdForPageIndex(BookContext context, int pageIndex) {
        return document(context).pageIdByPage().get(pageIndex - INTERIOR_PAGE_OFFSET);
    }

    @Override
    public int initialSpreadIndex(BookContext context) {
        INTRO_FLOW.syncToTruth(context);
        int preferred = firstReadableSpreadIndex();
        return Math.clamp(preferred, 0, Math.max(0, spreadCount(context) - 1));
    }

    @Override
    public Component displayTitle(BookContext context, Component defaultTitle) {
        INTRO_FLOW.syncToTruth(context);
        return INTRO_FLOW.isUnbound(context) ? Component.empty() : defaultTitle;
    }

    @Override
    public boolean showFrontCoverCard(BookContext context) {
        return frontCoverCardState(context).visible();
    }

    @Override
    public BookFrontCoverCardState frontCoverCardState(BookContext context) {
        return cardState(context);
    }

    @Override
    public boolean isFrontSpread(BookContext context, int spreadIndex) {
        return spreadIndex == FRONT_COVER_SPREAD_INDEX;
    }

    @Override
    public boolean isBackSpread(BookContext context, int spreadIndex) {
        return spreadIndex == Math.max(0, spreadCount(context) - 1);
    }

    private static LevelRpgJournalDocument document(BookContext context) {
        return LevelRpgJournalDocumentBuilder.build(context);
    }

    private static BookProjectionView archetypeProjection(BookContext context, String focusId) {
        boolean unbound = INTRO_FLOW.isUnbound(context);
        ResourceLocation selectedArchetypeId = LevelRpgJournalIntroFlowState.get().selectedArchetypeId(context);
        for (JournalArchetypeChoice choice : LevelRpgArchetypeBindingBridge.availableArchetypes()) {
            if (!focusId.equals(choice.focusId())) {
                continue;
            }
            if (!unbound && (selectedArchetypeId == null || !focusId.equals(selectedArchetypeId.toString()))) {
                return null;
            }
            return new BookProjectionView(
                    choice.focusId(),
                    Component.literal(choice.title()),
                    Component.literal("Archetype"),
                    Component.literal(choice.title()),
                    Component.literal(choice.description().isBlank() ? "No inscription accompanies this archetype yet." : choice.description()),
                    Component.literal(choice.affectedSpreadText()),
                    1.0f,
                    Component.literal(unbound ? "Confirm and bind this path" : "This path is already bound into the cover."),
                    unbound,
                    unbound ? Component.literal("Bind Archetype") : null,
                    unbound
                            ? (bookContext, spreadIndex, mouseButton) -> ACTION_POLICY.bindArchetype(bookContext, choice.focusId(), mouseButton)
                            : null
            );
        }
        return null;
    }

    private static BookTrackedRegion frontCoverTrackedRegion(BookContext context) {
        BookFrontCoverCardState cardState = cardState(context);
        if (!cardState.clickable()) {
            return null;
        }
        String focusId = focusIdForCardState(cardState);
        Component hoverText = cardState.boundArchetypeId() != null
                ? Component.literal("Your archetype is inscribed into this journal.")
                : Component.literal("Seat an archetype card in the waiting recess.");
        return BookTrackedRegion.of(
                BookTrackedRegion.Anchor.FRONT_COVER_CARD,
                hoverText,
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && ACTION_POLICY.handleFrontCoverCardInteraction(bookContext, cardState, focusId)
        );
    }

    private static BookFrontCoverCardState cardState(BookContext context) {
        INTRO_FLOW.syncToTruth(context);
        ResourceLocation boundArchetypeId = LevelRpgArchetypeBindingBridge.selectedArchetypeId(context);
        ResourceLocation selectedArchetypeId = selectedArchetypeId(context);
        return new BookFrontCoverCardState(
                boundArchetypeId != null,
                isCardAreaClickable(context, boundArchetypeId, selectedArchetypeId),
                null,
                selectedArchetypeId,
                boundArchetypeId
        );
    }

    private static boolean isCardAreaClickable(BookContext context, ResourceLocation boundArchetypeId, ResourceLocation selectedArchetypeId) {
        if (boundArchetypeId != null) {
            return true;
        }
        return selectedArchetypeId != null || INTRO_FLOW.isUnbound(context);
    }

    private static ResourceLocation selectedArchetypeId(BookContext context) {
        JournalArchetypeChoice selectedChoice = INTRO_FLOW.selectedChoice();
        if (selectedChoice != null) {
            try {
                return ResourceLocation.parse(selectedChoice.focusId());
            } catch (Exception ignored) {
                return null;
            }
        }
        return INTRO_FLOW.selectedArchetypeId(context);
    }

    private static String focusIdForCardState(BookFrontCoverCardState cardState) {
        ResourceLocation displayId = cardState.displayedArchetypeId();
        return displayId == null ? null : displayId.toString();
    }

    private static String adjacentProjectionFocus(List<String> focusOrder, String currentFocusId, int direction) {
        if (currentFocusId == null || currentFocusId.isBlank() || focusOrder.isEmpty()) {
            return null;
        }
        int currentIndex = focusOrder.indexOf(currentFocusId);
        if (currentIndex < 0) {
            return null;
        }
        int nextIndex = Math.floorMod(currentIndex + direction, focusOrder.size());
        return focusOrder.get(nextIndex);
    }

    private static int firstReadableSpreadIndex() {
        return INTERIOR_SPREAD_OFFSET;
    }
}
