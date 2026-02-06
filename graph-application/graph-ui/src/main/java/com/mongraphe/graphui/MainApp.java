package com.mongraphe.graphui;

import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

	public static void runMake(String directory) {
		try {
			File dir = new File(directory);
			System.out.println("Exécution de make dans : " + dir.getAbsolutePath());

			// Vérifier si un Makefile est présent
			File makefile = new File(dir, "Makefile");
			if (!makefile.exists()) {
				System.err.println("Avertissement : Aucun Makefile trouvé dans " + dir.getAbsolutePath());
			}

			// Initialisation du ProcessBuilder
			ProcessBuilder builder = new ProcessBuilder("make");

			// Définir le répertoire où exécuter la commande
			builder.directory(dir);

			// Rediriger les flux de sortie et d'erreur pour voir les logs
			builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			builder.redirectError(ProcessBuilder.Redirect.INHERIT);

			// Lancer le processus
			Process process = builder.start();

			// Attendre la fin de l'exécution
			int exitCode = process.waitFor();
			System.out.println("Make terminé avec le code : " + exitCode);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
			Parent root = loader.load();
			primaryStage.setTitle("Accueil");
			primaryStage.setScene(new Scene(root, 1000, 700));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
