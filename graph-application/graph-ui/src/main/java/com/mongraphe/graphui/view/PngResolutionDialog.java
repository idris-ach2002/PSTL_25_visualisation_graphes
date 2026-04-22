package com.mongraphe.graphui.view;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class PngResolutionDialog extends Dialog<int[]> {

    private static final int MIN_SIZE = 1;

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

        double aspectRatio = defaultHeight <= 0 ? 1.0 : (double) defaultWidth / (double) defaultHeight;

        CheckBox keepRatioBox = new CheckBox("Conserver le ratio");
        keepRatioBox.setSelected(true);

        Label ratioLabel = new Label(String.format("Ratio courant : %.3f", aspectRatio));

        final boolean[] internalUpdate = { false };

        widthField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (internalUpdate[0] || !keepRatioBox.isSelected()) {
                return;
            }

            Integer width = parsePositiveInt(newValue);
            if (width == null) {
                return;
            }

            int adjustedHeight = Math.max(MIN_SIZE, (int) Math.round(width / aspectRatio));
            internalUpdate[0] = true;
            heightField.setText(String.valueOf(adjustedHeight));
            internalUpdate[0] = false;
        });

        heightField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (internalUpdate[0] || !keepRatioBox.isSelected()) {
                return;
            }

            Integer height = parsePositiveInt(newValue);
            if (height == null) {
                return;
            }

            int adjustedWidth = Math.max(MIN_SIZE, (int) Math.round(height * aspectRatio));
            internalUpdate[0] = true;
            widthField.setText(String.valueOf(adjustedWidth));
            internalUpdate[0] = false;
        });

        keepRatioBox.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!selected) {
                return;
            }

            Integer width = parsePositiveInt(widthField.getText());
            if (width == null) {
                width = Math.max(MIN_SIZE, defaultWidth);
                widthField.setText(String.valueOf(width));
            }

            int adjustedHeight = Math.max(MIN_SIZE, (int) Math.round(width / aspectRatio));
            internalUpdate[0] = true;
            heightField.setText(String.valueOf(adjustedHeight));
            internalUpdate[0] = false;
        });

        grid.add(new Label("Largeur (px) :"), 0, 0);
        grid.add(widthField, 1, 0);
        grid.add(new Label("Hauteur (px) :"), 0, 1);
        grid.add(heightField, 1, 1);
        grid.add(keepRatioBox, 0, 2, 2, 1);
        grid.add(ratioLabel, 0, 3, 2, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton != okButtonType) {
                return null;
            }

            Integer width = parsePositiveInt(widthField.getText().trim());
            Integer height = parsePositiveInt(heightField.getText().trim());

            if (width == null || height == null) {
                showInvalidValuesAlert();
                return null;
            }

            if (keepRatioBox.isSelected()) {
                height = Math.max(MIN_SIZE, (int) Math.round(width / aspectRatio));
            }

            return new int[] { width, height };
        });
    }

    private static Integer parsePositiveInt(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return value >= MIN_SIZE ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showInvalidValuesAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Valeurs invalides");
        alert.setHeaderText(null);
        alert.setContentText("Veuillez entrer des entiers positifs.");
        alert.showAndWait();
    }
}