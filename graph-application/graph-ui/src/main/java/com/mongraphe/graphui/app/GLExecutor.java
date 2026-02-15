package com.mongraphe.graphui.app;

import java.util.concurrent.Executor;

import com.jogamp.newt.opengl.GLWindow;

public final class GLExecutor implements Executor {

    private final GLWindow window;

    public GLExecutor(GLWindow window) {
        this.window = window;
    }

    @Override
    public void execute(Runnable task) {
        if (window == null) return;

        window.invoke(false, drawable -> {
            task.run();
            return true;
        });
    }
}