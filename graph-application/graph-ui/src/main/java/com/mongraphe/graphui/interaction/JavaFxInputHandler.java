package com.mongraphe.graphui.interaction;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;

/**
 * Adaptateur d'entrées JavaFX vers le service d'interaction métier.
 *
 * <p>Il remplace l'ancien gestionnaire AWT/Swing par des événements JavaFX
 * natifs. Un callback optionnel permet de demander une frame OpenGL après un
 * déplacement caméra, un zoom ou une action clavier, même lorsque le rendu est
 * cadencé en mode non continu.</p>
 */
public final class JavaFxInputHandler {

    private final InteractionService interaction;
    private final Runnable repaintCallback;
    private int activeButton;

    public JavaFxInputHandler(InteractionService interaction) {
        this(interaction, null);
    }

    public JavaFxInputHandler(InteractionService interaction, Runnable repaintCallback) {
        this.interaction = interaction;
        this.repaintCallback = repaintCallback;
    }

    public void attach(Node node) {
        node.setFocusTraversable(true);

        node.setOnMousePressed(event -> {
            activeButton = toAwtButton(event.getButton());
            node.requestFocus();
            interaction.onMousePressed((int) event.getX(), (int) event.getY(), activeButton);
            requestRepaint();
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            interaction.onMouseDragged((int) event.getX(), (int) event.getY(), activeButton);
            requestRepaint();
            event.consume();
        });

        node.setOnMouseReleased(event -> {
            int button = activeButton == 0 ? toAwtButton(event.getButton()) : activeButton;
            interaction.onMouseReleased((int) event.getX(), (int) event.getY(), button);
            activeButton = 0;
            requestRepaint();
            event.consume();
        });

        node.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            if (Math.abs(deltaY) > 1e-9) {
                interaction.onMouseWheel((int) event.getX(), (int) event.getY(), deltaY < 0 ? 1f : -1f);
                requestRepaint();
            }
            event.consume();
        });

        node.setOnKeyPressed(event -> {
            if (handleCommonShortcuts(event)) {
                requestRepaint();
                event.consume();
                return;
            }
            interaction.onKeyPressed(toAwtKeyCode(event.getCode()), event.isControlDown() || event.isShortcutDown());
            requestRepaint();
            event.consume();
        });
    }

    public void detach(Node node) {
        node.setOnMousePressed(null);
        node.setOnMouseDragged(null);
        node.setOnMouseReleased(null);
        node.setOnScroll(null);
        node.setOnKeyPressed(null);
    }

    private void requestRepaint() {
        if (repaintCallback != null) {
            repaintCallback.run();
        }
    }

    private boolean handleCommonShortcuts(javafx.scene.input.KeyEvent event) {
        boolean ctrl = event.isControlDown() || event.isShortcutDown();
        if (!ctrl) {
            return false;
        }

        KeyCode code = event.getCode();
        if (code == KeyCode.Z) {
            if (event.isShiftDown()) {
                interaction.redo();
            } else {
                interaction.undo();
            }
            return true;
        }
        if (code == KeyCode.Y) {
            interaction.redo();
            return true;
        }
        if (code == KeyCode.EQUALS || code == KeyCode.ADD || code == KeyCode.PLUS) {
            interaction.zoomIn();
            return true;
        }
        if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            interaction.zoomOut();
            return true;
        }
        if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            interaction.resetView();
            return true;
        }
        return false;
    }

    private int toAwtButton(javafx.scene.input.MouseButton button) {
        return switch (button) {
            case PRIMARY -> MouseEvent.BUTTON1;
            case MIDDLE -> MouseEvent.BUTTON2;
            case SECONDARY -> MouseEvent.BUTTON3;
            default -> 0;
        };
    }

    private int toAwtKeyCode(KeyCode code) {
        return switch (code) {
            case DELETE -> KeyEvent.VK_DELETE;
            case BACK_SPACE -> KeyEvent.VK_BACK_SPACE;
            case SPACE -> KeyEvent.VK_SPACE;
            default -> code.getCode();
        };
    }
}
