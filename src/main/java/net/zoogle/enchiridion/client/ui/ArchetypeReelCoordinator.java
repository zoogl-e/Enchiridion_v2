package net.zoogle.enchiridion.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.client.levelrpg.archetype.ArchetypeReelState;
import net.zoogle.enchiridion.client.levelrpg.archetype.JournalArchetypeChoice;
import net.zoogle.enchiridion.client.levelrpg.archetype.ReelFlowController;
import net.zoogle.enchiridion.client.levelrpg.bridge.LevelRpgArchetypeBindGateway;
import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Coordinator for the Archetype Reel state, logic, and lifecycle.
 * Manages the reel flow, binding gateway, and overlay rendering.
 */
public final class ArchetypeReelCoordinator {
    private final ArchetypeReelState reelState = new ArchetypeReelState();
    private final ReelFlowController flowController = new ReelFlowController();
    private final LevelRpgArchetypeBindGateway bindGateway = new LevelRpgArchetypeBindGateway();
    private final ArchetypeReelOverlayRenderer overlayRenderer = new ArchetypeReelOverlayRenderer();
    private ReelFlowController.FlowPhase previousPhase = ReelFlowController.FlowPhase.INACTIVE;

    public void tick(
            BookScreenController controller,
            BookViewState viewState,
            BookCinematicManager cinematicManager,
            int width,
            int height
    ) {
        flowController.tick(
                reelState,
                controller.frontCoverCardState().boundArchetypeId(),
                controller.context(),
                bindGateway
        );

        ReelFlowController.FlowPhase currentPhase = flowController.phase();
        if (currentPhase != previousPhase) {
            cinematicManager.onReelPhaseChanged(previousPhase, currentPhase, width, height);
            previousPhase = currentPhase;
        }

        if (currentPhase == ReelFlowController.FlowPhase.COMPLETED) {
            reelState.close();
            flowController.onClosed();
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        }
    }

    public void renderDropTargetHint(GuiGraphics graphics, BookSceneRenderer.ScreenRect dropTarget) {
        if (shouldRender()) {
            overlayRenderer.renderDropTargetHint(graphics, reelState, dropTarget);
        }
    }

    public void renderTopmost(
            GuiGraphics graphics,
            Font font,
            BookViewState viewState,
            InfoPanelLayoutService.PanelRect panelRect
    ) {
        if (shouldRender()) {
            overlayRenderer.renderTopmost(graphics, font, reelState, flowController, viewState, panelRect);
        }
    }

    public boolean open(List<JournalArchetypeChoice> choices, ResourceLocation boundId, ResourceLocation selectedId) {
        boolean opened = reelState.open(choices, boundId, selectedId);
        if (opened) {
            flowController.onOpened();
        }
        return opened;
    }

    public void close() {
        reelState.close();
        flowController.onClosed();
    }

    public boolean beginBinding(BookScreenController controller, String focusId, BookViewState viewState) {
        boolean begun = controller.beginArchetypeBinding(focusId);
        if (begun) {
            reelState.close();
            flowController.onClosed();
            controller.reloadSpread();
            viewState.refreshDisplayedSpread(controller);
        }
        return begun;
    }

    public boolean isActive() {
        return reelState.active();
    }

    public boolean shouldRender() {
        return flowController.shouldRenderReel();
    }

    public ArchetypeReelState reelState() {
        return reelState;
    }

    public ReelFlowController flowController() {
        return flowController;
    }

    public LevelRpgArchetypeBindGateway bindGateway() {
        return bindGateway;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (flowController.canCancel()) {
                close();
                return true;
            }
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            reelState.move(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            reelState.move(1);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, BookViewState viewState, BookSceneRenderer sceneRenderer) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (flowController.canCancel()) {
                reelState.endDrag();
                close();
                return true;
            }
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        Integer hoveredIndex = sceneRenderer.hoveredArchetypeReelCardIndex(viewState.layout(), reelState, mouseX, mouseY);
        int dragIndex = hoveredIndex == null ? reelState.focusedIndex() : hoveredIndex;
        boolean started = reelState.beginDrag(dragIndex);
        if (started) {
            reelState.setDragMouse((float) mouseX, (float) mouseY);
        }
        return started;
    }

    public boolean mouseDragged(double mouseX, double mouseY, boolean hoveredOverTarget) {
        if (!reelState.dragging()) {
            return false;
        }
        reelState.setDragMouse((float) mouseX, (float) mouseY);
        reelState.setDropTargetHovered(hoveredOverTarget);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, boolean droppedOnTarget) {
        if (!reelState.dragging()) {
            return false;
        }
        JournalArchetypeChoice droppedChoice = reelState.draggedChoice();
        reelState.endDrag();
        if (!droppedOnTarget) {
            return true;
        }
        if (droppedChoice != null) {
            return flowController.confirmChoice(reelState, droppedChoice);
        }
        reelState.snapToNearestFocus();
        return confirmFocused();
    }

    private boolean confirmFocused() {
        if (reelState.phase() != ArchetypeReelState.Phase.IDLE) {
            return false;
        }
        reelState.endDrag();
        JournalArchetypeChoice focusedChoice = reelState.focusedChoice();
        if (focusedChoice == null) {
            return false;
        }
        return flowController.confirmFocused(reelState);
    }

    public void updateHover(double mouseX, double mouseY, BookViewState viewState, BookSceneRenderer sceneRenderer) {
        Integer hoveredIndex = sceneRenderer.hoveredArchetypeReelCardIndex(viewState.layout(), reelState, mouseX, mouseY);
        reelState.setHoveredIndex(hoveredIndex);
    }

    public ArchetypeReelState reelStateForPresentation() {
        return shouldRender() ? reelState : null;
    }
}
