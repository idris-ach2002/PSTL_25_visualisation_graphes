package com.mongraphe.graphui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class PngResolutionDialog extends Dialog<int[]> {

    public PngResolutionDialog(int defaultWidth, int defaultHeight) {
        setTitle("Résolution PNG");
        setHeaderText("Définissez les dimensions de l'image");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField widthField = new TextField(String.valueOf(defaultWidth));
        widthField.setPromptText("Largeur");
        TextField heightField = new TextField(String.valueOf(defaultHeight));
        heightField.setPromptText("Hauteur");

        grid.add(new Label("Largeur (px) :"), 0, 0);
        grid.add(widthField, 1, 0);
        grid.add(new Label("Hauteur (px) :"), 0, 1);
        grid.add(heightField, 1, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    int w = Integer.parseInt(widthField.getText().trim());
                    int h = Integer.parseInt(heightField.getText().trim());
                    if (w < 1 || h < 1) {
                        throw new NumberFormatException();
                    }
                    return new int[] { w, h };
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Valeurs invalides");
                    alert.setHeaderText(null);
                    alert.setContentText("Veuillez entrer des entiers positifs.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
    }
}