package com.mongraphe.graphui.view;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Gestion de la documentation utilisateur.
 *
 * <p>
 * La documentation est chargée depuis les ressources du projet et affichée
 * dans une fenêtre JavaFX via WebView. Si le système permet l'ouverture
 * dans un navigateur externe, un bouton dédié est proposé.
 * </p>
 *
 * <p>
 * Cette approche évite de dépendre de commandes système externes et reste
 * portable dans le cadre d'un projet Maven JavaFX non modulaire.
 * </p>
 */
public final class UserDocumentationService {

    private static final String DOC_INDEX = "/docs/index.html";
    private static final String DOC_STYLE = "/docs/style.css";

    private static Stage documentationStage;
    private static WebView webView;
    private static WebEngine webEngine;
    private static URL documentationUrl;

    private UserDocumentationService() {
    }

    public static void openDocumentationWindow() throws IOException {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                try {
                    openDocumentationWindow();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        if (documentationStage == null) {
            createDocumentationStage();
        }

        reloadDocumentation();
        documentationStage.show();
        documentationStage.toFront();
        documentationStage.requestFocus();
    }

    private static void createDocumentationStage() throws IOException {
        documentationUrl = UserDocumentationService.class.getResource(DOC_INDEX);
        if (documentationUrl == null) {
            throw new IOException("Ressource de documentation introuvable : " + DOC_INDEX);
        }

        webView = new WebView();
        webEngine = webView.getEngine();

        Button refreshButton = new Button("Actualiser");
        refreshButton.setOnAction(event -> reloadDocumentation());

        Button openBrowserButton = new Button("Ouvrir dans le navigateur");
        openBrowserButton.setDisable(!canOpenInBrowser());
        openBrowserButton.setOnAction(event -> {
            try {
                openDocumentationInBrowser();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button closeButton = new Button("Fermer");
        closeButton.setOnAction(event -> {
            if (documentationStage != null) {
                documentationStage.hide();
            }
        });

        ToolBar toolBar = new ToolBar(
                refreshButton,
                new Separator(),
                openBrowserButton,
                new Separator(),
                closeButton);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(webView);
        BorderPane.setMargin(webView, new Insets(0));

        Scene scene = new Scene(root, 1180, 820);

        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("F5"),
                thisSafeReload());
        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("Shortcut+R"),
                thisSafeReload());
        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("ESCAPE"),
                () -> {
                    if (documentationStage != null) {
                        documentationStage.hide();
                    }
                });

        documentationStage = new Stage();
        documentationStage.setTitle("Documentation utilisateur - MonGraphe");
        documentationStage.setMinWidth(900);
        documentationStage.setMinHeight(650);
        documentationStage.setScene(scene);
    }

    private static Runnable thisSafeReload() {
        return () -> {
            try {
                reloadDocumentation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private static void reloadDocumentation() {
        if (webEngine != null && documentationUrl != null) {
            webEngine.load(documentationUrl.toExternalForm());
        }
    }

    private static boolean canOpenInBrowser() {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        Desktop desktop = Desktop.getDesktop();
        return desktop.isSupported(Desktop.Action.BROWSE);
    }

    /**
     * Ouvre la documentation dans le navigateur système si cette action est
     * disponible. Pour éviter les problèmes liés aux chemins internes des
     * ressources, on exporte index.html et style.css dans un dossier temporaire.
     */
    public static void openDocumentationInBrowser() throws IOException {
        if (!canOpenInBrowser()) {
            throw new IOException("L'action d'ouverture dans le navigateur n'est pas disponible.");
        }

        Path docDir = exportDocumentationDirectory();
        Path indexFile = docDir.resolve("index.html");

        Desktop.getDesktop().browse(indexFile.toUri());
    }

    private static Path exportDocumentationDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("mongraphe-docs-");
        tempDir.toFile().deleteOnExit();

        copyResourceToDirectory(DOC_INDEX, tempDir.resolve("index.html"));
        copyResourceIfExists(DOC_STYLE, tempDir.resolve("style.css"));

        return tempDir;
    }

    private static void copyResourceToDirectory(String resourcePath, Path target) throws IOException {
        try (InputStream inputStream = UserDocumentationService.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Ressource introuvable : " + resourcePath);
            }
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            target.toFile().deleteOnExit();
        }
    }

    private static void copyResourceIfExists(String resourcePath, Path target) throws IOException {
        try (InputStream inputStream = UserDocumentationService.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return;
            }
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            target.toFile().deleteOnExit();
        }
    }
}