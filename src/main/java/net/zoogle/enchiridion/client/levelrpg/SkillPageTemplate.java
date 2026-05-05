package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;

final class SkillPageTemplate {
    private SkillPageTemplate() {}

    static BookPage build(JournalSkillEntry skill, int pageIndex, JournalPageId pageId) {
        JournalContentStore content = JournalContentStore.instance();
        JournalContentStore.PageContentView pageContent = content.page(pageId, pageIndex, JournalPagePurpose.SKILL_DETAIL);
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.SKILL_DETAIL, net.zoogle.enchiridion.api.BookPageSide.LEFT);
        page.addInteraction(
                "skill-button:" + skill.name() + ":0",
                pageContent.text(JournalPageSlot.INTERACTION, "View Skill"),
                Component.literal("Project " + skill.name() + " above the book"),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.openSkillProjection(bookContext, skill.name())
        );
        page.addTitle(pageContent.text(JournalPageSlot.TITLE, skill.name()));
        // Invested level is live progression data — never read from JournalContentStore or template
        // overrides freeze stale "0" when the player gains levels.
        page.addFocal(String.valueOf(skill.investedSkillLevel()));
        page.addBody(pageContent.text(JournalPageSlot.BODY, compactSkillDescription(skill.roleSummary())));
        return page.build();
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
