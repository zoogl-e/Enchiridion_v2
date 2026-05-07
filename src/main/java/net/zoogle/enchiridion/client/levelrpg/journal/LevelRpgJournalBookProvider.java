package net.zoogle.enchiridion.client.levelrpg.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContentMode;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSection;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.api.BookTemplateDebugProvider;
import net.zoogle.enchiridion.client.levelrpg.LevelRpgJournalActionPolicy;
import net.zoogle.enchiridion.client.levelrpg.archetype.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgArchetypeBindingBridge;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgJournalInteractionBridge;
import net.zoogle.enchiridion.client.levelrpg.archetype.LevelRpgJournalIntroFlowState;
import net.zoogle.enchiridion.client.levelrpg.bounty.JournalBountyCatalog;
import net.zoogle.enchiridion.client.levelrpg.bounty.JournalBountyOffer;
import net.zoogle.enchiridion.client.levelrpg.bounty.JournalBountyOfferSnapshot;
import net.zoogle.enchiridion.client.ui.BookScreen;

import java.util.ArrayList;
import java.util.List;

public final class LevelRpgJournalBookProvider implements BookPageProvider, BookTemplateDebugProvider {
    private static final String BOUNTY_PATH_NOT_READABLE = "The Bookmark cannot yet read the end of this path.";
    private static final LevelRpgJournalIntroFlowState INTRO_FLOW = LevelRpgJournalIntroFlowState.get();
    private static final LevelRpgJournalActionPolicy ACTION_POLICY = new LevelRpgJournalActionPolicy();
    // Phase 2: FRONT_SPECIAL is no longer counted as a normal interior spread.
    // Keep legacy constants removed to avoid accidental spread-offset reintroduction.

    @Override
    public int spreadCount(BookContext context) {
        if (isBountyDocument(context)) {
            return bountyDocumentSpreadCount(context);
        }
        return readingSpreadCount(context);
    }

    @Override
    public BookSpread getSpread(BookContext context, int spreadIndex) {
        if (isBountyDocument(context)) {
            return bountySpreadFor(context, spreadIndex);
        }
        List<BookSpread> spreads = document(context).spreads();
        return spreadIndex >= 0 && spreadIndex < spreads.size()
                ? spreads.get(spreadIndex)
                : BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    @Override
    public List<BookInteractiveRegion> interactiveRegions(BookContext context, int spreadIndex) {
        if (isBountyDocument(context)) {
            return bountyInteractiveRegions(context, spreadIndex);
        }
        return document(context).interactiveRegionsBySpread().getOrDefault(spreadIndex, List.of());
    }

    @Override
    public List<BookTrackedRegion> trackedInteractiveRegions(BookContext context, int spreadIndex) {
        if (isBountyDocument(context)) {
            return List.of();
        }
        return List.of();
    }

    @Override
    public boolean hasSpecialSection(BookContext context, BookSection section) {
        if (isBountyDocument(context)) {
            return false;
        }
        return section == BookSection.FRONT_SPECIAL || section == BookSection.BACK_SPECIAL;
    }

    @Override
    public BookSpread getSpecialSectionSpread(BookContext context, BookSection section) {
        if (isBountyDocument(context)) {
            return BookSpread.of(BookPage.empty(), BookPage.empty());
        }
        if (section == BookSection.FRONT_SPECIAL) {
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
        if (section == BookSection.BACK_SPECIAL) {
            return BookSpread.of(
                    BookPage.empty(),
                    BookPage.of(
                            BookTextBlock.title(Component.literal("Torn From The Binding")),
                            BookTextBlock.body(Component.literal("The final page is absent.")),
                            BookTextBlock.body(Component.literal("What the Enchiridion cannot hold has found a place in the world.")),
                            BookTextBlock.body(Component.literal("The Index remembers what was torn away."))
                    )
            );
        }
        return BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    @Override
    public List<BookInteractiveRegion> specialSectionInteractiveRegions(BookContext context, BookSection section) {
        if (isBountyDocument(context)) {
            return List.of();
        }
        if (section != BookSection.FRONT_SPECIAL && section != BookSection.BACK_SPECIAL) {
            return List.of();
        }
        return List.of();
    }

    @Override
    public List<BookTrackedRegion> specialSectionTrackedInteractiveRegions(BookContext context, BookSection section) {
        if (isBountyDocument(context)) {
            return List.of();
        }
        if (section != BookSection.FRONT_SPECIAL) {
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
        return interiorSpread;
    }

    @Override
    public int pageIndexForProjectionFocus(BookContext context, String focusId) {
        int interiorPage = document(context).projectionPageByFocus().getOrDefault(focusId, -1);
        return interiorPage;
    }

    @Override
    public String templatePurposeForPageIndex(BookContext context, int pageIndex) {
        return document(context).pagePurposeByPage().get(pageIndex);
    }

    @Override
    public String templatePageIdForPageIndex(BookContext context, int pageIndex) {
        return document(context).pageIdByPage().get(pageIndex);
    }

    @Override
    public int initialSpreadIndex(BookContext context) {
        INTRO_FLOW.syncToTruth(context);
        int preferred = firstReadableSpreadIndex();
        int max = Math.max(0, readingSpreadCount(context) - 1);
        return Math.clamp(preferred, 0, max);
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
        return false;
    }

    @Override
    public boolean isBackSpread(BookContext context, int spreadIndex) {
        // Compatibility-only during section migration. BACK_SPECIAL is section-state driven.
        return false;
    }

    private static LevelRpgJournalDocument document(BookContext context) {
        return LevelRpgJournalDocumentBuilder.build(context);
    }

    private static int readingSpreadCount(BookContext context) {
        return document(context).spreads().size();
    }

    private static boolean isBountyDocument(BookContext context) {
        return context.contentSession().mode() == BookContentMode.BOUNTY;
    }

    private static int bountyDocumentSpreadCount(BookContext context) {
        if (LevelRpgJournalInteractionBridge.activeSoloBountyId(context) != null) {
            return 1;
        }
        int tier = LevelRpgJournalInteractionBridge.bountyOfferTier(context);
        return JournalBountyCatalog.spreadCountForTier(tier);
    }

    private static int clampBountySpreadIndex(BookContext context, int spreadIndex) {
        int max = Math.max(0, bountyDocumentSpreadCount(context) - 1);
        return Math.clamp(spreadIndex, 0, max);
    }

    private static BookSpread bountySpreadFor(BookContext context, int spreadIndex) {
        var activeBountyId = LevelRpgJournalInteractionBridge.activeSoloBountyId(context);
        if (activeBountyId != null) {
            JournalBountyOffer active = resolveOffer(activeBountyId);
            boolean objectiveMet = LevelRpgJournalInteractionBridge.isActiveSoloBountyObjectiveMet(context);
            boolean placeholderActive = active != null && !active.objectiveImplemented();
            BookPage activePage = placeholderActive
                    ? BookPage.of(
                            BookTextBlock.title(Component.literal("Active Solo Bounty")),
                            BookTextBlock.subtitle(Component.literal(active.title())),
                            BookTextBlock.body(Component.literal(active.summary())),
                            BookTextBlock.section(Component.literal("Objective")),
                            BookTextBlock.body(Component.literal(active.objective())),
                            BookTextBlock.body(Component.literal(progressLine(context, active, objectiveMet))),
                            BookTextBlock.body(Component.literal(BOUNTY_PATH_NOT_READABLE)),
                            BookTextBlock.body(Component.literal("Reward: Essence +" + active.rewardEssence())),
                            BookTextBlock.body(Component.literal(LevelRpgJournalInteractionBridge.hasCompletedBounty(context, active.id())
                                    ? "First completion bonus claimed."
                                    : "First completion bonus available."))
                    )
                    : BookPage.of(
                            BookTextBlock.title(Component.literal("Active Solo Bounty")),
                            BookTextBlock.subtitle(Component.literal(active != null ? active.title() : activeBountyId.toString())),
                            BookTextBlock.body(Component.literal(active != null
                                    ? active.summary()
                                    : "The Bookmark remembers a path that cannot be read.")),
                            BookTextBlock.section(Component.literal("Objective")),
                            BookTextBlock.body(Component.literal(active != null ? active.objective() : "Definition missing.")),
                            BookTextBlock.body(Component.literal(progressLine(context, active, objectiveMet))),
                            BookTextBlock.body(Component.literal("Reward: Essence +" + (active != null ? active.rewardEssence() : 1))),
                            BookTextBlock.body(Component.literal(active != null && LevelRpgJournalInteractionBridge.hasCompletedBounty(context, active.id())
                                    ? "First completion bonus claimed."
                                    : "First completion bonus available."))
                    );
            return BookSpread.of(
                    activePage,
                    BookPage.of(
                            BookTextBlock.title(Component.literal("Bookmark")),
                            BookTextBlock.body(Component.literal("Tap the Bookmark to return."))
                    )
            );
        }

        int tier = LevelRpgJournalInteractionBridge.bountyOfferTier(context);
        int idx = clampBountySpreadIndex(context, spreadIndex);
        JournalBountyOfferSnapshot snapshot = JournalBountyCatalog.offerSnapshot(tier, idx);
        return BookSpread.of(
                bountyOfferPage(context, snapshot.leftOffer()),
                bountyOfferPage(context, snapshot.rightOffer())
        );
    }

    private static BookPage bountyOfferPage(BookContext context, JournalBountyOffer offer) {
        boolean previouslyCompleted = LevelRpgJournalInteractionBridge.hasCompletedBounty(context, offer.id());
        if (!offer.objectiveImplemented()) {
            return BookPage.of(
                    BookTextBlock.title(Component.literal(offer.title())),
                    BookTextBlock.subtitle(Component.literal(offer.summary())),
                    BookTextBlock.section(Component.literal("Objective")),
                    BookTextBlock.body(Component.literal(offer.objective())),
                    BookTextBlock.body(Component.literal(BOUNTY_PATH_NOT_READABLE)),
                    BookTextBlock.section(Component.literal("Reward")),
                    BookTextBlock.body(Component.literal("Essence +" + offer.rewardEssence())),
                    BookTextBlock.body(Component.literal(previouslyCompleted
                            ? "First completion bonus claimed."
                            : "First completion bonus: +" + offer.firstCompletionBonusEssence() + " Essence"))
            );
        }
        return BookPage.of(
                BookTextBlock.title(Component.literal(offer.title())),
                BookTextBlock.subtitle(Component.literal(offer.summary())),
                BookTextBlock.section(Component.literal("Objective")),
                BookTextBlock.body(Component.literal(offer.objective())),
                BookTextBlock.section(Component.literal("Reward")),
                BookTextBlock.body(Component.literal("Essence +" + offer.rewardEssence())),
                BookTextBlock.body(Component.literal(previouslyCompleted
                        ? "First completion bonus claimed."
                        : "First completion bonus: +" + offer.firstCompletionBonusEssence() + " Essence"))
        );
    }

    private static List<BookInteractiveRegion> bountyInteractiveRegions(BookContext context, int spreadIndex) {
        var activeBountyId = LevelRpgJournalInteractionBridge.activeSoloBountyId(context);
        if (activeBountyId != null) {
            if (spreadIndex != 0) {
                return List.of();
            }
            return List.of(
                    abandonRegion(context),
                    returnRegion(context)
            );
        }
        int idx = clampBountySpreadIndex(context, spreadIndex);
        if (idx != spreadIndex) {
            return List.of();
        }
        int tier = LevelRpgJournalInteractionBridge.bountyOfferTier(context);
        JournalBountyOfferSnapshot snapshot = JournalBountyCatalog.offerSnapshot(tier, idx);
        List<BookInteractiveRegion> regions = new ArrayList<>();
        if (snapshot.leftOffer().objectiveImplemented()) {
            regions.add(claimRegion(context, BookPageSide.LEFT, snapshot.leftOffer()));
        }
        if (snapshot.rightOffer().objectiveImplemented()) {
            regions.add(claimRegion(context, BookPageSide.RIGHT, snapshot.rightOffer()));
        }
        regions.add(returnRegion(context));
        return List.copyOf(regions);
    }

    private static BookInteractiveRegion claimRegion(BookContext context, BookPageSide pageSide, JournalBountyOffer offer) {
        return BookInteractiveRegion.of(
                pageSide,
                20,
                136,
                112,
                20,
                Component.literal("Claim " + offer.title()),
                Component.literal("[ Claim ]"),
                (bookContext, spreadIndex, mouseButton) -> {
                    if (mouseButton != 0) {
                        return false;
                    }
                    return LevelRpgJournalInteractionBridge.requestClaimBountyOffer(bookContext, offer.id());
                }
        );
    }

    private static BookInteractiveRegion returnRegion(BookContext context) {
        return BookInteractiveRegion.of(
                BookPageSide.RIGHT,
                20,
                150,
                112,
                20,
                Component.literal("Return to journal"),
                Component.literal("[ Return ]"),
                (bookContext, spreadIndex, mouseButton) -> {
                    if (mouseButton != 0) {
                        return false;
                    }
                    if (bookContext != null
                            && bookContext.minecraft() != null
                            && bookContext.minecraft().screen instanceof BookScreen bookScreen) {
                        bookScreen.beginReturnFromBountyOffers();
                        return true;
                    }
                    bookContext.contentSession().setMode(BookContentMode.READING);
                    return true;
                }
        );
    }

    private static BookInteractiveRegion abandonRegion(BookContext context) {
        return BookInteractiveRegion.of(
                BookPageSide.LEFT,
                20,
                150,
                116,
                20,
                Component.literal("Abandon active bounty"),
                Component.literal("[ Abandon ]"),
                (bookContext, spreadIndex, mouseButton) -> {
                    if (mouseButton != 0) {
                        return false;
                    }
                    return LevelRpgJournalInteractionBridge.requestAbandonSoloBounty(bookContext);
                }
        );
    }

    private static JournalBountyOffer resolveOffer(ResourceLocation bountyId) {
        return JournalBountyCatalog.resolveOffer(bountyId);
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
        Component hoverText = frontCoverTooltipText(cardState);
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
        return INTRO_FLOW.selectedArchetypeId(context);
    }

    private static String focusIdForCardState(BookFrontCoverCardState cardState) {
        ResourceLocation displayId = cardState.displayedArchetypeId();
        return displayId == null ? null : displayId.toString();
    }

    private static Component frontCoverTooltipText(BookFrontCoverCardState cardState) {
        ResourceLocation displayedArchetypeId = cardState.displayedArchetypeId();
        if (displayedArchetypeId == null) {
            return Component.literal("Seat an archetype card in the waiting recess.");
        }
        String title = archetypeTitle(displayedArchetypeId);
        if (cardState.boundArchetypeId() != null) {
            return Component.literal("Inscribed archetype: " + title);
        }
        return Component.literal("Selected archetype: " + title);
    }

    private static String archetypeTitle(ResourceLocation archetypeId) {
        for (JournalArchetypeChoice choice : LevelRpgArchetypeBindingBridge.availableArchetypes()) {
            if (archetypeId.equals(choice.archetypeId())) {
                return choice.title();
            }
        }
        return archetypeId.toString();
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
        return 0;
    }

    private static String progressLine(BookContext context, JournalBountyOffer active, boolean objectiveMet) {
        if (active == null) {
            return objectiveMet ? "Progress: Objective marked complete." : "Progress: Incomplete.";
        }
        int progress = LevelRpgJournalInteractionBridge.activeSoloBountyProgress(context);
        return switch (active.objectiveType()) {
            case REACH_Y -> {
                int y = active.objectiveTargetY();
                yield objectiveMet
                        ? "Progress: Depth objective met."
                        : "Progress: Descend to Y " + y + " or below.";
            }
            case KILL_HOSTILE_MOB -> {
                int need = Math.max(1, active.objectiveCount());
                yield objectiveMet
                        ? "Progress: Objective met."
                        : "Progress: Hostiles slain " + progress + "/" + need + ".";
            }
            case MINE_ORE -> {
                int need = Math.max(1, active.objectiveCount());
                int maxY = active.objectiveTargetY();
                String depth = maxY > 0 ? " (Y≤" + maxY + ")" : "";
                yield objectiveMet
                        ? "Progress: Objective met."
                        : "Progress: Ore mined " + progress + "/" + need + depth + ".";
            }
            default -> objectiveMet ? "Progress: Objective marked complete." : "Progress: Incomplete.";
        };
    }
}
