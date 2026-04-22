package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;

final class SkillPageTemplate {
    private SkillPageTemplate() {}

    static BookPage build(JournalSkillEntry skill, int pageIndex) {
        JournalContentStore content = JournalContentStore.instance();
        JournalPageStyleSystem.StyledPageBuilder page = JournalPageStyleSystem.builder(JournalPagePurpose.SKILL_DETAIL, net.zoogle.enchiridion.api.BookPageSide.LEFT);
        page.addBottomInteraction(
                "skill-button:" + skill.name() + ":0",
                content.text(pageIndex, JournalPageSlot.INTERACTION, "View Skill"),
                Component.literal("Open " + skill.name()),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.openSkillProjection(bookContext, skill.name())
        );
        page.addCenteredText(JournalPageSlot.TITLE, JournalTextRole.TITLE, content.text(pageIndex, JournalPageSlot.TITLE, skill.name()));
        page.addCenteredText(JournalPageSlot.FOCAL, JournalTextRole.FOCAL, content.text(pageIndex, JournalPageSlot.FOCAL, String.valueOf(skill.investedSkillLevel())));
        page.addCenteredBody(JournalPageSlot.BODY, content.text(pageIndex, JournalPageSlot.BODY, compactSkillDescription(skill.roleSummary())));
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
