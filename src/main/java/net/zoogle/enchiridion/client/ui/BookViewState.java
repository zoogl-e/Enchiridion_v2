package net.zoogle.enchiridion.client.ui;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookSpread;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;

import java.util.ArrayList;
import java.util.List;

public final class BookViewState {
    private static final int INITIAL_TEXT_DELAY_TICKS = 32;
    private static final float TEXT_FADE_OUT_PER_TICK = 0.45f;
    private static final float TEXT_FADE_OUT_CLOSING_PER_TICK = 0.14f;
    private static final float TEXT_FADE_IN_PER_TICK = 0.18f;
    private static final float TEXT_ALPHA_EPSILON = 0.01f;
    private static final float INSPECT_YAW_PER_PIXEL = 0.45f;
    private static final float INSPECT_PITCH_PER_PIXEL = 0.35f;

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
    private List<PageInteractiveNode> resolvedInteractiveTargets = List.of();
    private PageInteractiveNode hoveredInteractiveTarget;
    private ProjectionButtonHit hoveredProjectionButton;
    private BookInteractionResolver.PageInteractionDebugState pageInteractionDebugState;
    private float inspectYaw;
    private float inspectPitch;
    private float currentProjectionFocusOffset;
    private double lastInspectMouseX;
    private double lastInspectMouseY;

    void initLayout(int width, int height) {
        layout = BookLayout.fromScreen(width, height);
    }

    public BookLayout layout() {
        return layout;
    }

    void updateProjectionFocusOffset(BookScreenController controller) {
        currentProjectionFocusOffset += (targetProjectionFocusOffset(controller) - currentProjectionFocusOffset) * 0.2f;
    }

    void updateTextTransition(BookScreenController controller) {
        if (displayedSpread == null) {
            syncDisplayedSpreadFromController(controller);
        }

        targetSpread = controller.currentSpread();
        targetSpreadIndex = controller.spreadIndex();

        boolean flipping = switch (controller.state()) {
            case FLIPPING_FRONT,
                    FLIPPING_FRONT_TO_ORIGIN,
                    FLIPPING_BACK,
                    FLIPPING_BACK_TO_ORIGIN,
                    FLIPPING_NEXT,
                    FLIPPING_PREV -> true;
            default -> false;
        };
        boolean arriving = controller.isArriving();
        boolean opening = controller.isOpening();
        boolean closed = controller.isClosed();
        boolean closing = controller.isClosing();
        if (closing) {
            textTransitionState = TextTransitionState.FADING_OUT;
            textAlpha = Math.max(0.0f, textAlpha - TEXT_FADE_OUT_CLOSING_PER_TICK);
            return;
        }
        if (arriving || closed) {
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

    void syncDisplayedSpreadFromController(BookScreenController controller) {
        displayedSpread = controller.currentSpread();
        displayedSpreadIndex = controller.spreadIndex();
        targetSpread = displayedSpread;
        targetSpreadIndex = displayedSpreadIndex;
        textAlpha = 0.0f;
        textTransitionState = controller.isClosed() ? TextTransitionState.FADING_OUT : TextTransitionState.INITIAL_DELAY;
        initialTextDelayTicksRemaining = INITIAL_TEXT_DELAY_TICKS;
    }

    List<net.minecraft.util.FormattedCharSequence> tooltipLines(Component title) {
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        lines.add(title.getVisualOrderText());
        lines.add(Component.literal("Click to open | Esc to leave").getVisualOrderText());
        return lines;
    }

    void resetInspectRotation() {
        inspectYaw = 0.0f;
        inspectPitch = 0.0f;
        closedBookInspecting = false;
    }

    void beginInspect(double mouseX, double mouseY) {
        closedBookInspecting = true;
        lastInspectMouseX = mouseX;
        lastInspectMouseY = mouseY;
    }

    void updateInspectDrag(double mouseX, double mouseY) {
        inspectYaw += (float) ((mouseX - lastInspectMouseX) * INSPECT_YAW_PER_PIXEL);
        inspectPitch -= (float) ((mouseY - lastInspectMouseY) * INSPECT_PITCH_PER_PIXEL);
        inspectYaw = wrapDegrees(inspectYaw);
        lastInspectMouseX = mouseX;
        lastInspectMouseY = mouseY;
    }

    void endInspect() {
        closedBookInspecting = false;
    }

    float projectionFocusOffset(BookScreenController controller) {
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

    private float targetProjectionFocusOffset(BookScreenController controller) {
        return projectionFocusOffset(controller);
    }

    BookPageSide focusedProjectionPageSide(BookScreenController controller) {
        if (!controller.isProjectionVisible()) {
            return null;
        }
        int pageIndex = controller.selectedProjectionPageIndex();
        if (pageIndex < 0) {
            return null;
        }
        return (pageIndex % 2) == 0 ? BookPageSide.LEFT : BookPageSide.RIGHT;
    }

    public BookSpread displayedSpread() {
        return displayedSpread;
    }

    public int displayedSpreadIndex() {
        return displayedSpreadIndex;
    }

    float textAlpha() {
        return textAlpha;
    }

    boolean closedBookHovered() {
        return closedBookHovered;
    }

    void setClosedBookHovered(boolean closedBookHovered) {
        this.closedBookHovered = closedBookHovered;
    }

    boolean closedBookInspecting() {
        return closedBookInspecting;
    }

    float inspectYaw() {
        return inspectYaw;
    }

    float inspectPitch() {
        return inspectPitch;
    }

    public float currentProjectionFocusOffset() {
        return currentProjectionFocusOffset;
    }

    public void refreshDisplayedSpread(BookScreenController controller) {
        displayedSpread = controller.currentSpread();
        displayedSpreadIndex = controller.spreadIndex();
        targetSpread = displayedSpread;
        targetSpreadIndex = displayedSpreadIndex;
        textAlpha = 1.0f;
        textTransitionState = TextTransitionState.STABLE;
    }

    void setInteraction(BookInteractionResolver.Resolution interaction) {
        hoveredInteractiveTarget = interaction.hoveredTarget();
        resolvedInteractiveTargets = interaction.targets();
        pageInteractionDebugState = interaction.debugState();
    }

    List<PageInteractiveNode> resolvedInteractiveTargets() {
        return resolvedInteractiveTargets;
    }

    PageInteractiveNode hoveredInteractiveTarget() {
        return hoveredInteractiveTarget;
    }

    BookInteractionResolver.PageInteractionDebugState pageInteractionDebugState() {
        return pageInteractionDebugState;
    }

    ProjectionButtonHit hoveredProjectionButton() {
        return hoveredProjectionButton;
    }

    void setHoveredProjectionButton(ProjectionButtonHit hoveredProjectionButton) {
        this.hoveredProjectionButton = hoveredProjectionButton;
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
}
