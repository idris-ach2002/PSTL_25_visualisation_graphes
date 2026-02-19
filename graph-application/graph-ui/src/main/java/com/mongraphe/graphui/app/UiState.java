package com.mongraphe.graphui.app;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

/**
 * Etat UI centralisé (bindable). Permet de rendre l'interface plus "réactive"
 * sans saupoudrer des setText() partout.
 */
public final class UiState {

    private final StringProperty statusText = new SimpleStringProperty("Prêt");
    private final BooleanProperty simulationRunning = new SimpleBooleanProperty(true);
    private final ObjectProperty<Color> backgroundColor = new SimpleObjectProperty<>(Color.WHITE);

    public StringProperty statusTextProperty() { return statusText; }
    public BooleanProperty simulationRunningProperty() { return simulationRunning; }
    public ObjectProperty<Color> backgroundColorProperty() { return backgroundColor; }

    public void setStatus(String txt) { statusText.set(txt); }
    public void setRunning(boolean running) { simulationRunning.set(running); }
    public void setBackground(Color c) { backgroundColor.set(c); }
}
