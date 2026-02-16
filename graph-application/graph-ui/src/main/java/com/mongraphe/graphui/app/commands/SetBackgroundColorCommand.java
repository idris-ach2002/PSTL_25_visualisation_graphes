package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.UndoableGraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetBackgroundColorCommand implements UndoableGraphCommand<GraphEngine> {

    private final float r, g, b, a;
    private float prevR, prevG, prevB, prevA;

    public SetBackgroundColorCommand(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void execute(GraphEngine engine) {
        // Sauvegarde de l'ancienne couleur pour pouvoir undo
        prevR = engine.getBackgroundColorR();
        prevG = engine.getBackgroundColorG();
        prevB = engine.getBackgroundColorB();
        prevA = engine.getBackgroundColorA();

        engine.setBackgroundColor(r, g, b, a);
    }

    @Override
    public void undo(GraphEngine engine) {
        engine.setBackgroundColor(prevR, prevG, prevB, prevA);
    }
}