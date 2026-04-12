package com.mongraphe.graphui.view;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class SvgResolutionDialog extends Dialog<Double> {

    public SvgResolutionDialog(double defaultSize) {
        setTitle("Résolution SVG");
        setHeaderText("Choisissez la taille de l'image (en pixels)");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        TextField sizeField = new TextField(String.valueOf((int) defaultSize));
        sizeField.setPromptText("ex: 2000");
        GridPane.setHgrow(sizeField, Priority.ALWAYS);
        grid.add(new Label("Largeur / Hauteur (px) :"), 0, 0);
        grid.add(sizeField, 1, 0);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    double val = Double.parseDouble(sizeField.getText());
                    return Math.max(100, val);
                } catch (NumberFormatException e) {
                    return defaultSize;
                }
            }
            return null;
        });
    }
}