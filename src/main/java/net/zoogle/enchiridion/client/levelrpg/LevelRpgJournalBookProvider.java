package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;

import java.util.List;

public final class LevelRpgJournalBookProvider implements BookPageProvider {
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
    public BookProjectionView projection(BookContext context, String focusId) {
        if (focusId == null || focusId.isBlank()) {
            return null;
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

    private static LevelRpgJournalDocument document(BookContext context) {
        return LevelRpgJournalDocumentBuilder.build(context);
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
