package com.mongraphe.graphui.interaction;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.mongraphe.graphui.app.InteractionService;

public final class OpenGLInputHandler
        implements MouseListener, KeyListener {

    private final InteractionService interaction;

    public OpenGLInputHandler(InteractionService interaction) {
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
    public void mouseWheelMoved(MouseEvent e) {
        interaction.onMouseWheel(
                e.getX(),
                e.getY(),
                e.getRotation()[1]);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        interaction.onKeyPressed(
                e.getKeyCode(),
                e.isControlDown());
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }
}