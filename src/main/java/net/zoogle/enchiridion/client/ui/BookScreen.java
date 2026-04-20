package net.zoogle.enchiridion.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookDefinition;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;
import org.lwjgl.glfw.GLFW;

public final class BookScreen extends Screen {
    private static final boolean SHOW_GUI_TEXT_DEBUG = false;
    private static final int INITIAL_TEXT_DELAY_TICKS = 32;
    private static final float TEXT_FADE_OUT_PER_TICK = 0.45f;
    private static final float TEXT_FADE_IN_PER_TICK = 0.18f;
    private static final float TEXT_ALPHA_EPSILON = 0.01f;

    private final BookScreenController controller;
    private final BookSceneRenderer sceneRenderer = new BookSceneRenderer();
    private final PageCanvasRenderer pageRenderer = new PageCanvasRenderer();

    private BookLayout layout;
    private BookSpread displayedSpread;
    private int displayedSpreadIndex;
    private BookSpread targetSpread;
    private int targetSpreadIndex;
    private float textAlpha = 1.0f;
    private TextTransitionState textTransitionState = TextTransitionState.STABLE;
    private int initialTextDelayTicksRemaining;
    private boolean closedBookHovered;

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
        syncDisplayedSpreadFromController();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        controller.tick();
        updateTextTransition();
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

        sceneRenderer.renderBook(
                graphics,
                layout,
                displayedSpread,
                displayedSpreadIndex,
                controller.state(),
                controller.animationProgress(),
                mouseX,
                mouseY,
                textAlpha,
                partialTick,
                closedBookHovered
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

        graphics.drawCenteredString(font, title, width / 2, layout.bookY() - 24, 0xFFF2E3CB);
        if (controller.isArriving()) {
            graphics.drawString(
                    font,
                    Component.literal("Journal settles into place..."),
                    layout.bookX(),
                    layout.bookY() + layout.bookHeight() + 8,
                    0xFFD6C7AF
            );
        } else if (controller.isClosed()) {
            graphics.drawString(
                    font,
                    Component.literal(closedBookHovered ? "Click the book to open | Esc to leave" : "Hover and click the book to open | Esc to leave"),
                    layout.bookX(),
                    layout.bookY() + layout.bookHeight() + 8,
                    0xFFD6C7AF
            );
        } else {
            graphics.drawString(
                    font,
                    Component.literal("Left/Right or A/D to flip | Esc to close"),
                    layout.bookX(),
                    layout.bookY() + layout.bookHeight() + 8,
                    0xFFD6C7AF
            );
            graphics.drawString(font, pageRenderer.footerText(controller.spreadIndex()), layout.bookX(), layout.bookY() - 12, 0xFFD6C7AF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (controller.isClosing()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            if (controller.isOpenReadable()) {
                controller.nextSpread();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            if (controller.isOpenReadable()) {
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
            if (controller.beginOpening()) {
                syncDisplayedSpreadFromController();
            }
            return true;
        }
        if (!controller.isOpenReadable()) {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    private enum TextTransitionState {
        INITIAL_DELAY,
        STABLE,
        FADING_OUT,
        HIDDEN_DURING_FLIP,
        FADING_IN
    }
}
