package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.rendering.GraphEngine;

public interface InteractionModeHandler {

    void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button);

    void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button);

    void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button);

    void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation);

    void onKeyPressed(CommandBus<GraphEngine> bus, int keyCode, boolean ctrlDown);
}