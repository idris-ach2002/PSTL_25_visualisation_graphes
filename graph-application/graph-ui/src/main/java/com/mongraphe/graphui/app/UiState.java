package com.mongraphe.graphui.app;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class UiState {
    private final StringProperty statusText = new SimpleStringProperty("Prêt");
    private final BooleanProperty simulationRunning = new SimpleBooleanProperty(false);

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

    public void setStatus(String status) {
        final String safeStatus = status == null ? "" : status;
        if (!Objects.equals(statusText.get(), safeStatus)) {
            statusText.set(safeStatus);
        }
    }

    public void setRunning(boolean running) {
        if (simulationRunning.get() != running) {
            simulationRunning.set(running);
        }
    }
}
