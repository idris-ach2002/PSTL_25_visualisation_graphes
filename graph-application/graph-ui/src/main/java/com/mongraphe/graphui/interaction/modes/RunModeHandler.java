package com.mongraphe.graphui.interaction.modes;

import com.mongraphe.graphui.app.InteractionContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;

public final class RunModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(InteractionContext ctx, int sx, int sy, int button) {
        // Ne rien faire, le mode "Run" ne gère pas les interactions de la souris
    }

    @Override
    public void onMouseDragged(InteractionContext ctx, int sx, int sy, int button) {
        // Déplacement de la caméra pour faire du "panning"
        if (ctx.getGraphAdapter() != null) {
            ctx.getGraphAdapter().panCamera(sx, sy);
        }
    }

    @Override
    public void onMouseReleased(InteractionContext ctx, int sx, int sy, int button) {
        // Ne rien faire, le mode "Run" ne gère pas les interactions de la souris
    }

    @Override
    public void onMouseWheel(InteractionContext ctx, int sx, int sy, float rotation) {
        // Zoom de la caméra
        if (ctx.getGraphAdapter() != null) {
            ctx.getGraphAdapter().zoomCamera(sx, sy, rotation);
        }
    }

    @Override
    public void onKeyPressed(InteractionContext ctx, int keyCode, boolean ctrlDown) {
        // TODO : Remettre la possibilité de mettre en pause ou de relancer la simulation avec la barre d'espace
    }

}
