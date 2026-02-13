package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.app.InteractionContext;

public interface InteractionModeHandler {

    void onMousePressed(InteractionContext ctx, int sx, int sy, int button);

    void onMouseDragged(InteractionContext ctx, int sx, int sy, int button);

    void onMouseReleased(InteractionContext ctx, int sx, int sy, int button);

    void onMouseWheel(InteractionContext ctx, int sx, int sy, float rotation);

    void onKeyPressed(InteractionContext ctx, int keyCode, boolean ctrlDown);
}