package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.client.render.BookSceneRenderer;
import net.zoogle.enchiridion.client.page.PageInteractiveNode;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

final class BookInputController {
    boolean keyPressed(BookScreenController controller, BookViewState state, int keyCode, Supplier<Boolean> superKeyPressed, Runnable closeNow) {
        if (controller.isClosing()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (controller.isClosed() || controller.isArriving()) {
                closeNow.run();
            } else {
                controller.beginUserClosing();
            }
            return true;
        }
        if (controller.isProjectionVisible()) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
                return controller.nextProjectionFocus();
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
                return controller.previousProjectionFocus();
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                controller.closeProjection();
                return true;
            }
            return superKeyPressed.get();
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
        return superKeyPressed.get();
    }

    boolean mouseClicked(BookScreenController controller, BookViewState state, BookLayout layout, BookSceneRenderer sceneRenderer, double mouseX, double mouseY, int button) {
        boolean hoveringClosedBook = layout != null && sceneRenderer.isClosedBookHovered(layout, (int) mouseX, (int) mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && controller.isClosed() && hoveringClosedBook) {
            state.resetInspectRotation();
            if (controller.beginOpening()) {
                state.syncDisplayedSpreadFromController(controller);
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && controller.isClosed() && hoveringClosedBook) {
            state.beginInspect(mouseX, mouseY);
            return true;
        }
        if (controller.isProjectionInteractive()) {
            ProjectionButtonHit projectionButton = state.hoveredProjectionButton();
            if (projectionButton != null && projectionButton.view().hasPrimaryAction()) {
                return projectionButton.view().primaryAction().onClick(controller.context(), state.displayedSpreadIndex(), button);
            }
            return false;
        }
        if (controller.isJournalReadable()) {
            PageInteractiveNode hoveredTarget = state.hoveredInteractiveTarget();
            if (hoveredTarget != null && hoveredTarget.enabled() && hoveredTarget.action().onClick(controller.context(), state.displayedSpreadIndex(), button)) {
                return true;
            }
        }
        return false;
    }

    boolean mouseDragged(BookScreenController controller, BookViewState state, int button, double mouseX, double mouseY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && controller.isClosed() && state.closedBookInspecting()) {
            state.updateInspectDrag(mouseX, mouseY);
            return true;
        }
        return false;
    }

    boolean mouseReleased(BookViewState state, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            state.endInspect();
            return true;
        }
        return false;
    }
}
