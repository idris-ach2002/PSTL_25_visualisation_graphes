package com.mongraphe.graphui.interaction;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public final class SwingInputHandler implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private final InteractionService interaction;
    private int activeButton;

    public SwingInputHandler(InteractionService interaction) {
        this.interaction = interaction;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        activeButton = e.getButton();
        if (e.getSource() instanceof Component component) {
            component.requestFocusInWindow();
        }
        interaction.onMousePressed(e.getX(), e.getY(), activeButton);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        interaction.onMouseDragged(e.getX(), e.getY(), activeButton);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        interaction.onMouseReleased(e.getX(), e.getY(), activeButton == 0 ? e.getButton() : activeButton);
        activeButton = 0;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        interaction.onMouseWheel(e.getX(), e.getY(), e.getWheelRotation());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        interaction.onKeyPressed(e.getKeyCode(), e.isControlDown());
    }

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
