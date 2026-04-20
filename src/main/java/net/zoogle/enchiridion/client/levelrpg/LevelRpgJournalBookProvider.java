package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;
import net.zoogle.enchiridion.client.ui.BookLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class LevelRpgJournalBookProvider implements BookPageProvider {
    @Override
    public int spreadCount(BookContext context) {
        return buildSpreads(LevelRpgJournalSnapshotFactory.create(context)).size();
    }

    @Override
    public BookSpread getSpread(BookContext context, int spreadIndex) {
        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        List<BookSpread> spreads = buildSpreads(snapshot);
        return spreadIndex >= 0 && spreadIndex < spreads.size()
                ? spreads.get(spreadIndex)
                : BookSpread.of(BookPage.empty(), BookPage.empty());
    }

    private static String formatSkillList(LevelRpgJournalSnapshot snapshot) {
        return snapshot.skills().stream()
                .map(entry -> entry.name() + "\n" + entry.description())
                .collect(Collectors.joining("\n\n"));
    }

    private static String formatMilestones(LevelRpgJournalSnapshot snapshot) {
        return snapshot.focusedSkillMilestones().stream()
                .map(entry -> entry.title() + "\n" + entry.description())
                .collect(Collectors.joining("\n\n"));
    }

    private static List<BookSpread> buildSpreads(LevelRpgJournalSnapshot snapshot) {
        List<BookPage> pages = new ArrayList<>();
        pages.addAll(paginateSection(snapshot.journalTitle(), snapshot.introText()));
        pages.addAll(paginateSection(snapshot.overviewTitle(), snapshot.overviewText()));
        pages.addAll(paginateSection(snapshot.skillsTitle(), formatSkillList(snapshot)));
        pages.addAll(paginateSection(snapshot.currentFocusTitle(), snapshot.currentFocusText()));
        pages.addAll(paginateSection(snapshot.focusedSkill().name(), snapshot.focusedSkill().description()));
        pages.addAll(paginateSection(snapshot.milestonesTitle(), formatMilestones(snapshot)));

        List<BookSpread> spreads = new ArrayList<>();
        for (int i = 0; i < pages.size(); i += 2) {
            BookPage left = pages.get(i);
            BookPage right = i + 1 < pages.size() ? pages.get(i + 1) : BookPage.empty();
            spreads.add(BookSpread.of(left, right));
        }
        return List.copyOf(spreads);
    }

    private static List<BookPage> paginateSection(String title, String body) {
        int bodyLinesPerPage = Math.max(1, bodyLinesPerPage(title));
        List<String> wrappedLines = wrapPlainText(body, contentWidth());
        if (wrappedLines.isEmpty()) {
            return List.of(pageFor(title, body));
        }

        List<BookPage> pages = new ArrayList<>();
        for (int i = 0; i < wrappedLines.size(); i += bodyLinesPerPage) {
            int end = Math.min(i + bodyLinesPerPage, wrappedLines.size());
            String pageTitle = title;
            String pageBody = String.join("\n", wrappedLines.subList(i, end));
            pages.add(pageFor(pageTitle, pageBody));
        }
        return List.copyOf(pages);
    }

    private static BookPage pageFor(String title, String body) {
        return BookPage.of(
                BookTextBlock.title(Component.literal(title)),
                BookTextBlock.body(Component.literal(body))
        );
    }

    private static int bodyLinesPerPage(String title) {
        Font font = Minecraft.getInstance().font;
        int contentHeight = BookLayout.fromScreen(
                Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight()
        ).pageHeight() - (PageCanvasRenderer.PAGE_MARGIN_Y * 2);
        int titleLines = Math.max(1, wrapPlainText(title, contentWidth()).size());
        int reservedHeight = titleLines * titleLineHeight() + PageCanvasRenderer.BLOCK_SPACING;
        return Math.max(1, (contentHeight - reservedHeight) / bodyLineHeight());
    }

    private static int contentWidth() {
        return Math.max(
                1,
                BookLayout.fromScreen(
                        Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                        Minecraft.getInstance().getWindow().getGuiScaledHeight()
                ).pageWidth() - (PageCanvasRenderer.PAGE_MARGIN_X * 2) - PageCanvasRenderer.RIGHT_PAGE_INSET
        );
    }

    private static int titleLineHeight() {
        return PageCanvasRenderer.TITLE_LINE_HEIGHT + PageCanvasRenderer.LINE_SPACING;
    }

    private static int bodyLineHeight() {
        return PageCanvasRenderer.BODY_LINE_HEIGHT + PageCanvasRenderer.LINE_SPACING;
    }

    private static List<String> wrapPlainText(String text, int width) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        String[] paragraphs = text.split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            font.split(Component.literal(paragraph), width).stream()
                    .map(LevelRpgJournalBookProvider::formattedLineToString)
                    .forEach(lines::add);
        }
        return lines;
    }

    private static String formattedLineToString(net.minecraft.util.FormattedCharSequence line) {
        if (line == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }
}
