package com.mongraphe.graphui.model;

import java.io.File;
import java.util.Objects;

/**
 * Représente un "projet" chargé dans l'application : source (CSV/DOT),
 * paramètres d'import et quelques métadonnées UI.
 *
 * Objectif : éviter que le Controller JavaFX serve de poubelle globale.
 */
public final class GraphProject {

    public enum SourceType { CSV, DOT }

    private final File sourceFile;
    private final SourceType sourceType;

    public GraphProject(File sourceFile, SourceType sourceType) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
    }

    public File sourceFile() { return sourceFile; }
    public SourceType sourceType() { return sourceType; }

    @Override
    public String toString() {
        return sourceType + ":" + sourceFile.getName();
    }
}
