package net.zoogle.enchiridion.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookProjectionView;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class BookScreen extends Screen {
    private static final boolean SHOW_GUI_TEXT_DEBUG = false;
    private static final boolean DEBUG_PAGE_LOCAL_INTERACTION = true;
    private static final boolean DEBUG_HOVERED_INTERACTIVE_TEXT_BOUNDS = true;
    private static final String[] PROJECTION_GLYPHS = {
            "\u2726", "\u2727", "\u2055", "\u2058", "\u2020", "\u2021", "\u203b", "\u2299",
            "\u27e1", "\u27e2", "\u27e3", "\u27ea", "\u27eb", "\u2303", "\u2234", "\u2235"
    };
    private static final int INITIAL_TEXT_DELAY_TICKS = 32;
    private static final float TEXT_FADE_OUT_PER_TICK = 0.45f;
    private static final float TEXT_FADE_IN_PER_TICK = 0.18f;
    private static final float TEXT_ALPHA_EPSILON = 0.01f;
    private static final float INSPECT_YAW_PER_PIXEL = 0.45f;
    private static final float INSPECT_PITCH_PER_PIXEL = 0.35f;

    private final BookScreenController controller;
    private final BookSceneRenderer sceneRenderer = new BookSceneRenderer();
    private final PageCanvasRenderer pageRenderer = new PageCanvasRenderer();
    private final BookInteractionResolver interactionResolver = new BookInteractionResolver();

    private BookLayout layout;
    private BookSpread displayedSpread;
    private int displayedSpreadIndex;
    private BookSpread targetSpread;
    private int targetSpreadIndex;
    private float textAlpha = 1.0f;
    private TextTransitionState textTransitionState = TextTransitionState.STABLE;
    private int initialTextDelayTicksRemaining;
    private boolean closedBookHovered;
    private boolean closedBookInspecting;
    private List<BookInteractionResolver.ResolvedInteractiveTarget> resolvedInteractiveTargets = List.of();
    private BookInteractionResolver.ResolvedInteractiveTarget hoveredInteractiveTarget;
    private ProjectionButton hoveredProjectionButton;
    private BookInteractionResolver.PageInteractionDebugState pageInteractionDebugState;
    private float inspectYaw;
    private float inspectPitch;
    private float currentProjectionFocusOffset;
    private double lastInspectMouseX;
    private double lastInspectMouseY;

    public BookScreen(BookDefinition definition) {
        this(definition, createContext(definition));
    }

    public BookScreen(BookDefinition definition, BookContext context) {
        super(definition.title());
        this.controller = new BookScreenController(definition, context);
    }

    private static BookContext createContext(BookDefinition definition) {
        Minecraft minecraft = Minecraft.getInstance();
        return BookContext.of(minecraft, minecraft.player, definition.id());
    }

    @Override
    protected void init() {
        layout = BookLayout.fromScreen(width, height);
        if (displayedSpread == null) {
            syncDisplayedSpreadFromController();
            return;
        }

        targetSpread = controller.currentSpread();
        targetSpreadIndex = controller.spreadIndex();
        if (displayedSpreadIndex != targetSpreadIndex) {
            displayedSpread = targetSpread;
            displayedSpreadIndex = targetSpreadIndex;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        controller.tick();
        updateTextTransition();
        currentProjectionFocusOffset += (targetProjectionFocusOffset() - currentProjectionFocusOffset) * 0.2f;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null) {
            layout = BookLayout.fromScreen(width, height);
        }

        renderBackground(graphics, mouseX, mouseY, partialTick);
        if (displayedSpread == null) {
            syncDisplayedSpreadFromController();
        }
        closedBookHovered = controller.isClosed() && sceneRenderer.isClosedBookHovered(layout, mouseX, mouseY);
        sceneRenderer.preparePresentation(
                layout,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                mouseX,
                mouseY,
                closedBookHovered,
                inspectYaw,
                inspectPitch
        );
        hoveredProjectionButton = resolveProjectionButton(mouseX, mouseY);
        BookInteractionResolver.Resolution interaction = interactionResolver.resolve(
                controller,
                sceneRenderer,
                layout,
                displayedSpread,
                displayedSpreadIndex,
                currentProjectionFocusOffset,
                mouseX,
                mouseY,
                DEBUG_PAGE_LOCAL_INTERACTION
        );
        hoveredInteractiveTarget = interaction.hoveredTarget();
        resolvedInteractiveTargets = interaction.targets();
        pageInteractionDebugState = interaction.debugState();
        sceneRenderer.renderBook(
                graphics,
                layout,
                displayedSpread,
                displayedSpreadIndex,
                controller.visualState(),
                controller.animationProgress(),
                controller.projectionProgress(),
                currentProjectionFocusOffset,
                focusedProjectionPageSide(),
                interactionResolver.hoveredInteractiveElement(interaction, displayedSpread, BookPageSide.LEFT),
                interactionResolver.hoveredInteractiveElement(interaction, displayedSpread, BookPageSide.RIGHT),
                mouseX,
                mouseY,
                textAlpha,
                partialTick,
                closedBookHovered,
                inspectYaw,
                inspectPitch
        );

        if (SHOW_GUI_TEXT_DEBUG) {
            pageRenderer.renderPage(
                    graphics,
                    displayedSpread.left(),
                    layout.leftPageX(),
                    layout.leftPageY(),
                    layout.pageWidth(),
                    layout.pageHeight(),
                    textAlpha
            );
            pageRenderer.renderPage(
                    graphics,
                    displayedSpread.right(),
                    layout.rightPageX(),
                    layout.rightPageY(),
                    layout.pageWidth(),
                    layout.pageHeight(),
                    textAlpha,
                    PageCanvasRenderer.RIGHT_PAGE_INSET
            );
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTopmostOverlay(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (controller.isClosing()) {
            return true;
        }
        if (controller.isProjectionVisible()) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
                return controller.nextProjectionFocus();
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
                return controller.previousProjectionFocus();
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                controller.closeProjection();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            if (controller.isJournalReadable()) {
                controller.nextSpread();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            if (controller.isJournalReadable()) {
                controller.previousSpread();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (controller.isClosed() || controller.isArriving()) {
                super.onClose();
            } else {
                controller.beginClosing();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (controller.isClosed() || controller.isArriving()) {
            super.onClose();
            return;
        }
        controller.beginClosing();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean hoveringClosedBook = layout != null && sceneRenderer.isClosedBookHovered(layout, (int) mouseX, (int) mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && controller.isClosed() && hoveringClosedBook) {
            resetInspectRotation();
            if (controller.beginOpening()) {
                syncDisplayedSpreadFromController();
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && controller.isClosed() && hoveringClosedBook) {
            closedBookInspecting = true;
            lastInspectMouseX = mouseX;
            lastInspectMouseY = mouseY;
            return true;
        }
        if (controller.isProjectionInteractive()) {
            ProjectionButton projectionButton = resolveProjectionButton((int) mouseX, (int) mouseY);
            if (projectionButton != null && projectionButton.view().hasPrimaryAction()) {
                BookProjectionView view = projectionButton.view();
                if (view.primaryAction().onClick(controller.context(), displayedSpreadIndex, button)) {
                    return true;
                }
            }
            return false;
        }
        if (controller.isJournalReadable()) {
            BookInteractionResolver.Resolution interaction = interactionResolver.resolve(
                    controller,
                    sceneRenderer,
                    layout,
                    displayedSpread,
                    displayedSpreadIndex,
                    currentProjectionFocusOffset,
                    (int) mouseX,
                    (int) mouseY,
                    false
            );
            BookInteractionResolver.ResolvedInteractiveTarget hoveredTarget = interaction.hoveredTarget();
            if (hoveredTarget != null && hoveredTarget.enabled() && hoveredTarget.action().onClick(controller.context(), displayedSpreadIndex, button)) {
                return true;
            }
        }
        if (!controller.isJournalReadable()) {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && controller.isClosed() && closedBookInspecting) {
            inspectYaw += (float) ((mouseX - lastInspectMouseX) * INSPECT_YAW_PER_PIXEL);
            inspectPitch -= (float) ((mouseY - lastInspectMouseY) * INSPECT_PITCH_PER_PIXEL);
            inspectYaw = wrapDegrees(inspectYaw);
            lastInspectMouseX = mouseX;
            lastInspectMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            closedBookInspecting = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean jumpToSpread(int spreadIndex) {
        if (!controller.isJournalReadable()) {
            return false;
        }
        return controller.jumpToSpread(spreadIndex);
    }

    public boolean openSkillProjection(String focusId) {
        return controller.beginProjection(focusId);
    }

    private float projectionFocusOffset() {
        if (!controller.isProjectionVisible()) {
            return 0.0f;
        }
        int pageIndex = controller.selectedProjectionPageIndex();
        if (pageIndex < 0) {
            return 0.0f;
        }
        boolean focusLeft = (pageIndex % 2) == 0;
        return focusLeft ? BookSceneRenderer.PROJECTION_FOCUS_X_OFFSET : -BookSceneRenderer.PROJECTION_FOCUS_X_OFFSET;
    }

    private float targetProjectionFocusOffset() {
        return projectionFocusOffset();
    }

    private BookPageSide focusedProjectionPageSide() {
        if (!controller.isProjectionVisible()) {
            return null;
        }
        int pageIndex = controller.selectedProjectionPageIndex();
        if (pageIndex < 0) {
            return null;
        }
        return (pageIndex % 2) == 0 ? BookPageSide.LEFT : BookPageSide.RIGHT;
    }

    private void updateTextTransition() {
        if (displayedSpread == null) {
            syncDisplayedSpreadFromController();
        }

        targetSpread = controller.currentSpread();
        targetSpreadIndex = controller.spreadIndex();

        boolean flipping = controller.state() == BookAnimState.FLIPPING_NEXT || controller.state() == BookAnimState.FLIPPING_PREV;
        boolean arriving = controller.isArriving();
        boolean opening = controller.isOpening();
        boolean closed = controller.isClosed();
        boolean closing = controller.isClosing();
        if (arriving || closed || closing) {
            textTransitionState = TextTransitionState.FADING_OUT;
            textAlpha = Math.max(0.0f, textAlpha - TEXT_FADE_OUT_PER_TICK);
            return;
        }
        if (opening) {
            textTransitionState = TextTransitionState.INITIAL_DELAY;
            if (initialTextDelayTicksRemaining > 0) {
                initialTextDelayTicksRemaining--;
                textAlpha = 0.0f;
                return;
            }
        }
        if (flipping) {
            if (textTransitionState == TextTransitionState.STABLE || textTransitionState == TextTransitionState.FADING_IN) {
                textTransitionState = TextTransitionState.FADING_OUT;
            }

            if (textTransitionState == TextTransitionState.FADING_OUT) {
                textAlpha = Math.max(0.0f, textAlpha - TEXT_FADE_OUT_PER_TICK);
                if (textAlpha <= TEXT_ALPHA_EPSILON) {
                    textAlpha = 0.0f;
                    textTransitionState = TextTransitionState.HIDDEN_DURING_FLIP;
                }
            } else {
                textAlpha = 0.0f;
            }

            if (controller.animationProgress() >= 0.5f && displayedSpreadIndex != targetSpreadIndex && textAlpha <= TEXT_ALPHA_EPSILON) {
                displayedSpread = targetSpread;
                displayedSpreadIndex = targetSpreadIndex;
            }
            return;
        }

        if (displayedSpreadIndex != targetSpreadIndex) {
            displayedSpread = targetSpread;
            displayedSpreadIndex = targetSpreadIndex;
        }

        if (textTransitionState == TextTransitionState.INITIAL_DELAY) {
            if (initialTextDelayTicksRemaining > 0) {
                initialTextDelayTicksRemaining--;
                textAlpha = 0.0f;
                return;
            }

            textTransitionState = TextTransitionState.FADING_IN;
        }

        if (textTransitionState == TextTransitionState.HIDDEN_DURING_FLIP || textTransitionState == TextTransitionState.FADING_OUT) {
            textTransitionState = TextTransitionState.FADING_IN;
            textAlpha = 0.0f;
        }

        if (textTransitionState == TextTransitionState.FADING_IN) {
            textAlpha = Math.min(1.0f, textAlpha + TEXT_FADE_IN_PER_TICK);
            if (textAlpha >= 1.0f - TEXT_ALPHA_EPSILON) {
                textAlpha = 1.0f;
                textTransitionState = TextTransitionState.STABLE;
            }
            return;
        }

        textAlpha = 1.0f;
        textTransitionState = TextTransitionState.STABLE;
    }

    private void syncDisplayedSpreadFromController() {
        displayedSpread = controller.currentSpread();
        displayedSpreadIndex = controller.spreadIndex();
        targetSpread = displayedSpread;
        targetSpreadIndex = displayedSpreadIndex;
        textAlpha = 0.0f;
        textTransitionState = controller.isClosed() ? TextTransitionState.FADING_OUT : TextTransitionState.INITIAL_DELAY;
        initialTextDelayTicksRemaining = INITIAL_TEXT_DELAY_TICKS;
    }

    private List<FormattedCharSequence> tooltipLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(title.getVisualOrderText());
        lines.add(Component.literal("Click to open | Esc to leave").getVisualOrderText());
        return lines;
    }

    private void resetInspectRotation() {
        inspectYaw = 0.0f;
        inspectPitch = 0.0f;
        closedBookInspecting = false;
    }

    private ProjectionButton resolveProjectionButton(int mouseX, int mouseY) {
        if (layout == null || !controller.isProjectionInteractive()) {
            return null;
        }
        BookProjectionView view = controller.projectionView();
        if (view == null || !view.hasPrimaryAction()) {
            return null;
        }
        PanelLayout panel = projectionPanelLayout();
        int buttonWidth = Math.min(120, panel.width() - 32);
        int buttonHeight = 16;
        int buttonX = panel.x() + (panel.width() - buttonWidth) / 2;
        int buttonY = panel.y() + panel.height() - 28;
        if (mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
            return new ProjectionButton(view, buttonX, buttonY, buttonWidth, buttonHeight);
        }
        return null;
    }

    private void renderProjectionOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        BookProjectionView view = controller.projectionView();
        if (view == null) {
            return;
        }

        float alpha = controller.projectionProgress();
        PanelLayout panel = projectionPanelLayout();
        renderProjectionBackdrop(graphics, panel, alpha);
        renderPanelBase(graphics, panel, alpha);
        renderGlyphDither(graphics, panel, alpha, view.focusId());
        renderProjectionOrnaments(graphics, panel, alpha);
        renderSkillGlyphIcon(graphics, panel, view, alpha);
        renderProjectionText(graphics, panel, view, alpha);
        drawCenteredText(
                graphics,
                Component.literal("Esc to return to the journal"),
                panel.x(),
                panel.width(),
                panel.y() + panel.height() - 14,
                withAlpha(0xFF5A4636, Math.max(alpha, 0.95f))
        );

        if (view.hasPrimaryAction()) {
            drawProjectionButton(graphics, hoveredProjectionButton, alpha);
        }
    }

    private void renderProjectionBackdrop(GuiGraphics graphics, PanelLayout panel, float alpha) {
        graphics.fill(0, 0, width, height, withAlpha(0x8A000000, alpha));

        int left = panel.x();
        int top = panel.y();
        int right = panel.x() + panel.width();
        int bottom = panel.y() + panel.height();

        int halo = withAlpha(0x38110D18, Math.max(alpha, 0.85f));
        int glow = withAlpha(0x1E6B52C2, Math.max(alpha, 0.7f));
        int corner = withAlpha(0x500B0910, Math.max(alpha, 0.82f));

        graphics.fill(left - 24, top + 10, left - 12, bottom - 14, halo);
        graphics.fill(right + 12, top + 14, right + 24, bottom - 10, halo);
        graphics.fill(left + 22, top - 16, right - 26, top - 7, halo);
        graphics.fill(left + 26, bottom + 7, right - 20, bottom + 16, halo);

        graphics.fill(left - 18, top + 34, left - 12, bottom - 34, withAlpha(0x220B0910, Math.max(alpha, 0.72f)));
        graphics.fill(right + 12, top + 30, right + 18, bottom - 38, withAlpha(0x1C0B0910, Math.max(alpha, 0.68f)));

        graphics.fill(left - 12, top - 10, left + 10, top + 12, corner);
        graphics.fill(right - 10, top - 10, right + 12, top + 12, corner);
        graphics.fill(left - 12, bottom - 12, left + 10, bottom + 10, corner);
        graphics.fill(right - 10, bottom - 12, right + 12, bottom + 10, corner);

        graphics.fill(left - 8, top + 20, left - 2, bottom - 20, glow);
        graphics.fill(right + 2, top + 20, right + 8, bottom - 20, glow);
        graphics.fill(left + 26, top - 6, right - 26, top - 2, glow);
        graphics.fill(left + 26, bottom + 2, right - 26, bottom + 6, glow);
    }

    private void renderPanelBase(GuiGraphics graphics, PanelLayout panel, float alpha) {
        int base = withAlpha(0xF2E2D4BC, Math.max(alpha, 0.97f));
        int inner = withAlpha(0x34FFF5DE, Math.max(alpha, 0.45f));
        int edgeShade = withAlpha(0x246A5133, Math.max(alpha, 0.85f));
        int innerShade = withAlpha(0x1280674A, Math.max(alpha, 0.8f));
        int centerSupport = withAlpha(0x14917A56, Math.max(alpha, 0.75f));
        graphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), base);
        graphics.fill(panel.x() + 6, panel.y() + 6, panel.x() + panel.width() - 6, panel.y() + panel.height() - 6, inner);

        graphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + 7, edgeShade);
        graphics.fill(panel.x(), panel.y() + panel.height() - 7, panel.x() + panel.width(), panel.y() + panel.height(), edgeShade);
        graphics.fill(panel.x(), panel.y(), panel.x() + 7, panel.y() + panel.height(), edgeShade);
        graphics.fill(panel.x() + panel.width() - 7, panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), edgeShade);

        graphics.fill(panel.x() + 18, panel.y() + 18, panel.x() + panel.width() - 18, panel.y() + panel.height() - 28, innerShade);
        graphics.fill(panel.x() + 28, panel.y() + 52, panel.x() + panel.width() - 28, panel.y() + 53, centerSupport);
        graphics.fill(panel.x() + 34, panel.y() + 72, panel.x() + panel.width() - 34, panel.y() + 73, withAlpha(0x0E8E7350, alpha));
    }

    private void renderProjectionOrnaments(GuiGraphics graphics, PanelLayout panel, float alpha) {
        int accent = withAlpha(0xFF6F53C0, Math.max(alpha, 0.88f));
        int support = withAlpha(0xFF9C7B51, Math.max(alpha, 0.72f));
        renderCornerMark(graphics, panel.x() + 10, panel.y() + 8, false, accent, support);
        renderCornerMark(graphics, panel.x() + panel.width() - 18, panel.y() + 8, true, accent, support);
        renderCornerMark(graphics, panel.x() + 10, panel.y() + panel.height() - 22, false, support, withAlpha(0xFF6F53C0, Math.max(alpha, 0.6f)));
        renderCornerMark(graphics, panel.x() + panel.width() - 18, panel.y() + panel.height() - 22, true, support, withAlpha(0xFF6F53C0, Math.max(alpha, 0.6f)));
        renderArcaneDivider(graphics, panel, panel.y() + 38, accent, support);
        graphics.drawString(font, "\u2058", panel.x() + 18, panel.y() + 29, withAlpha(0xAA8E7350, Math.max(alpha, 0.7f)), false);
        graphics.drawString(font, "\u2234", panel.x() + panel.width() - 26, panel.y() + 30, withAlpha(0x88906E48, Math.max(alpha, 0.64f)), false);
        graphics.drawString(font, "\u2235", panel.x() + 16, panel.y() + panel.height() - 30, withAlpha(0x66906E48, Math.max(alpha, 0.58f)), false);
        graphics.drawString(font, "\u2055", panel.x() + panel.width() - 24, panel.y() + panel.height() - 30, withAlpha(0x88805CCB, Math.max(alpha, 0.62f)), false);
    }

    private void renderGlyphDither(GuiGraphics graphics, PanelLayout panel, float alpha, String seedText) {
        long timeBucket = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() / 6L : 0L;
        long baseSeed = (seedText == null ? 0L : seedText.hashCode()) ^ timeBucket;
        renderGlyphBand(graphics, panel.x() + 14, panel.y() + 12, panel.width() - 28, true, alpha, baseSeed ^ 0x11L);
        renderGlyphBand(graphics, panel.x() + 14, panel.y() + panel.height() - 18, panel.width() - 28, true, alpha, baseSeed ^ 0x21L);
        renderGlyphBandVertical(graphics, panel.x() + 8, panel.y() + 18, panel.height() - 40, alpha, baseSeed ^ 0x31L);
        renderGlyphBandVertical(graphics, panel.x() + panel.width() - 14, panel.y() + 18, panel.height() - 40, alpha, baseSeed ^ 0x41L);
        renderCornerCluster(graphics, panel.x() + 12, panel.y() + 12, alpha, baseSeed ^ 0x51L);
        renderCornerCluster(graphics, panel.x() + panel.width() - 32, panel.y() + 12, alpha, baseSeed ^ 0x61L);
        renderCornerCluster(graphics, panel.x() + 12, panel.y() + panel.height() - 34, alpha, baseSeed ^ 0x71L);
        renderCornerCluster(graphics, panel.x() + panel.width() - 32, panel.y() + panel.height() - 34, alpha, baseSeed ^ 0x81L);
    }

    private void renderSkillGlyphIcon(GuiGraphics graphics, PanelLayout panel, BookProjectionView view, float alpha) {
        String icon = skillGlyphIcon(view.focusId());
        int iconColor = withAlpha(0xFF5E43B8, Math.max(alpha, 0.94f));
        int support = withAlpha(0xFFC4A673, Math.max(alpha, 0.62f));
        graphics.drawString(font, "\u27ea", panel.x() + 18, panel.y() + 20, support, false);
        graphics.drawString(font, icon, panel.x() + 29, panel.y() + 19, iconColor, false);
        graphics.drawString(font, "\u2058", panel.x() + 36, panel.y() + 29, withAlpha(0x9A8E7350, Math.max(alpha, 0.62f)), false);
        graphics.drawString(font, "\u27eb", panel.x() + 61, panel.y() + 20, support, false);
    }

    private void renderProjectionText(GuiGraphics graphics, PanelLayout panel, BookProjectionView view, float alpha) {
        int textX = panel.x() + 18;
        int textWidth = panel.width() - 36;
        int cursorY = panel.y() + 15;
        drawProjectionTitle(graphics, panel, view, cursorY, alpha);
        cursorY += 22;
        drawLargeCenteredText(graphics, view.dominantValue().getString(), panel.x(), panel.width(), cursorY, withAlpha(0xFF22140F, Math.max(alpha, 0.99f)), 2.6f);
        cursorY += 28;
        drawCenteredText(graphics, view.secondaryValue(), panel.x(), panel.width(), cursorY, withAlpha(0xFF654718, Math.max(alpha, 0.98f)));
        cursorY += 14;
        cursorY = drawWrappedText(graphics, view.description(), textX, cursorY, textWidth, withAlpha(0xFF2A2018, Math.max(alpha, 0.98f)), 2);
        cursorY += 6;
        renderProjectionProgressBar(graphics, panel, view, cursorY, alpha);
        cursorY += 22;
        int statusColor = view.emphasizedStatus() ? withAlpha(0xFF5E2781, Math.max(alpha, 0.99f)) : withAlpha(0xFF4A3829, Math.max(alpha, 0.97f));
        drawCenteredText(graphics, view.statusLabel(), panel.x(), panel.width(), cursorY, statusColor);
    }

    private void renderProjectionProgressBar(GuiGraphics graphics, PanelLayout panel, BookProjectionView view, int y, float alpha) {
        int barX = panel.x() + 18;
        int barWidth = panel.width() - 36;
        int back = withAlpha(0x8864533A, Math.max(alpha, 0.86f));
        int interference = withAlpha(0x2C7E5FD1, Math.max(alpha, 0.75f));
        graphics.fill(barX, y, barX + barWidth, y + 10, back);
        for (int stripe = 0; stripe < barWidth; stripe += 14) {
            graphics.fill(barX + stripe, y + 1, Math.min(barX + stripe + 5, barX + barWidth), y + 9, interference);
        }
        drawProgressBar(graphics, barX, y + 1, barWidth, 8, view.progress(), alpha);
        drawCenteredText(graphics, view.progressLabel(), panel.x(), panel.width(), y + 14, withAlpha(0xFF38261B, Math.max(alpha, 0.98f)));
    }

    private void renderGlyphBand(GuiGraphics graphics, int x, int y, int width, boolean top, float alpha, long seedBase) {
        int step = 12;
        for (int offset = 0; offset < width; offset += step) {
            long seed = mix64(seedBase + offset);
            if (((seed >>> 8) & 3L) == 0L) {
                continue;
            }
            String glyph = PROJECTION_GLYPHS[Math.floorMod((int) (seed >>> 32), PROJECTION_GLYPHS.length)];
            int drawY = y + (top ? Math.floorMod((int) (seed >>> 18), 3) : -Math.floorMod((int) (seed >>> 18), 3));
            int color = withAlpha(
                    (offset % 24 == 0) ? 0xFF7657C8 : 0xFF8A744F,
                    Math.max(alpha, 0.8f) * (((seed >>> 12) & 1L) == 0L ? 0.28f : 0.18f)
            );
            graphics.drawString(font, glyph, x + offset, drawY, color, false);
        }
    }

    private void renderGlyphBandVertical(GuiGraphics graphics, int x, int y, int height, float alpha, long seedBase) {
        int step = 11;
        for (int offset = 0; offset < height; offset += step) {
            long seed = mix64(seedBase + offset);
            if (((seed >>> 9) & 3L) == 0L) {
                continue;
            }
            String glyph = PROJECTION_GLYPHS[Math.floorMod((int) (seed >>> 28), PROJECTION_GLYPHS.length)];
            int drawX = x + Math.floorMod((int) (seed >>> 16), 3);
            int color = withAlpha(
                    (offset % 22 == 0) ? 0xFF7657C8 : 0xFF8A744F,
                    Math.max(alpha, 0.8f) * (((seed >>> 13) & 1L) == 0L ? 0.24f : 0.15f)
            );
            graphics.drawString(font, glyph, drawX, y + offset, color, false);
        }
    }

    private void renderCornerCluster(GuiGraphics graphics, int x, int y, float alpha, long seedBase) {
        for (int index = 0; index < 5; index++) {
            long seed = mix64(seedBase + (index * 17L));
            String glyph = PROJECTION_GLYPHS[Math.floorMod((int) (seed >>> 24), PROJECTION_GLYPHS.length)];
            int drawX = x + Math.floorMod((int) seed, 12);
            int drawY = y + Math.floorMod((int) (seed >>> 12), 10);
            int color = withAlpha(
                    (index == 0) ? 0xFF7B5FD1 : 0xFF967A51,
                    Math.max(alpha, 0.8f) * (index == 0 ? 0.32f : 0.18f)
            );
            graphics.drawString(font, glyph, drawX, drawY, color, false);
        }
    }

    private void renderArcaneDivider(GuiGraphics graphics, PanelLayout panel, int y, int accent, int support) {
        int centerX = panel.x() + panel.width() / 2;
        graphics.fill(panel.x() + 52, y + 4, centerX - 26, y + 5, withAlpha(support, 0.72f));
        graphics.fill(centerX + 28, y + 4, panel.x() + panel.width() - 40, y + 5, withAlpha(support, 0.54f));
        graphics.drawString(font, "\u27ea \u2058", panel.x() + 20, y, withAlpha(accent, 0.92f), false);
        graphics.drawString(font, "\u2058 \u2726 \u2058", centerX - 14, y, accent, false);
        graphics.drawString(font, "\u2234", panel.x() + panel.width() - 22, y, withAlpha(support, 0.74f), false);
    }

    private void drawProjectionTitle(GuiGraphics graphics, PanelLayout panel, BookProjectionView view, int y, float alpha) {
        int titleColor = withAlpha(0xFF311043, Math.max(alpha, 0.995f));
        int haloColor = withAlpha(0xE0EEE0C0, Math.max(alpha, 0.74f));
        int support = withAlpha(0xB28E7350, Math.max(alpha, 0.72f));
        int titleWidth = font.width(view.title());
        int titleX = panel.x() + (panel.width() - titleWidth) / 2;
        graphics.fill(panel.x() + 54, y + 10, panel.x() + panel.width() - 54, y + 11, withAlpha(0x1A8B7251, alpha));
        graphics.drawString(font, "\u2058", titleX - 12, y, support, false);
        graphics.drawString(font, view.title(), titleX + 1, y + 1, haloColor, false);
        graphics.drawString(font, view.title(), titleX, y, titleColor, false);
        graphics.drawString(font, "\u2058", titleX + titleWidth + 6, y, support, false);
    }

    private void renderCornerMark(GuiGraphics graphics, int x, int y, boolean rightSide, int outer, int inner) {
        if (rightSide) {
            graphics.drawString(font, "\u27eb", x, y, outer, false);
            graphics.drawString(font, "\u2058", x - 8, y + 4, inner, false);
        } else {
            graphics.drawString(font, "\u27ea", x, y, outer, false);
            graphics.drawString(font, "\u2058", x + 8, y + 4, inner, false);
        }
    }

    private void renderTopmostOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);

        try {
            if (hoveredInteractiveTarget != null && hoveredInteractiveTarget.role() == BookInteractionResolver.InteractiveVisualRole.PAGE_REGION) {
                drawRegionHover(graphics, hoveredInteractiveTarget);
            }
            drawVisiblePageActions(graphics);

            if (controller.isProjectionVisible()) {
                renderProjectionOverlay(graphics, mouseX, mouseY);
            }

            if (DEBUG_HOVERED_INTERACTIVE_TEXT_BOUNDS) {
                drawHoveredInteractiveTextBounds(graphics);
            }

            if (DEBUG_PAGE_LOCAL_INTERACTION) {
                drawInteractiveTextDebug(graphics, mouseX, mouseY);
            }
            renderOverlayTooltips(graphics, mouseX, mouseY);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private void renderOverlayTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (closedBookHovered && controller.isClosed()) {
            graphics.renderTooltip(font, tooltipLines(), mouseX, mouseY);
        } else if (hoveredProjectionButton != null && hoveredProjectionButton.view().primaryActionLabel() != null) {
            graphics.renderTooltip(font, hoveredProjectionButton.view().primaryActionLabel(), mouseX, mouseY);
        } else if (hoveredInteractiveTarget != null && hoveredInteractiveTarget.tooltip() != null) {
            int tooltipX = Math.round(hoveredInteractiveTarget.screenRect().x() + (hoveredInteractiveTarget.screenRect().width() / 2.0f));
            int tooltipY = Math.round(hoveredInteractiveTarget.screenRect().y() + Math.max(0.0f, hoveredInteractiveTarget.screenRect().height() / 2.0f));
            graphics.renderTooltip(font, hoveredInteractiveTarget.tooltip(), tooltipX, tooltipY);
        }
    }

    private void drawProjectionButton(GuiGraphics graphics, ProjectionButton button, float alpha) {
        if (button == null) {
            BookProjectionView view = controller.projectionView();
            if (view == null || !view.hasPrimaryAction()) {
                return;
            }
            PanelLayout panel = projectionPanelLayout();
            int buttonWidth = Math.min(120, panel.width() - 32);
            int buttonHeight = 16;
            int buttonX = panel.x() + (panel.width() - buttonWidth) / 2;
            int buttonY = panel.y() + panel.height() - 28;
            button = new ProjectionButton(view, buttonX, buttonY, buttonWidth, buttonHeight);
        }

        boolean hovered = hoveredProjectionButton != null;
        int base = withAlpha(hovered ? 0xDAE4D8F8 : 0xD8E4D7C4, Math.max(alpha, 0.98f));
        int inner = withAlpha(hovered ? 0x346A4FC4 : 0x1E8A7354, Math.max(alpha, 0.9f));
        int accent = withAlpha(hovered ? 0xFF7F5ED1 : 0xFF9E7C52, Math.max(alpha, 0.98f));
        graphics.fill(button.x(), button.y() + 2, button.x() + button.width(), button.y() + button.height() - 2, base);
        graphics.fill(button.x() + 4, button.y() + 5, button.x() + button.width() - 4, button.y() + button.height() - 5, inner);
        graphics.fill(button.x() + 10, button.y() + 2, button.x() + button.width() - 10, button.y() + 3, accent);
        graphics.fill(button.x() + 10, button.y() + button.height() - 3, button.x() + button.width() - 10, button.y() + button.height() - 2, accent);
        graphics.drawString(font, "\u27ea", button.x() + 6, button.y() + 4, accent, false);
        graphics.drawString(font, "\u27eb", button.x() + button.width() - 12, button.y() + 4, accent, false);
        drawCenteredText(graphics, button.view().primaryActionLabel(), button.x(), button.width(), button.y() + 4, withAlpha(0xFF24160F, Math.max(alpha, 0.99f)));
    }

    private PanelLayout projectionPanelLayout() {
        int width = Math.min(240, this.width - 36);
        int height = 152;
        int x = (this.width - width) / 2;
        int y = Math.max(16, ((this.height - height) / 2) - 12);
        return new PanelLayout(x, y, width, height);
    }

    private void drawCenteredText(GuiGraphics graphics, Component text, int x, int width, int y, int color) {
        int textWidth = font.width(text);
        graphics.drawString(font, text, x + (width - textWidth) / 2, y, color, false);
    }

    private void drawLargeCenteredText(GuiGraphics graphics, String text, int x, int width, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        float scaledWidth = font.width(text);
        float drawX = (x + (width / 2.0f)) / scale - (scaledWidth / 2.0f);
        float drawY = y / scale;
        graphics.drawString(font, text, Math.round(drawX), Math.round(drawY), color, false);
        graphics.pose().popPose();
    }

    private int drawWrappedText(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            graphics.drawString(font, lines.get(i), x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, float alpha) {
        int clampedProgress = Mth.floor(width * progress);
        graphics.fill(x, y, x + width, y + height, withAlpha(0xCC7D6A52, Math.max(alpha, 0.95f)));
        graphics.fill(x, y, x + clampedProgress, y + height, withAlpha(0xFF7A3CC2, Math.max(alpha, 0.99f)));
        graphics.fill(x, y, x + width, y + 1, withAlpha(0xFFF0D48A, Math.max(alpha, 0.98f)));
        graphics.fill(x, y + height - 1, x + width, y + height, withAlpha(0xFF5A4E3A, Math.max(alpha, 0.95f)));
    }

    private void drawVisiblePageActions(GuiGraphics graphics) {
        if (layout == null || !controller.isJournalReadable()) {
            return;
        }
        for (BookInteractionResolver.ResolvedInteractiveTarget region : resolvedInteractiveTargets) {
            if (region.role() != BookInteractionResolver.InteractiveVisualRole.LABELED_ACTION || region.region() == null) {
                continue;
            }
            boolean hovered = hoveredInteractiveTarget != null
                    && hoveredInteractiveTarget.stableId().equals(region.stableId());
            drawVisibleActionButton(
                    graphics,
                    region.region().visibleLabel(),
                    Math.round(region.screenRect().x()),
                    Math.round(region.screenRect().y()),
                    Math.round(region.screenRect().width()),
                    Math.round(region.screenRect().height()),
                    hovered
            );
        }
    }

    private void drawInteractiveTextDebug(GuiGraphics graphics, int mouseX, int mouseY) {
        if (pageInteractionDebugState != null) {
            drawPageSideDebug(graphics, pageInteractionDebugState.left(), 0xCC4FD6A8, 0xFFB9FFF0);
            drawPageSideDebug(graphics, pageInteractionDebugState.right(), 0xCCD66A4F, 0xFFFFE0B9);

            int debugY = height - 38;
            drawDebugLine(graphics, describePageDebug("L", pageInteractionDebugState.left()), 8, debugY);
            drawDebugLine(graphics, describePageDebug("R", pageInteractionDebugState.right()), 8, debugY + 10);
        }

        for (BookPageElement.InteractiveTextElement text : interactionResolver.pageInteractiveElements(displayedSpread)) {
            BookSceneRenderer.ScreenRect rect = sceneRenderer.projectPageRect(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    currentProjectionFocusOffset,
                    interactionResolver.pageSideFor(displayedSpread, text),
                    text.x(),
                    text.y(),
                    text.width(),
                    text.height()
            );
            int color = hoveredInteractiveTarget != null
                    && hoveredInteractiveTarget.role() == BookInteractionResolver.InteractiveVisualRole.MANUSCRIPT_LINK
                    && hoveredInteractiveTarget.textElement() == text ? 0xCCAA33FF : 0xAA33CC66;
            int x0 = Math.round(rect.x());
            int y0 = Math.round(rect.y());
            int x1 = Math.round(rect.x() + rect.width());
            int y1 = Math.round(rect.y() + rect.height());
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);
        }
        if (hoveredInteractiveTarget != null && hoveredInteractiveTarget.textElement() != null) {
            BookSceneRenderer.PageLocalPoint localPoint = sceneRenderer.pageLocalPoint(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    currentProjectionFocusOffset,
                    interactionResolver.pageSideFor(displayedSpread, hoveredInteractiveTarget.textElement()),
                    mouseX,
                    mouseY
            );
            if (localPoint != null) {
                graphics.drawString(
                        font,
                        String.format("page %.1f, %.1f", localPoint.localX(), localPoint.localY()),
                        8,
                        height - 16,
                        0xFFFFFFFF,
                        false
                );
            }
        }
    }

    private void drawPageSideDebug(GuiGraphics graphics, BookInteractionResolver.PageDebugSide side, int quadColor, int pointColor) {
        if (side == null || side.bounds() == null) {
            return;
        }
        drawQuadOutline(graphics, side.bounds(), quadColor);
        if (side.mouseProjection() != null) {
            int px = Math.round(side.mouseProjection().x());
            int py = Math.round(side.mouseProjection().y());
            graphics.fill(px - 2, py - 1, px + 3, py + 2, pointColor);
            graphics.fill(px - 1, py - 2, px + 2, py + 3, pointColor);
        }
        if (side.containingText() != null) {
            BookSceneRenderer.ScreenRect rect = sceneRenderer.projectPageRect(
                    layout,
                    controller.visualState(),
                    controller.animationProgress(),
                    controller.projectionProgress(),
                    currentProjectionFocusOffset,
                    side.pageSide(),
                    side.containingText().x(),
                    side.containingText().y(),
                    side.containingText().width(),
                    side.containingText().height()
            );
            int x0 = Math.round(rect.x());
            int y0 = Math.round(rect.y());
            int x1 = Math.round(rect.x() + rect.width());
            int y1 = Math.round(rect.y() + rect.height());
            graphics.fill(x0, y0, x1, y0 + 1, 0xFFFF66CC);
            graphics.fill(x0, y1 - 1, x1, y1, 0xFFFF66CC);
            graphics.fill(x0, y0, x0 + 1, y1, 0xFFFF66CC);
            graphics.fill(x1 - 1, y0, x1, y1, 0xFFFF66CC);
        }
    }

    private void drawQuadOutline(GuiGraphics graphics, BookSceneRenderer.PageSurfaceBounds bounds, int color) {
        drawLineRect(graphics, bounds.topLeft(), bounds.topRight(), color);
        drawLineRect(graphics, bounds.topRight(), bounds.bottomRight(), color);
        drawLineRect(graphics, bounds.bottomRight(), bounds.bottomLeft(), color);
        drawLineRect(graphics, bounds.bottomLeft(), bounds.topLeft(), color);
    }

    private void drawLineRect(GuiGraphics graphics, BookSceneRenderer.ScreenPoint from, BookSceneRenderer.ScreenPoint to, int color) {
        int minX = Math.round(Math.min(from.x(), to.x()));
        int minY = Math.round(Math.min(from.y(), to.y()));
        int maxX = Math.round(Math.max(from.x(), to.x()));
        int maxY = Math.round(Math.max(from.y(), to.y()));
        graphics.fill(minX, minY, Math.max(minX + 1, maxX + 1), Math.max(minY + 1, maxY + 1), color);
    }

    private void drawDebugLine(GuiGraphics graphics, String text, int x, int y) {
        graphics.drawString(font, text, x, y, 0xFFFFFFFF, false);
    }

    private String describePageDebug(String prefix, BookInteractionResolver.PageDebugSide side) {
        if (side == null) {
            return prefix + ": no side";
        }
        if (side.localPoint() == null) {
            return prefix + ": local=null hit=none";
        }
        String hit = side.containingText() == null ? "none" : side.containingText().text().getString();
        return String.format(
                "%s: local=(%.1f, %.1f) hit=%s quad=(%.1f,%.1f)-(%.1f,%.1f)",
                prefix,
                side.localPoint().localX(),
                side.localPoint().localY(),
                hit,
                side.bounds().screenX(),
                side.bounds().screenY(),
                side.bounds().screenX() + side.bounds().screenWidth(),
                side.bounds().screenY() + side.bounds().screenHeight()
        );
    }

    private void drawHoveredInteractiveTextBounds(GuiGraphics graphics) {
        if (hoveredInteractiveTarget == null) {
            return;
        }
        BookSceneRenderer.ScreenRect rect = hoveredInteractiveTarget.screenRect();
        int x0 = Math.round(rect.x());
        int y0 = Math.round(rect.y());
        int x1 = Math.round(rect.x() + rect.width());
        int y1 = Math.round(rect.y() + rect.height());
        int outer = 0xEEFFCC33;
        int inner = 0xEE6A38FF;
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y0, outer);
        graphics.fill(x0 - 1, y1, x1 + 1, y1 + 1, outer);
        graphics.fill(x0 - 1, y0 - 1, x0, y1 + 1, outer);
        graphics.fill(x1, y0 - 1, x1 + 1, y1 + 1, outer);
        graphics.fill(x0, y0, x1, y0 + 1, inner);
        graphics.fill(x0, y1 - 1, x1, y1, inner);
        graphics.fill(x0, y0, x0 + 1, y1, inner);
        graphics.fill(x1 - 1, y0, x1, y1, inner);
    }

    private void drawVisibleActionButton(GuiGraphics graphics, Component label, int x, int y, int width, int height, boolean hovered) {
        int drawWidth = Math.max(36, Math.min(width, font.width(label) + 12));
        int drawHeight = Math.max(10, height - 2);
        int drawX = x + ((width - drawWidth) / 2);
        int drawY = y + ((height - drawHeight) / 2);
        int fill = hovered ? 0xAA65421E : 0x88342011;
        int border = hovered ? 0xFFF0D48A : 0xD0C8A86A;
        graphics.fill(drawX, drawY, drawX + drawWidth, drawY + drawHeight, fill);
        graphics.fill(drawX, drawY, drawX + drawWidth, drawY + 1, border);
        graphics.fill(drawX, drawY + drawHeight - 1, drawX + drawWidth, drawY + drawHeight, border);
        graphics.fill(drawX, drawY, drawX + 1, drawY + drawHeight, border);
        graphics.fill(drawX + drawWidth - 1, drawY, drawX + drawWidth, drawY + drawHeight, border);
        int labelY = drawY + Math.max(1, (drawHeight - font.lineHeight) / 2);
        drawCenteredText(graphics, label, drawX, drawWidth, labelY, hovered ? 0xFFFFF1 : 0xF8E8C8);
    }

    private static int withAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    private void drawCornerGlyph(GuiGraphics graphics, String glyph, int x, int y, int color) {
        graphics.drawString(font, glyph, x, y, color, false);
    }

    private String skillGlyphIcon(String focusId) {
        return switch (focusId == null ? "" : focusId.toLowerCase()) {
            case "valor" -> "\u2020\u2058\u2726";
            case "vitality" -> "\u2756+\u2058";
            case "mining" -> "\u27e1\u2058\u2303";
            case "culinary" -> "\u2058\u2727\u2058";
            case "forging" -> "\u2021\u2058\u2303";
            case "artificing" -> "\u203b\u2058\u2299";
            case "magick" -> "\u2726\u2058\u27e3";
            case "exploration" -> "\u2303\u2058\u2727";
            default -> "\u2726\u2058\u27e1";
        };
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static void drawRegionHover(GuiGraphics graphics, BookInteractionResolver.ResolvedInteractiveTarget region) {
        int x0 = Math.round(region.screenRect().x());
        int y0 = Math.round(region.screenRect().y());
        int x1 = Math.round(region.screenRect().x() + region.screenRect().width());
        int y1 = Math.round(region.screenRect().y() + region.screenRect().height());
        graphics.fill(x0, y0, x1, y1, 0x221B140D);
        graphics.fill(x0, y0, x1, y0 + 1, 0x90D7C38A);
        graphics.fill(x0, y1 - 1, x1, y1, 0x90D7C38A);
        graphics.fill(x0, y0, x0 + 1, y1, 0x90D7C38A);
        graphics.fill(x1 - 1, y0, x1, y1, 0x90D7C38A);
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private enum TextTransitionState {
        INITIAL_DELAY,
        STABLE,
        FADING_OUT,
        HIDDEN_DURING_FLIP,
        FADING_IN
    }

    private record PanelLayout(int x, int y, int width, int height) {}
    private record ProjectionButton(BookProjectionView view, int x, int y, int width, int height) {}
}
