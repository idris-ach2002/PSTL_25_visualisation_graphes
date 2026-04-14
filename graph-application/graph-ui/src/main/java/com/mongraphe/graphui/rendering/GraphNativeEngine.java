package com.mongraphe.graphui.rendering;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mongraphe.graphui.model.EdgeC;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.model.Vertex;

/**
 * Pont JNI (Java Native Interface) assurant la communication avec le moteur de
 * calcul en C.
 *
 * <p>
 * Cette classe est responsable du chargement de la bibliothèque native
 * ({@code .so}, {@code .dll} ou {@code .dylib})
 * et de l'exposition des méthodes de calcul intensif (layout, simulation de
 * forces, clustering).
 * </p>
 *
 * <h2>Optimisation de la Mémoire</h2>
 * <p>
 * Pour éviter les surcoûts liés au passage d'objets entre Java et C, cette
 * classe utilise un
 * <b>Direct ByteBuffer</b> ({@code sharedPositionsBuffer}). Ce tampon mémoire
 * est alloué
 * en dehors du tas Java (Heap), permettant au code natif d'y écrire directement
 * les nouvelles
 * positions des sommets sans copie intermédiaire.
 * </p>
 *
 * <h2>Chargement Dynamique</h2>
 * <p>
 * Contrairement à un chargement statique classique, le moteur cherche la
 * bibliothèque dans
 * plusieurs répertoires (chemins relatifs, {@code java.library.path}, variables
 * d'environnement)
 * pour faciliter le déploiement sur différents environnements de développement.
 * </p>
 */
public final class GraphNativeEngine {

    private static final String LIB_BASENAME = "native";

    private static volatile boolean nativeLoaded;
    private static volatile String loadedFrom;
    private static volatile Throwable nativeLoadError;

    private Metadata metadata;
    private Metadata initMetadata;

    public GraphNativeEngine() {
        ensureNativeLoaded();
    }

    /**
     * * Buffer de communication direct.
     * [x0, y0, x1, y1, ...] en float 32 bits.
     */
    public ByteBuffer sharedPositionsBuffer;
    private int sharedBufferCapacity; // nombre de floats (2 * num_nodes)

    /**
     * Alloue un espace mémoire "Direct" partagé avec le moteur C.
     * 
     * @param numNodes Nombre de sommets du graphe.
     */
    public void initSharedPositionsBuffer(int numNodes) {
        int floatCount = numNodes * 2;
        sharedPositionsBuffer = ByteBuffer.allocateDirect(floatCount * Float.BYTES);
        // Important : aligner l'ordre des octets sur celui du processeur (Little/Big
        // Endian)
        sharedPositionsBuffer.order(ByteOrder.nativeOrder());
        sharedBufferCapacity = floatCount;
    }

    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public static String getLoadedFrom() {
        return loadedFrom;
    }

    public static String getNativeLoadErrorMessage() {
        if (nativeLoadError == null) {
            return null;
        }
        String message = nativeLoadError.getMessage();
        return message == null || message.isBlank() ? nativeLoadError.toString() : message;
    }

    /**
     * Tente de charger la bibliothèque native selon une liste de candidats.
     * 
     * @throws UnsatisfiedLinkError si la bibliothèque reste introuvable.
     */
    public static synchronized void ensureNativeLoaded() {
        if (nativeLoaded) {
            return;
        }
        if (nativeLoadError != null) {
            throw nativeLoadException(nativeLoadError);
        }

        Throwable lastError = null;

        for (Path candidate : nativeCandidates()) {
            try {
                if (candidate != null && Files.isRegularFile(candidate)) {
                    System.load(candidate.toAbsolutePath().normalize().toString());
                    nativeLoaded = true;
                    loadedFrom = candidate.toAbsolutePath().normalize().toString();
                    nativeLoadError = null;
                    return;
                }
            } catch (Throwable t) {
                lastError = t;
            }
        }

        try {
            System.loadLibrary(LIB_BASENAME);
            nativeLoaded = true;
            loadedFrom = "java.library.path";
            nativeLoadError = null;
            return;
        } catch (Throwable t) {
            lastError = t;
        }

        nativeLoadError = lastError == null ? new UnsatisfiedLinkError("Impossible de charger la bibliothèque native")
                : lastError;
        throw nativeLoadException(nativeLoadError);
    }

    /** Génère une erreur détaillée en cas d'échec de chargement. */
    private static UnsatisfiedLinkError nativeLoadException(Throwable cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("Impossible de charger la bibliothèque native 'lib")
                .append(LIB_BASENAME)
                .append(nativeFileExtension())
                .append("'.\n")
                .append("Chemins essayés :\n");
        for (Path candidate : nativeCandidates()) {
            sb.append(" - ").append(candidate.toAbsolutePath().normalize()).append('\n');
        }
        String libPath = System.getProperty("java.library.path", "");
        if (!libPath.isBlank()) {
            sb.append("java.library.path = ").append(libPath).append('\n');
        }
        sb.append("Construis d'abord la librairie avec : cd graph-native && make");

        UnsatisfiedLinkError err = new UnsatisfiedLinkError(sb.toString());
        if (cause != null) {
            err.initCause(cause);
        }
        return err;
    }

    /** Liste les chemins potentiels où la librairie pourrait se trouver. */
    private static List<Path> nativeCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();
        String ext = nativeFileExtension();
        Path cwd = Paths.get("").toAbsolutePath().normalize();

        addIfPresent(candidates, System.getProperty("mongraphe.native.path"));
        addIfPresent(candidates, System.getProperty("graph.native.path"));

        candidates.add(cwd.resolve("../graph-native/out/lib" + LIB_BASENAME + ext));
        candidates.add(cwd.resolve("graph-native/out/lib" + LIB_BASENAME + ext));
        candidates.add(cwd.resolve("../graph-native/out/linux/lib" + LIB_BASENAME + ext));
        candidates.add(cwd.resolve("graph-native/out/linux/lib" + LIB_BASENAME + ext));
        candidates.add(cwd.resolve("out/lib" + LIB_BASENAME + ext));
        candidates.add(cwd.resolve("out/linux/lib" + LIB_BASENAME + ext));

        String libraryPath = System.getProperty("java.library.path", "");
        if (!libraryPath.isBlank()) {
            for (String token : libraryPath.split(File.pathSeparator)) {
                if (token == null || token.isBlank()) {
                    continue;
                }
                Path dir = Paths.get(token.trim());
                candidates.add(dir.resolve("lib" + LIB_BASENAME + ext));
                candidates.add(dir.resolve(LIB_BASENAME + ext));
            }
        }

        return new ArrayList<>(candidates);
    }

    private static void addIfPresent(Set<Path> candidates, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }
        candidates.add(Paths.get(rawPath.trim()));
    }

    /** Identifie l'extension de fichier selon l'OS. */
    private static String nativeFileExtension() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return ".dll";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return ".dylib";
        }
        return ".so";
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Metadata getInitMetadata() {
        return initMetadata;
    }

    /**
     * * Nettoyage de la mémoire native.
     * Indispensable pour éviter les fuites de mémoire (le GC Java ne voit pas la
     * mémoire C).
     */
    public void freeAllocatedMemory() {
        nativeFreeAllocatedMemory(); // appel natif
        sharedPositionsBuffer = null; // éviter les références pendantes
    }

    // --- Méthodes Natives (Implémentées en C) ---

    public synchronized native Metadata initializeDot(String filepath, int modeCommunity);

    public synchronized native Metadata initializeGraph(int modeSimilitude, int modeCommunity, double threshold,
            double antiThreshold);

    public synchronized native double[][] startsProgram(String filename);

    public synchronized native Metadata computeThreshold(int modeSimilitude, int edgeFactor);

    public synchronized native void setDimension(double width, double height);

    /** Met à jour les positions dans le buffer partagé. Retourne true si succès. */
    public synchronized native boolean updatePositions(ByteBuffer positionsBuffer);

    public synchronized native Vertex[] getPositions();

    public synchronized native void setNodePosition(int index, double x, double y);

    public synchronized native EdgeC[] getEdges();

    public synchronized native int[] getCommunities();

    public synchronized native float[][] getCommunityColors();

    public synchronized native void setFriction(double friction);

    public synchronized native void setModeRepulsion(int mode);

    public synchronized native void setAntiRepulsion(double antiedgeRepulsion);

    public synchronized native void setAttractionCoeff(double attractionCoeff);

    public synchronized native void setSeuilRep(double seuilrep);

    public synchronized native void setAmortissement(double amortissement);

    public synchronized native void setSpatialCells(int cells);

    public synchronized native void setEpsilon(double eps);

    public synchronized native void setKmeansMode(boolean md);

    public synchronized native void setInitialNodeSize(double size);

    public synchronized native void setDegreeScaleFactor(double factor);

    public synchronized native void deleteNode(int index);

    public synchronized native void restoreNode(int index);

    /** Libère les structures de données (listes d'adjacence, etc.) côté C. */
    public synchronized native void nativeFreeAllocatedMemory();

    public synchronized native double[] getDimensions();

    public synchronized native void setRepulsionCoeff(double coeff);

    public synchronized native void loadCsvDataOnly(String filepath);
}