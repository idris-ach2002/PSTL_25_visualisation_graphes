package com.mongraphe.graphui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;

public final class ViewSwitcherController {

    @FXML
    private ToggleGroup viewToggleGroup;
    @FXML
    private Pane overviewPane;
    @FXML
    private Pane dataPane;
    @FXML
    private Pane previewPane;

    @FXML
    private void handleViewChange() {
        Toggle selected = viewToggleGroup.getSelectedToggle();
        if (selected == null)
            return;

        String view = String.valueOf(selected.getUserData());

        overviewPane.setVisible("overview".equals(view));
        dataPane.setVisible("data".equals(view));
        previewPane.setVisible("preview".equals(view));
    }
}