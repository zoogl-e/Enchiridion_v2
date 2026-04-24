package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTrackedRegion;
import net.zoogle.enchiridion.api.BookTemplateDebugProvider;

import java.util.List;

public final class LevelRpgJournalBookProvider implements BookPageProvider, BookTemplateDebugProvider {
    private static final LevelRpgJournalIntroFlowState INTRO_FLOW = LevelRpgJournalIntroFlowState.get();

    @Override
    public int spreadCount(BookContext context) {
        return document(context).spreads().size();
    }

    @Override
    public BookSpread getSpread(BookContext context, int spreadIndex) {
        List<BookSpread> spreads = document(context).spreads();
        return spreadIndex >= 0 && spreadIndex < spreads.size()
                ? spreads.get(spreadIndex)
                : BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    @Override
    public List<BookInteractiveRegion> interactiveRegions(BookContext context, int spreadIndex) {
        return document(context).interactiveRegionsBySpread().getOrDefault(spreadIndex, List.of());
    }

    @Override
    public List<BookTrackedRegion> trackedInteractiveRegions(BookContext context, int spreadIndex) {
        if (!isFrontSpread(context, spreadIndex)) {
            return List.of();
        }
        BookTrackedRegion frontRegion = frontCoverTrackedRegion(context);
        return frontRegion == null ? List.of() : List.of(frontRegion);
    }

    @Override
    public BookProjectionView projection(BookContext context, String focusId) {
        if (focusId == null || focusId.isBlank()) {
            return null;
        }
        INTRO_FLOW.syncToTruth(context);
        BookProjectionView archetypeProjection = archetypeProjection(context, focusId);
        if (archetypeProjection != null) {
            return archetypeProjection;
        }
        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        for (JournalSkillEntry skill : snapshot.skills()) {
            if (!focusId.equals(skill.name())) {
                continue;
            }
            float progress = skill.masteryRequiredForNextLevel() <= 0L
                    ? 1.0f
                    : Math.clamp((float) skill.masteryProgress() / (float) skill.masteryRequiredForNextLevel(), 0.0f, 1.0f);
            return new BookProjectionView(
                    skill.name(),
                    Component.literal(skill.name()),
                    Component.literal(String.valueOf(skill.investedSkillLevel())),
                    Component.literal("Mastery " + skill.masteryLevel()),
                    Component.literal(compactSkillDescription(skill.roleSummary())),
                    Component.literal("Mastery " + skill.masteryProgressText()),
                    progress,
                    Component.literal(skill.canSpendSkillPoint()
                            ? "A Skill Point is ready for this discipline."
                            : "No Skill Point is ready for this discipline."),
                    skill.canSpendSkillPoint(),
                    skill.canSpendSkillPoint() ? Component.literal("Spend Point") : null,
                    skill.canSpendSkillPoint()
                            ? (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                            && LevelRpgJournalInteractionBridge.requestSpendSkillPoint(bookContext, skill.name())
                            : null
            );
        }
        return null;
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
        return document(context).projectionSpreadByFocus().getOrDefault(focusId, -1);
    }

    @Override
    public int pageIndexForProjectionFocus(BookContext context, String focusId) {
        return document(context).projectionPageByFocus().getOrDefault(focusId, -1);
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
        int preferred = 1;
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
        return spreadIndex == 0;
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
            LevelRpgJournalIntroFlowState flow = LevelRpgJournalIntroFlowState.get();
            flow.setSelectedFocus(choice.focusId());
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
                            ? (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                            && LevelRpgJournalInteractionBridge.beginArchetypeBinding(bookContext, choice.focusId())
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
        Component hoverText = cardState.visible()
                ? Component.literal("Recall the archetype bound into the cover.")
                : Component.literal("Seat an archetype card in the waiting recess.");
        return BookTrackedRegion.of(
                BookTrackedRegion.Anchor.FRONT_COVER_CARD,
                hoverText,
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && handleFrontCoverCardInteraction(bookContext, cardState, focusId)
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

    private static boolean handleFrontCoverCardInteraction(BookContext context, BookFrontCoverCardState cardState, String focusId) {
        if (cardState.boundArchetypeId() != null && focusId != null && !focusId.isBlank()) {
            return LevelRpgJournalInteractionBridge.openArchetypeProjection(context, focusId);
        }
        return LevelRpgJournalInteractionBridge.beginArchetypeSelection(context);
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

    private static String compactSkillDescription(String description) {
        if (description == null || description.isBlank()) {
            return "No field note is inscribed for this discipline.";
        }
        String trimmed = description.trim();
        for (int index = 0; index < trimmed.length(); index++) {
            char c = trimmed.charAt(index);
            if (c == '.' || c == '!' || c == '?') {
                return trimmed.substring(0, index + 1).trim();
            }
        }
        return trimmed;
    }
}
