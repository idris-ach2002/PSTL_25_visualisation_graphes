package com.mongraphe.graphui.rendering;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EngineExecutor implements java.util.concurrent.Executor {

    private final ExecutorService engineThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "graph-engine-thread");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void execute(Runnable task) {
        engineThread.submit(task);
    }

    public void shutdown() {
        engineThread.shutdownNow();
    }
}