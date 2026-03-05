package com.mongraphe.graphui.interaction;

import java.awt.event.*;

import com.mongraphe.graphui.app.InteractionService;

public final class SwingInputHandler implements
        MouseListener,
        MouseMotionListener,
        MouseWheelListener,
        KeyListener {

    private final InteractionService interaction;

    public SwingInputHandler(InteractionService interaction) {
        this.interaction = interaction;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        interaction.onMousePressed(
                e.getX(),
                e.getY(),
                e.getButton());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        interaction.onMouseDragged(
                e.getX(),
                e.getY(),
                e.getButton());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        interaction.onMouseReleased(
                e.getX(),
                e.getY(),
                e.getButton());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        interaction.onMouseWheel(
                e.getX(),
                e.getY(),
                e.getWheelRotation());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        interaction.onKeyPressed(
                e.getKeyCode(),
                e.isControlDown());
    }

    // Méthodes inutilisées
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}