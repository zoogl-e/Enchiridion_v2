package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;

import java.util.ArrayList;
import java.util.List;

final class SkillPageTemplate {
    private static final int CONTENT_X = PageCanvasRenderer.PAGE_MARGIN_X;
    private static final int CONTENT_Y = PageCanvasRenderer.PAGE_MARGIN_Y;
    private static final int CONTENT_W = JournalLayoutMetrics.PAGE_CONTENT_WIDTH;
    private static final int CONTENT_H = JournalLayoutMetrics.PAGE_CONTENT_HEIGHT;

    private static final int TITLE_Y = CONTENT_Y;
    private static final int LEVEL_Y = TITLE_Y + 24;
    private static final int DESCRIPTION_BODY_Y = LEVEL_Y + 32;
    private static final int BUTTON_Y = CONTENT_Y + CONTENT_H - JournalLayoutMetrics.SKILL_BUTTON_HEIGHT - 6;
    private static final int DESCRIPTION_INSET_X = 8;

    private SkillPageTemplate() {}

    static BookPage build(JournalSkillEntry skill) {
        List<BookPageElement> elements = new ArrayList<>();

        elements.add(JournalElementFactory.toInteractiveTextElement(JournalElementFactory.pageTextLayout(
                "skill-title:" + skill.name() + ":0",
                BookTextBlock.Kind.TITLE,
                skill.name(),
                CONTENT_X,
                TITLE_Y,
                CONTENT_W,
                Component.literal("Open " + skill.name()),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.openSkillProjection(bookContext, skill.name())
        )));
        elements.add(JournalElementFactory.centeredTextElement(BookTextBlock.Kind.LEVEL, String.valueOf(skill.investedSkillLevel()), CONTENT_X, LEVEL_Y, CONTENT_W));

        appendBodyLines(
                elements,
                clampLines(compactSkillDescription(skill.roleSummary()), CONTENT_W - (DESCRIPTION_INSET_X * 2), 3),
                DESCRIPTION_BODY_Y
        );

        int buttonX = CONTENT_X + Math.max(0, (CONTENT_W - JournalLayoutMetrics.SKILL_BUTTON_WIDTH) / 2);
        elements.add(JournalElementFactory.buttonElement(
                "skill-button:" + skill.name() + ":0",
                "View Skill",
                buttonX,
                BUTTON_Y,
                JournalLayoutMetrics.SKILL_BUTTON_WIDTH,
                JournalLayoutMetrics.SKILL_BUTTON_HEIGHT,
                Component.literal("Project " + skill.name()),
                (bookContext, spreadIndex, mouseButton) -> mouseButton == 0
                        && LevelRpgJournalInteractionBridge.openSkillProjection(bookContext, skill.name())
        ));

        return BookPage.of(List.of(), elements);
    }

    private static void appendBodyLines(List<BookPageElement> elements, List<String> lines, int startY) {
        int y = startY;
        for (String line : lines) {
            if (!line.isBlank()) {
                elements.add(JournalElementFactory.centeredTextElement(
                        BookTextBlock.Kind.BODY,
                        line,
                        CONTENT_X + DESCRIPTION_INSET_X,
                        y,
                        CONTENT_W - (DESCRIPTION_INSET_X * 2)
                ));
            }
            y += JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY);
        }
    }

    private static List<String> clampLines(String text, int width, int maxLines) {
        List<JournalPaginationEngine.MeasuredLine> wrapped = JournalPaginationEngine.wrapBodyText(text, width);
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < wrapped.size() && lines.size() < maxLines; index++) {
            lines.add(wrapped.get(index).text());
        }
        if (wrapped.size() > maxLines && !lines.isEmpty()) {
            int lastIndex = lines.size() - 1;
            lines.set(lastIndex, withEllipsis(lines.get(lastIndex), width));
        }
        return lines;
    }

    private static String withEllipsis(String line, int width) {
        String base = line == null ? "" : line.trim();
        if (base.isEmpty()) {
            return "...";
        }
        String candidate = base + "...";
        while (!base.isEmpty() && net.minecraft.client.Minecraft.getInstance().font.width(candidate) > width) {
            base = base.substring(0, base.length() - 1).trim();
            candidate = base + "...";
        }
        return candidate;
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
