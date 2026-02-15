package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.app.ApplicationContext;

public interface InteractionModeHandler {

    void onMousePressed(ApplicationContext ctx, int sx, int sy, int button);

    void onMouseDragged(ApplicationContext ctx, int sx, int sy, int button);

    void onMouseReleased(ApplicationContext ctx, int sx, int sy, int button);

    void onMouseWheel(ApplicationContext ctx, int sx, int sy, float rotation);

    void onKeyPressed(ApplicationContext ctx, int keyCode, boolean ctrlDown);
}