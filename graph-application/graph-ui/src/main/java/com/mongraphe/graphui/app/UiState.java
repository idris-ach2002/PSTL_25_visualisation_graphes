package com.mongraphe.graphui.app;

import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public final class UiState {
    private final StringProperty statusText = new SimpleStringProperty("Prêt");
    private final BooleanProperty simulationRunning = new SimpleBooleanProperty(false);

    private volatile Color backgroundColor = Color.WHITE;

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public BooleanProperty simulationRunningProperty() {
        return simulationRunning;
    }

    public String getStatus() {
        return statusText.get();
    }

    public boolean isRunning() {
        return simulationRunning.get();
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setStatus(String status) {
        final String safeStatus = status == null ? "" : status;
        runOnFxThread(() -> {
            if (!Objects.equals(statusText.get(), safeStatus)) {
                statusText.set(safeStatus);
            }
        });
    }

    public void setRunning(boolean running) {
        runOnFxThread(() -> {
            if (simulationRunning.get() != running) {
                simulationRunning.set(running);
            }
        });
    }

    public void setBackgroundColor(Color color) {
        final Color safeColor = color == null ? Color.WHITE : color;
        runOnFxThread(() -> backgroundColor = safeColor);
    }

    private void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
