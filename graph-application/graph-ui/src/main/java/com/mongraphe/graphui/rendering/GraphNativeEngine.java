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
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.model.Vertex;

/**
 * Pont JNI avec le moteur natif C.
 *
 * La bibliothèque n'est pas chargée dans un bloc statique pour éviter de
 * faire échouer brutalement tout le chargement FXML avec un
 * ExceptionInInitializerError. On charge explicitement la librairie au moment
 * de construire le moteur, avec plusieurs chemins de repli et un message clair.
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

    public ByteBuffer sharedPositionsBuffer;
    private int sharedBufferCapacity; // nombre de floats (2 * num_nodes)

    public void initSharedPositionsBuffer(int numNodes) {
        int floatCount = numNodes * 2;
        sharedPositionsBuffer = ByteBuffer.allocateDirect(floatCount * Float.BYTES);
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

    public double[][] initGraphCsv(String path, GraphData.SimilitudeMode sim, GraphData.NodeCommunity community) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("initGraphCsv: chemin du fichier non spécifié.");
        }

        double[][] csvData = startsProgram(path);
        int modeSimilitude = getModeSimilitude(sim);
        initMetadata = computeThreshold(modeSimilitude, 10);
        if (initMetadata == null) {
            throw new IllegalStateException("initGraphCsv: impossible de calculer les seuils.");
        }

        double threshold = initMetadata.getEdgeThreshold();
        double antiThreshold = initMetadata.getAntiThreshold();
        metadata = initializeGraph(getModeCommunity(community), threshold, antiThreshold);
        return csvData;
    }

    public void initGraphDot(String path, GraphData.NodeCommunity community) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("initGraphDot: chemin du fichier non spécifié.");
        }
        metadata = initializeDot(path, getModeCommunity(community));
        initMetadata = null;
    }

    private int getModeCommunity(GraphData.NodeCommunity community) {
        if (community == null) {
            throw new IllegalArgumentException("Le mode de communauté ne peut pas être nul.");
        }
        return switch (community) {
            case LOUVAIN -> 0;
            case LOUVAIN_PAR_COMPOSANTE -> 1;
            case LEIDEN -> 2;
            case LEIDEN_CPM -> 3;
            case COULEURS_SPECIALES -> 4;
        };
    }

    private int getModeSimilitude(GraphData.SimilitudeMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Le mode de similarité ne peut pas être nul.");
        }
        return switch (mode) {
            case CORRELATION -> 0;
            case DISTANCE_COSINE -> 1;
            case DISTANCE_EUCLIDIENNE -> 2;
            case NORME_L1 -> 3;
            case NORME_LINF -> 4;
            case KL_DIVERGENCE -> 5;
        };
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Metadata getInitMetadata() {
        return initMetadata;
    }

    public void freeAllocatedMemory() {
        nativeFreeAllocatedMemory(); // appel natif
        sharedPositionsBuffer = null; // éviter les références pendantes
    }

    public synchronized native Metadata initializeDot(String filepath, int md);

    public synchronized native Metadata initializeGraph(int modeCommunity, double threshold, double antiThreshold);

    public synchronized native double[][] startsProgram(String filename);

    public synchronized native Metadata computeThreshold(int modeSimilitude, int edgeFactor);

    public synchronized native void setDimension(double width, double height);

    public synchronized native boolean updatePositions(ByteBuffer positionsBuffer);

    public synchronized native Vertex[] getPositions();

    public synchronized native void setNodePosition(int index, double x, double y);

    public synchronized native EdgeC[] getEdges();

    public synchronized native int[] getCommunities();

    public synchronized native float[][] getClusterColors();

    public synchronized native void setSaut(int saut);

    public synchronized native void setThresholdS(double thresholdS);

    public synchronized native void setFriction(double friction);

    public synchronized native void setModeRepulsion(int mode);

    public synchronized native void setAntiRepulsion(double antiedgeRepulsion);

    public synchronized native void setAttractionCoeff(double attractionCoeff);

    public synchronized native void setThresholdA(double thresholdA);

    public synchronized native void setSeuilRep(double seuilrep);

    public synchronized native void setAmortissement(double amortissement);

    public synchronized native void SetNumberClusters(int newNumberOfClusters);

    public synchronized native void setKmeansMode(boolean md);

    public synchronized native void setInitialNodeSize(double size);

    public synchronized native void setDegreeScaleFactor(double factor);

    public synchronized native void deleteNode(int index);

    public synchronized native void restoreNode(int index);

    public synchronized native void nativeFreeAllocatedMemory();
}
