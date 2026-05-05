package net.zoogle.enchiridion.client.levelrpg.journal.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookTemplateDebugProvider;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.ui.BookLayout;
import net.zoogle.enchiridion.client.ui.BookDebugSettings;
import net.zoogle.enchiridion.client.ui.BookScreenController;
import net.zoogle.enchiridion.client.ui.BookViewState;
import net.zoogle.enchiridion.client.levelrpg.journal.JournalContentStore;
import net.zoogle.enchiridion.client.levelrpg.journal.JournalPageId;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPagePurpose;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPageSlot;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPageStyleSystem;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalTemplateStore;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class LevelRpgJournalTemplateEditor {
    private static final int TOGGLE_KEY = GLFW.GLFW_KEY_F6;
    private static final int HANDLE_THRESHOLD = 4;
    private static final int MIN_SLOT_SIZE = 8;
    private static final int CONTROL_HEIGHT = 18;
    private static final int CONTROL_PADDING_X = 8;
    private static final int CONTROL_SPACING = 6;

    private boolean enabled;
    private boolean templateMode = true;
    private boolean debugVisible = true;
    private boolean templateDirty;
    private ActiveDrag activeDrag;
    private Selection selection;
    private EditingSession editing;
    private JournalTemplateStore savedTemplateSnapshot = JournalPageStyleSystem.snapshotTemplates();

    public boolean keyPressed(int keyCode, int modifiers, BookScreenController controller, BookViewState viewState, Runnable onChanged) {
        if (keyCode == TOGGLE_KEY) {
            enabled = !enabled;
            if (!enabled) {
                activeDrag = null;
                selection = null;
                editing = null;
            } else {
                debugVisible = BookDebugSettings.anyDebugEnabled();
            }
            return true;
        }
        if (!enabled) {
            return false;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_S) {
            saveAll(onChangedRunnable(controller, viewState));
            return true;
        }
        if (editing != null) {
            return handleEditingKey(keyCode, modifiers, onChanged);
        }
        if (keyCode == GLFW.GLFW_KEY_E && selection != null) {
            startEditing(controller, viewState);
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, BookScreenController controller, BookViewState viewState, Runnable onChanged) {
        if (!enabled || editing == null || Character.isISOControl(codePoint)) {
            return false;
        }
        editing.buffer().append(codePoint);
        persistEditing(onChanged);
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean mouseClicked(
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            double mouseX,
            double mouseY,
            int button
    ) {
        if (!enabled || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        ControlButton control = controlHit(Minecraft.getInstance().font, mouseX, mouseY);
        if (control != null) {
            return handleControlClick(control, onChangedRunnable(controller, viewState));
        }
        Hit hit = resolveHit(controller, viewState, sceneRenderer, mouseX, mouseY);
        if (hit == null) {
            selection = null;
            activeDrag = null;
            editing = null;
            return false;
        }
        selection = new Selection(hit.purpose(), hit.pageSide(), hit.slot());
        if (templateMode) {
            activeDrag = new ActiveDrag(hit.purpose(), hit.pageSide(), hit.slot(), hit.region(), hit.handle(), hit.localX(), hit.localY());
        }
        return true;
    }

    public boolean mouseDragged(
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            double mouseX,
            double mouseY
    ) {
        if (!enabled || !templateMode || activeDrag == null) {
            return false;
        }
        BookSceneRenderer.PageLocalPoint point = localPoint(sceneRenderer, controller, viewState, activeDrag.pageSide(), mouseX, mouseY);
        if (point == null) {
            return true;
        }
        JournalPageStyleSystem.SlotRegion updated = resized(
                activeDrag.startRegion(),
                activeDrag.handle(),
                (int) Math.round(point.localX() - activeDrag.startLocalX()),
                (int) Math.round(point.localY() - activeDrag.startLocalY())
        );
        JournalPageStyleSystem.updateTemplateRegion(activeDrag.purpose(), activeDrag.slot(), activeDrag.pageSide(), updated);
        templateDirty = true;
        selection = new Selection(activeDrag.purpose(), activeDrag.pageSide(), activeDrag.slot());
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!enabled || activeDrag == null || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        activeDrag = null;
        return true;
    }

    public void renderOverlay(
            GuiGraphics graphics,
            Font font,
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            int screenHeight
    ) {
        if (!enabled || viewState.layout() == null) {
            return;
        }
        if (templateMode && BookDebugSettings.templateRegionsDebug()) {
            drawForSide(graphics, font, controller, viewState, sceneRenderer, BookPageSide.LEFT);
            drawForSide(graphics, font, controller, viewState, sceneRenderer, BookPageSide.RIGHT);
        }
        drawControlStrip(graphics, font, screenHeight);
        if (selection != null && BookDebugSettings.debugLabels()) {
            JournalPageStyleSystem.TemplateSpec template = JournalPageStyleSystem.currentTemplate(selection.purpose(), selection.pageSide());
            JournalPageStyleSystem.SlotRegion region = template.slot(selection.slot()).region();
            graphics.drawString(font, selection.purpose() + " / " + selection.slot() + " [" + selection.pageSide() + "]", 8, screenHeight - 52, 0xFFE8F7FF, false);
            graphics.drawString(font, "bounds=" + region.x() + "," + region.y() + " " + region.width() + "x" + region.height(), 8, screenHeight - 42, 0xFFFFFFFF, false);
        }
        if (editing != null) {
            graphics.fill(6, 6, 246, 76, 0xD0201A16);
            graphics.drawString(font, "Editing " + editing.slot() + " | Enter commit | Ctrl+S save", 12, 12, 0xFFFFE9AF, false);
            int y = 24;
            for (String line : editing.buffer().toString().split("\\R", -1)) {
                graphics.drawString(font, line.isEmpty() ? " " : line, 12, y, 0xFFFFFFFF, false);
                y += 10;
                if (y > 66) {
                    break;
                }
            }
        }
    }

    private boolean handleEditingKey(int keyCode, int modifiers, Runnable onChanged) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                editing = null;
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!editing.buffer().isEmpty()) {
                    editing.buffer().deleteCharAt(editing.buffer().length() - 1);
                    persistEditing(onChanged);
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (isMultiline(editing.slot()) && (modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
                    editing.buffer().append('\n');
                    persistEditing(onChanged);
                } else {
                    persistEditing(onChanged);
                    editing = null;
                }
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    private void drawForSide(
            GuiGraphics graphics,
            Font font,
            BookScreenController controller,
            BookViewState viewState,
            BookSceneRenderer sceneRenderer,
            BookPageSide pageSide
    ) {
        JournalPagePurpose purpose = purposeFor(controller.definition().provider(), controller.context(), pageIndex(viewState, pageSide));
        if (purpose == null) {
            return;
        }
        JournalPageStyleSystem.TemplateSpec template = JournalPageStyleSystem.currentTemplate(purpose, pageSide);
        for (Map.Entry<JournalPageSlot, JournalPageStyleSystem.SlotDefinition> entry : template.slots().entrySet()) {
            JournalPageStyleSystem.SlotRegion region = entry.getValue().region();
            BookSceneRenderer.ScreenQuad quad = sceneRenderer.projectPageQuad(
                    viewState.layout(),
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    viewState.currentProjectionFocusOffset(),
                    pageSide,
                    region.x(),
                    region.y(),
                    region.width(),
                    region.height()
            );
            int color = isSelected(purpose, pageSide, entry.getKey()) ? 0xFFFFD54D : 0xAA47C7FF;
            drawQuad(graphics, quad, color);
            BookSceneRenderer.ScreenRect screenRect = sceneRenderer.projectPageRect(
                    viewState.layout(),
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    viewState.currentProjectionFocusOffset(),
                    pageSide,
                    region.x(),
                    region.y(),
                    region.width(),
                    region.height()
            );
            drawHandle(graphics, screenRect, color);
            graphics.drawString(font, entry.getKey().name(), Math.round(screenRect.x()), Math.round(screenRect.y()) - 8, color, false);
        }
    }

    private Hit resolveHit(BookScreenController controller, BookViewState viewState, BookSceneRenderer sceneRenderer, double mouseX, double mouseY) {
        for (BookPageSide pageSide : BookPageSide.values()) {
            BookSceneRenderer.PageLocalPoint point = localPoint(sceneRenderer, controller, viewState, pageSide, mouseX, mouseY);
            if (point == null) {
                continue;
            }
            JournalPagePurpose purpose = purposeFor(controller.definition().provider(), controller.context(), pageIndex(viewState, pageSide));
            if (purpose == null) {
                continue;
            }
            JournalPageStyleSystem.TemplateSpec template = JournalPageStyleSystem.currentTemplate(purpose, pageSide);
            for (Map.Entry<JournalPageSlot, JournalPageStyleSystem.SlotDefinition> entry : template.slots().entrySet()) {
                JournalPageStyleSystem.SlotRegion region = entry.getValue().region();
                if (contains(region, point.localX(), point.localY())) {
                    return new Hit(purpose, pageSide, entry.getKey(), region, handleFor(region, point.localX(), point.localY()), point.localX(), point.localY());
                }
            }
        }
        return null;
    }

    private BookSceneRenderer.PageLocalPoint localPoint(
            BookSceneRenderer sceneRenderer,
            BookScreenController controller,
            BookViewState viewState,
            BookPageSide pageSide,
            double mouseX,
            double mouseY
    ) {
        BookLayout layout = viewState.layout();
        return layout == null ? null : sceneRenderer.pageLocalPoint(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                viewState.currentProjectionFocusOffset(),
                pageSide,
                (int) mouseX,
                (int) mouseY
        );
    }

    private void startEditing(BookScreenController controller, BookViewState viewState) {
        if (selection == null) {
            return;
        }
        int pageIndex = pageIndex(viewState, selection.pageSide());
        JournalPageId pageId = pageIdFor(controller, viewState, selection.pageSide());
        String currentText = JournalContentStore.instance()
                .page(pageId, pageIndex, selection.purpose())
                .text(selection.slot(), captureCurrentSlotContent(viewState, selection));
        editing = new EditingSession(selection.purpose(), selection.pageSide(), selection.slot(), pageId, pageIndex, new StringBuilder(currentText));
    }

    private void persistEditing(Runnable onChanged) {
        if (editing == null) {
            return;
        }
        JournalContentStore.instance().putText(editing.pageId(), editing.pageIndex(), editing.purpose(), editing.slot(), editing.buffer().toString());
        onChanged.run();
    }

    private boolean handleControlClick(ControlButton control, Runnable onChanged) {
        switch (control) {
            case TEMPLATE_MODE -> {
                templateMode = !templateMode;
                activeDrag = null;
                return true;
            }
            case SAVE -> {
                saveAll(onChanged);
                return true;
            }
            case DISCARD -> {
                if (templateDirty) {
                    JournalPageStyleSystem.restoreTemplates(savedTemplateSnapshot);
                    templateDirty = false;
                    activeDrag = null;
                    editing = null;
                    onChanged.run();
                }
                return true;
            }
            case DEBUG_UI -> {
                debugVisible = !debugVisible;
                BookDebugSettings.setAllDebugEnabled(debugVisible);
                onChanged.run();
                return true;
            }
            case DEBUG_TEMPLATE -> {
                BookDebugSettings.setTemplateRegionsDebug(!BookDebugSettings.templateRegionsDebug());
                debugVisible = BookDebugSettings.anyDebugEnabled();
                onChanged.run();
                return true;
            }
            case DEBUG_MEASURED -> {
                BookDebugSettings.setMeasuredTextBoundsDebug(!BookDebugSettings.measuredTextBoundsDebug());
                debugVisible = BookDebugSettings.anyDebugEnabled();
                onChanged.run();
                return true;
            }
            case DEBUG_INTERACTION -> {
                BookDebugSettings.setInteractionBoundsDebug(!BookDebugSettings.interactionBoundsDebug());
                debugVisible = BookDebugSettings.anyDebugEnabled();
                onChanged.run();
                return true;
            }
            case DEBUG_LABELS -> {
                BookDebugSettings.setDebugLabels(!BookDebugSettings.debugLabels());
                debugVisible = BookDebugSettings.anyDebugEnabled();
                onChanged.run();
                return true;
            }
        }
        return false;
    }

    private void saveAll(Runnable onChanged) {
        JournalPageStyleSystem.saveTemplates();
        JournalContentStore.instance().save();
        savedTemplateSnapshot = JournalPageStyleSystem.snapshotTemplates();
        templateDirty = false;
        onChanged.run();
    }

    private Runnable onChangedRunnable(BookScreenController controller, BookViewState viewState) {
        return () -> {
            controller.refreshCurrentSpread();
            viewState.refreshDisplayedSpread(controller);
        };
    }

    private String captureCurrentSlotContent(BookViewState viewState, Selection selection) {
        if (viewState.displayedSpread() == null) {
            return "";
        }
        BookPage page = selection.pageSide() == BookPageSide.LEFT
                ? viewState.displayedSpread().left()
                : viewState.displayedSpread().right();
        JournalPageStyleSystem.TemplateSpec template = JournalPageStyleSystem.currentTemplate(selection.purpose(), selection.pageSide());
        JournalPageStyleSystem.SlotRegion region = template.slot(selection.slot()).region();
        return switch (selection.slot()) {
            case TITLE, FOCAL, SUBTITLE, BODY, FOOTER, STATS -> String.join("\n", textLinesInRegion(page, region));
            case INTERACTION -> firstInteractiveLabelInRegion(page, region);
            case ROWS -> ledgerRowsInRegion(page, region);
            case RADAR -> "";
        };
    }

    private void drawControlStrip(GuiGraphics graphics, Font font, int screenHeight) {
        int y = screenHeight - CONTROL_HEIGHT - 6;
        int x = 8;
        for (ControlButton button : ControlButton.values()) {
            if (button == ControlButton.DISCARD && !templateDirty) {
                continue;
            }
            if (isDebugSubToggle(button) && !debugVisible) {
                continue;
            }
            String label = buttonLabel(button);
            int width = Math.max(46, font.width(label) + (CONTROL_PADDING_X * 2));
            int fill = buttonFill(button);
            int border = buttonBorder(button);
            graphics.fill(x, y, x + width, y + CONTROL_HEIGHT, fill);
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y + CONTROL_HEIGHT - 1, x + width, y + CONTROL_HEIGHT, border);
            graphics.fill(x, y, x + 1, y + CONTROL_HEIGHT, border);
            graphics.fill(x + width - 1, y, x + width, y + CONTROL_HEIGHT, border);
            graphics.drawString(font, label, x + CONTROL_PADDING_X, y + 5, buttonTextColor(button), false);
            x += width + CONTROL_SPACING;
        }
    }

    private ControlButton controlHit(Font font, double mouseX, double mouseY) {
        int y = Minecraft.getInstance().getWindow().getGuiScaledHeight() - CONTROL_HEIGHT - 6;
        int x = 8;
        for (ControlButton button : ControlButton.values()) {
            if (button == ControlButton.DISCARD && !templateDirty) {
                continue;
            }
            if (isDebugSubToggle(button) && !debugVisible) {
                continue;
            }
            String label = buttonLabel(button);
            int width = Math.max(46, font.width(label) + (CONTROL_PADDING_X * 2));
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CONTROL_HEIGHT) {
                return button;
            }
            x += width + CONTROL_SPACING;
        }
        return null;
    }

    private String buttonLabel(ControlButton button) {
        return switch (button) {
            case TEMPLATE_MODE -> templateMode ? "Template Mode: ON" : "Template Mode";
            case SAVE -> "Save";
            case DISCARD -> "Discard Changes";
            case DEBUG_UI -> debugVisible ? "Debug UI: ON" : "Debug UI";
            case DEBUG_TEMPLATE -> BookDebugSettings.templateRegionsDebug() ? "Slots: ON" : "Slots";
            case DEBUG_MEASURED -> BookDebugSettings.measuredTextBoundsDebug() ? "Text Bounds: ON" : "Text Bounds";
            case DEBUG_INTERACTION -> BookDebugSettings.interactionBoundsDebug() ? "Interaction: ON" : "Interaction";
            case DEBUG_LABELS -> BookDebugSettings.debugLabels() ? "Labels: ON" : "Labels";
        };
    }

    private int buttonFill(ControlButton button) {
        if (button == ControlButton.SAVE && !templateDirty) {
            return 0x70403028;
        }
        return switch (button) {
            case TEMPLATE_MODE -> templateMode ? 0xB05B4C1D : 0x90403028;
            case SAVE -> 0x905A4020;
            case DISCARD -> 0x90523030;
            case DEBUG_UI -> debugVisible ? 0x90442858 : 0x90303038;
            case DEBUG_TEMPLATE -> BookDebugSettings.templateRegionsDebug() ? 0x9030556F : 0x90303038;
            case DEBUG_MEASURED -> BookDebugSettings.measuredTextBoundsDebug() ? 0x905A3030 : 0x90303038;
            case DEBUG_INTERACTION -> BookDebugSettings.interactionBoundsDebug() ? 0x90404D6A : 0x90303038;
            case DEBUG_LABELS -> BookDebugSettings.debugLabels() ? 0x90604B2C : 0x90303038;
        };
    }

    private int buttonBorder(ControlButton button) {
        if (button == ControlButton.SAVE && !templateDirty) {
            return 0x885E5346;
        }
        return switch (button) {
            case TEMPLATE_MODE -> 0xFFDAB46D;
            case SAVE -> 0xFFD7C08C;
            case DISCARD -> 0xFFD98C8C;
            case DEBUG_UI -> 0xFFB38CFF;
            case DEBUG_TEMPLATE -> 0xFF8FD8FF;
            case DEBUG_MEASURED -> 0xFFFF8F8F;
            case DEBUG_INTERACTION -> 0xFF8FE3C9;
            case DEBUG_LABELS -> 0xFFFFD27A;
        };
    }

    private int buttonTextColor(ControlButton button) {
        if (button == ControlButton.SAVE && !templateDirty) {
            return 0xFF9E9387;
        }
        return 0xFFF5E9D6;
    }

    private static boolean isDebugSubToggle(ControlButton button) {
        return button == ControlButton.DEBUG_TEMPLATE
                || button == ControlButton.DEBUG_MEASURED
                || button == ControlButton.DEBUG_INTERACTION
                || button == ControlButton.DEBUG_LABELS;
    }

    private static List<String> textLinesInRegion(BookPage page, JournalPageStyleSystem.SlotRegion region) {
        List<Line> lines = new ArrayList<>();
        for (BookPageElement element : page.elements()) {
            if (!intersects(region, element)) {
                continue;
            }
            switch (element) {
                case BookPageElement.TextElement text -> lines.add(new Line(text.y(), text.text().getString()));
                case BookPageElement.InteractiveTextElement text -> lines.add(new Line(text.y(), text.text().getString()));
                case BookPageElement.ButtonElement button -> lines.add(new Line(button.y(), button.label().getString()));
                default -> {
                }
            }
        }
        lines.sort(Comparator.comparingInt(Line::y));
        List<String> result = new ArrayList<>();
        for (Line line : lines) {
            result.add(line.text());
        }
        return result;
    }

    private static String firstInteractiveLabelInRegion(BookPage page, JournalPageStyleSystem.SlotRegion region) {
        for (BookPageElement element : page.elements()) {
            if (!intersects(region, element)) {
                continue;
            }
            if (element instanceof BookPageElement.ButtonElement button) {
                return button.label().getString();
            }
            if (element instanceof BookPageElement.InteractiveTextElement text) {
                return text.text().getString();
            }
        }
        return "";
    }

    private static String ledgerRowsInRegion(BookPage page, JournalPageStyleSystem.SlotRegion region) {
        List<BookPageElement.InteractiveTextElement> labels = new ArrayList<>();
        List<BookPageElement.DecorationElement> values = new ArrayList<>();
        for (BookPageElement element : page.elements()) {
            if (!intersects(region, element)) {
                continue;
            }
            if (element instanceof BookPageElement.InteractiveTextElement text) {
                labels.add(text);
            } else if (element instanceof BookPageElement.DecorationElement decoration && !decoration.text().getString().contains(".")) {
                values.add(decoration);
            }
        }
        labels.sort(Comparator.comparingInt(BookPageElement.InteractiveTextElement::y));
        values.sort(Comparator.comparingInt(BookPageElement.DecorationElement::y));
        List<String> lines = new ArrayList<>();
        for (BookPageElement.InteractiveTextElement label : labels) {
            String value = values.stream()
                    .filter(candidate -> Math.abs(candidate.y() - label.y()) <= 2)
                    .map(candidate -> candidate.text().getString())
                    .findFirst()
                    .orElse("0");
            lines.add(label.text().getString() + "|" + value);
        }
        return String.join("\n", lines);
    }

    private static boolean intersects(JournalPageStyleSystem.SlotRegion region, BookPageElement element) {
        return region.x() < element.x() + element.width()
                && region.right() > element.x()
                && region.y() < element.y() + element.height()
                && region.bottom() > element.y();
    }

    private static boolean isMultiline(JournalPageSlot slot) {
        return slot == JournalPageSlot.BODY || slot == JournalPageSlot.STATS || slot == JournalPageSlot.ROWS;
    }

    private static JournalPagePurpose purposeFor(BookPageProvider provider, BookContext context, int pageIndex) {
        if (!(provider instanceof BookTemplateDebugProvider debugProvider)) {
            return null;
        }
        String purposeName = debugProvider.templatePurposeForPageIndex(context, pageIndex);
        if (purposeName == null || purposeName.isBlank()) {
            return null;
        }
        try {
            return JournalPagePurpose.valueOf(purposeName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static JournalPageId pageIdFor(BookScreenController controller, BookViewState viewState, BookPageSide pageSide) {
        if (viewState.displayedSpread() == null) {
            return null;
        }
        if (!(controller.definition().provider() instanceof BookTemplateDebugProvider debugProvider)) {
            return null;
        }
        String pageId = debugProvider.templatePageIdForPageIndex(controller.context(), pageIndex(viewState, pageSide));
        return pageId == null || pageId.isBlank() ? null : new JournalPageId(pageId);
    }

    private static int pageIndex(BookViewState viewState, BookPageSide pageSide) {
        return (viewState.displayedSpreadIndex() * 2) + (pageSide == BookPageSide.RIGHT ? 1 : 0);
    }

    private static boolean contains(JournalPageStyleSystem.SlotRegion region, float x, float y) {
        return x >= region.x() && x <= region.right() && y >= region.y() && y <= region.bottom();
    }

    private static ResizeHandle handleFor(JournalPageStyleSystem.SlotRegion region, float x, float y) {
        boolean left = Math.abs(x - region.x()) <= HANDLE_THRESHOLD;
        boolean right = Math.abs(x - region.right()) <= HANDLE_THRESHOLD;
        boolean top = Math.abs(y - region.y()) <= HANDLE_THRESHOLD;
        boolean bottom = Math.abs(y - region.bottom()) <= HANDLE_THRESHOLD;
        if (left && top) return ResizeHandle.TOP_LEFT;
        if (right && top) return ResizeHandle.TOP_RIGHT;
        if (left && bottom) return ResizeHandle.BOTTOM_LEFT;
        if (right && bottom) return ResizeHandle.BOTTOM_RIGHT;
        if (left) return ResizeHandle.LEFT;
        if (right) return ResizeHandle.RIGHT;
        if (top) return ResizeHandle.TOP;
        if (bottom) return ResizeHandle.BOTTOM;
        return ResizeHandle.MOVE;
    }

    private static JournalPageStyleSystem.SlotRegion resized(JournalPageStyleSystem.SlotRegion start, ResizeHandle handle, int deltaX, int deltaY) {
        int left = start.x();
        int top = start.y();
        int right = start.right();
        int bottom = start.bottom();
        switch (handle) {
            case MOVE -> {
                left += deltaX;
                right += deltaX;
                top += deltaY;
                bottom += deltaY;
            }
            case LEFT -> left += deltaX;
            case RIGHT -> right += deltaX;
            case TOP -> top += deltaY;
            case BOTTOM -> bottom += deltaY;
            case TOP_LEFT -> {
                left += deltaX;
                top += deltaY;
            }
            case TOP_RIGHT -> {
                right += deltaX;
                top += deltaY;
            }
            case BOTTOM_LEFT -> {
                left += deltaX;
                bottom += deltaY;
            }
            case BOTTOM_RIGHT -> {
                right += deltaX;
                bottom += deltaY;
            }
        }
        if (right - left < MIN_SLOT_SIZE) {
            if (handle == ResizeHandle.LEFT || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT) {
                left = right - MIN_SLOT_SIZE;
            } else {
                right = left + MIN_SLOT_SIZE;
            }
        }
        if (bottom - top < MIN_SLOT_SIZE) {
            if (handle == ResizeHandle.TOP || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT) {
                top = bottom - MIN_SLOT_SIZE;
            } else {
                bottom = top + MIN_SLOT_SIZE;
            }
        }
        return new JournalPageStyleSystem.SlotRegion(left, top, right - left, bottom - top);
    }

    private boolean isSelected(JournalPagePurpose purpose, BookPageSide pageSide, JournalPageSlot slot) {
        return selection != null && selection.purpose() == purpose && selection.pageSide() == pageSide && selection.slot() == slot;
    }

    private void drawQuad(GuiGraphics graphics, BookSceneRenderer.ScreenQuad quad, int color) {
        drawLine(graphics, quad.topLeft(), quad.topRight(), color);
        drawLine(graphics, quad.topRight(), quad.bottomRight(), color);
        drawLine(graphics, quad.bottomRight(), quad.bottomLeft(), color);
        drawLine(graphics, quad.bottomLeft(), quad.topLeft(), color);
    }

    private void drawHandle(GuiGraphics graphics, BookSceneRenderer.ScreenRect rect, int color) {
        int x = Math.round(rect.x() + rect.width() - 3);
        int y = Math.round(rect.y() + rect.height() - 3);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
    }

    private void drawLine(GuiGraphics graphics, BookSceneRenderer.ScreenPoint from, BookSceneRenderer.ScreenPoint to, int color) {
        int minX = Math.round(Math.min(from.x(), to.x()));
        int minY = Math.round(Math.min(from.y(), to.y()));
        int maxX = Math.round(Math.max(from.x(), to.x()));
        int maxY = Math.round(Math.max(from.y(), to.y()));
        graphics.fill(minX, minY, Math.max(minX + 1, maxX + 1), Math.max(minY + 1, maxY + 1), color);
    }

    private record Selection(JournalPagePurpose purpose, BookPageSide pageSide, JournalPageSlot slot) {}

    private record ActiveDrag(
            JournalPagePurpose purpose,
            BookPageSide pageSide,
            JournalPageSlot slot,
            JournalPageStyleSystem.SlotRegion startRegion,
            ResizeHandle handle,
            float startLocalX,
            float startLocalY
    ) {}

    private record EditingSession(
            JournalPagePurpose purpose,
            BookPageSide pageSide,
            JournalPageSlot slot,
            JournalPageId pageId,
            int pageIndex,
            StringBuilder buffer
    ) {}

    private record Line(int y, String text) {}

    private record Hit(
            JournalPagePurpose purpose,
            BookPageSide pageSide,
            JournalPageSlot slot,
            JournalPageStyleSystem.SlotRegion region,
            ResizeHandle handle,
            float localX,
            float localY
    ) {}

    private enum ResizeHandle {
        MOVE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private enum ControlButton {
        TEMPLATE_MODE,
        SAVE,
        DISCARD,
        DEBUG_UI,
        DEBUG_TEMPLATE,
        DEBUG_MEASURED,
        DEBUG_INTERACTION,
        DEBUG_LABELS
    }
}
