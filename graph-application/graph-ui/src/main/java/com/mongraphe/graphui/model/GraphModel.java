package com.mongraphe.graphui.model;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongraphe.graphui.rendering.Camera2D;

/**
 * <strong>GraphModel</strong> est le cœur de stockage des données du graphe.
 * <p>
 * Il est conçu pour une haute performance en environnement multi-thread :
 * <ul>
 * <li>Lectures lock-free pour le thread de rendu via des collections
 * concurrentes.</li>
 * <li>Modifications structurelles protégées par un verrou d'écriture unique
 * (writeLock).</li>
 * <li>Paramètres de filtrage et d'affichage utilisant la visibilité
 * volatile.</li>
 * </ul>
 */
public class GraphModel {

    /**
     * Définit la logique de mise en couleur des sommets dans le shader.
     */
    public enum ColoringMode {
        /** Basé sur l'algorithme de détection de communautés. */
        COMMUNITY,
        /** Basé sur l'importance (nombre de voisins). */
        DEGREE,
        /** Couleur fixe définie par l'utilisateur. */
        UNIFORM
    }

    /**
     * Résultat d'une reconstruction depuis les buffers natifs.
     *
     * <p>
     * {@code verticesByNativeId} permet au moteur de simulation de mettre à jour
     * les positions en accès tableau direct sans repasser par une recherche dans
     * le modèle.
     * </p>
     */
    public record NativeBuildResult(Vertex[] verticesByNativeId, int vertexCount, int edgeCount) {
    }

    // Collections concurrentes (lecture lock-free pour ne pas bloquer le rendu)
    private final ConcurrentLinkedQueue<Vertex> vertices = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, Vertex> vertexById = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Edge> edges = new ConcurrentLinkedQueue<>();

    // Statistiques atomiques pour des compteurs thread-safe sans contention
    private final AtomicInteger visibleVertexCount = new AtomicInteger(0);
    private final AtomicInteger visibleEdgeCount = new AtomicInteger(0);
    private final AtomicInteger maxDegree = new AtomicInteger(1);
    private final AtomicInteger deletedVerticesCount = new AtomicInteger(0);
    private final AtomicInteger nextVertexId = new AtomicInteger(0);

    // Paramètres volatils (écrits par l'UI, lus par le Renderer)
    private volatile int selectedVertexId = -1;
    private volatile int filterMinDegree = 0;
    private volatile double filterMinEdgeWeight = 0.0;
    private volatile ColoringMode coloringMode = ColoringMode.COMMUNITY;

    private volatile float uniformNodeR = 0.82f;
    private volatile float uniformNodeG = 0.82f;
    private volatile float uniformNodeB = 0.86f;

    /**
     * Verrou interne pour garantir l'atomicité des opérations de filtrage et
     * reconstruction.
     */
    private final Object writeLock = new Object();

    // ----------------------------------------------------------------------
    // Accesseurs (lecture lock-free)
    // ----------------------------------------------------------------------

    public ConcurrentLinkedQueue<Vertex> vertices() {
        return vertices;
    }

    public ConcurrentLinkedQueue<Edge> edges() {
        return edges;
    }

    public Vertex vertexById(int id) {
        return vertexById.get(id);
    }

    public int vertexCount() {
        return vertices.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public int getVisibleVertexCount() {
        return visibleVertexCount.get();
    }

    public int getVisibleEdgeCount() {
        return visibleEdgeCount.get();
    }

    public int getMaxDegree() {
        return maxDegree.get();
    }

    public int getDeletedVerticesCount() {
        return deletedVerticesCount.get();
    }

    public int getSelectedVertexId() {
        return selectedVertexId;
    }

    public ColoringMode getColoringMode() {
        return coloringMode;
    }

    public int getFilterMinDegree() {
        return filterMinDegree;
    }

    public double getFilterMinEdgeWeight() {
        return filterMinEdgeWeight;
    }

    public float getUniformNodeR() {
        return uniformNodeR;
    }

    public float getUniformNodeG() {
        return uniformNodeG;
    }

    public float getUniformNodeB() {
        return uniformNodeB;
    }

    // ----------------------------------------------------------------------
    // Mutateurs
    // ----------------------------------------------------------------------

    public void setSelectedVertexId(int id) {
        this.selectedVertexId = id;
    }

    public void setColoringMode(ColoringMode mode) {
        this.coloringMode = mode == null ? ColoringMode.COMMUNITY : mode;
    }

    public void setUniformNodeColor(float r, float g, float b) {
        this.uniformNodeR = clamp01(r);
        this.uniformNodeG = clamp01(g);
        this.uniformNodeB = clamp01(b);
    }

    public void setFilterMinDegree(int minDegree) {
        this.filterMinDegree = Math.max(0, minDegree);
        applyFilters();
    }

    public void setFilterMinEdgeWeight(double minEdgeWeight) {
        this.filterMinEdgeWeight = Math.max(0.0, minEdgeWeight);
        applyFilters();
    }

    /**
     * Réapplique les filtres courants.
     *
     * <p>
     * Utile quand un état de sommet change sans modifier les seuils eux-mêmes
     * restauration/suppression, changement de degré, etc.
     * </p>
     */
    public void applyCurrentFilters() {
        applyFilters();
    }

    // ----------------------------------------------------------------------
    // Construction / Mise à jour depuis le natif
    // ----------------------------------------------------------------------

    /**
     * Reconstruit entièrement le modèle à partir des tableaux natifs.
     * <p>
     * Cette méthode synchronisée nettoie l'ancien graphe et mappe les structures C
     * vers les objets Vertex/Edge Java.
     * </p>
     *
     * @param verticesArray Tableau brut des sommets du moteur natif.
     * @param edgesArray    Tableau brut des arêtes du moteur natif.
     */
    public void buildFromData(Vertex[] verticesArray, EdgeC[] edgesArray) {
        Objects.requireNonNull(verticesArray, "verticesArray");
        Objects.requireNonNull(edgesArray, "edgesArray");

        synchronized (writeLock) {
            clear();

            // Table temporaire pour les correspondances index -> Vertex (Mapping JNI)
            Vertex[] tempIndex = new Vertex[verticesArray.length];

            for (int i = 0; i < verticesArray.length; i++) {
                Vertex v = verticesArray[i];
                if (v != null) {
                    v.setId(i);
                    vertices.add(v);
                    vertexById.put(i, v);
                    tempIndex[i] = v;
                }
            }

            nextVertexId.set(verticesArray.length);

            for (EdgeC ec : edgesArray) {
                if (ec == null) {
                    continue;
                }

                int startId = ec.getStart();
                int endId = ec.getEnd();

                if (startId < 0 || endId < 0 || startId >= tempIndex.length || endId >= tempIndex.length) {
                    continue;
                }

                Vertex start = tempIndex[startId];
                Vertex end = tempIndex[endId];

                if (start != null && end != null) {
                    edges.add(new Edge(start, end, ec.getWeight()));
                }
            }

            for (Vertex v : tempIndex) {
                if (v != null) {
                    v.updateDiameter();
                }
            }

            applyFiltersLocked();
        }
    }

    /**
     * Reconstruit le modèle depuis les buffers primitifs remplis par le JNI.
     *
     * <p>
     * Il évite la création JNI de
     * {@code Vertex[]} et {@code EdgeC[]} et ne fait qu'une reconstruction Java
     * linéaire.
     * </p>
     *
     * @param nativeVertexCount     nombre de sommets côté C
     * @param nativeEdgeCount       nombre d'arêtes côté C
     * @param positionsBuffer       float[2 * V] : x, y
     * @param edgeEndpointsBuffer   int[2 * E] : start, end
     * @param edgeWeightsBuffer     float[E] : poids des arêtes
     * @param communityIdsBuffer    int[V] : communauté de chaque sommet
     * @param communityColorsBuffer float[3 * V] : couleur associée à chaque sommet
     * @return table d'accès direct nativeId -> Vertex
     */
    public NativeBuildResult buildFromNativeBuffers(
            int nativeVertexCount,
            int nativeEdgeCount,
            FloatBuffer positionsBuffer,
            IntBuffer edgeEndpointsBuffer,
            FloatBuffer edgeWeightsBuffer,
            IntBuffer communityIdsBuffer,
            FloatBuffer communityColorsBuffer) {

        if (nativeVertexCount < 0 || nativeEdgeCount < 0) {
            throw new IllegalArgumentException(
                    "Tailles natives invalides : V=" + nativeVertexCount + ", E=" + nativeEdgeCount);
        }

        Objects.requireNonNull(positionsBuffer, "positionsBuffer");
        Objects.requireNonNull(communityIdsBuffer, "communityIdsBuffer");
        Objects.requireNonNull(communityColorsBuffer, "communityColorsBuffer");

        if (positionsBuffer.capacity() < nativeVertexCount * 2) {
            throw new IllegalArgumentException("positionsBuffer trop petit pour " + nativeVertexCount + " sommets");
        }

        if (communityIdsBuffer.capacity() < nativeVertexCount) {
            throw new IllegalArgumentException("communityIdsBuffer trop petit pour " + nativeVertexCount + " sommets");
        }

        if (communityColorsBuffer.capacity() < nativeVertexCount * 3) {
            throw new IllegalArgumentException(
                    "communityColorsBuffer trop petit pour " + nativeVertexCount + " sommets");
        }

        if (nativeEdgeCount > 0) {
            Objects.requireNonNull(edgeEndpointsBuffer, "edgeEndpointsBuffer");
            Objects.requireNonNull(edgeWeightsBuffer, "edgeWeightsBuffer");

            if (edgeEndpointsBuffer.capacity() < nativeEdgeCount * 2) {
                throw new IllegalArgumentException(
                        "edgeEndpointsBuffer trop petit pour " + nativeEdgeCount + " arêtes");
            }

            if (edgeWeightsBuffer.capacity() < nativeEdgeCount) {
                throw new IllegalArgumentException("edgeWeightsBuffer trop petit pour " + nativeEdgeCount + " arêtes");
            }
        }

        synchronized (writeLock) {
            clear();

            Vertex[] byNativeId = new Vertex[nativeVertexCount];

            // Les communautés sont mutualisées par identifiant.
            Map<Integer, Community> communities = new HashMap<>();

            for (int i = 0; i < nativeVertexCount; i++) {
                int p = i * 2;
                float x = positionsBuffer.get(p);
                float y = positionsBuffer.get(p + 1);

                int cid = communityIdsBuffer.get(i);

                Community community = communities.get(cid);
                if (community == null) {
                    int c = i * 3;

                    float r = communityColorsBuffer.get(c);
                    float g = communityColorsBuffer.get(c + 1);
                    float b = communityColorsBuffer.get(c + 2);

                    community = new Community(cid, r, g, b);
                    communities.put(cid, community);
                }

                Vertex vertex = new Vertex(x, y);
                vertex.setId(i);
                vertex.setCommunity(community);

                vertices.add(vertex);
                vertexById.put(i, vertex);
                byNativeId[i] = vertex;
            }

            nextVertexId.set(nativeVertexCount);

            for (int i = 0; i < nativeEdgeCount; i++) {
                int e = i * 2;
                int startId = edgeEndpointsBuffer.get(e);
                int endId = edgeEndpointsBuffer.get(e + 1);

                if (startId < 0 || endId < 0 || startId >= nativeVertexCount || endId >= nativeVertexCount) {
                    continue;
                }

                Vertex start = byNativeId[startId];
                Vertex end = byNativeId[endId];

                if (start == null || end == null) {
                    continue;
                }

                float weight = edgeWeightsBuffer.get(i);
                edges.add(new Edge(start, end, weight));
            }

            /*
             * Le diamètre dépend du degré.
             * On le calcule donc après la création des arêtes, pas avant.
             */
            for (Vertex vertex : byNativeId) {
                if (vertex != null) {
                    vertex.updateDiameter();
                }
            }

            applyFiltersLocked();

            return new NativeBuildResult(byNativeId, nativeVertexCount, nativeEdgeCount);
        }
    }

    // ----------------------------------------------------------------------
    // Ajout dynamique
    // ----------------------------------------------------------------------

    public void addVertex(Vertex v) {
        synchronized (writeLock) {
            int id = nextVertexId.getAndIncrement();
            v.setId(id);
            v.updateDiameter();
            vertices.add(v);
            vertexById.put(id, v);
            applyFiltersLocked();
        }
    }

    public void addEdge(Edge e) {
        synchronized (writeLock) {
            edges.add(e);
            e.getStart().updateDiameter();
            e.getEnd().updateDiameter();
            applyFiltersLocked();
        }
    }

    // ----------------------------------------------------------------------
    // Nettoyage
    // ----------------------------------------------------------------------

    public void clear() {
        synchronized (writeLock) {
            vertices.clear();
            vertexById.clear();
            edges.clear();
            nextVertexId.set(0);
            selectedVertexId = -1;
            visibleVertexCount.set(0);
            visibleEdgeCount.set(0);
            maxDegree.set(1);
            deletedVerticesCount.set(0);
        }
    }

    // ----------------------------------------------------------------------
    // Suppression
    // ----------------------------------------------------------------------

    public void deleteVertex(Vertex v) {
        synchronized (writeLock) {
            v.delete();
            if (selectedVertexId == v.getId()) {
                selectedVertexId = -1;
            }
            applyFiltersLocked();
        }
    }

    // ----------------------------------------------------------------------
    // Application des filtres (recalcule visibilité et stats)
    // ----------------------------------------------------------------------

    /**
     * Recalcule l'état de visibilité de chaque élément du graphe en fonction des
     * filtres actifs.
     * <p>
     * Une arête n'est visible que si ses deux sommets sont visibles et que son
     * poids
     * dépasse le seuil minimal.
     * </p>
     */
    private void applyFilters() {
        synchronized (writeLock) {
            applyFiltersLocked();
        }
    }

    /**
     * Version interne appelée quand writeLock est déjà détenu.
     */
    private void applyFiltersLocked() {
        int visV = 0, visE = 0, maxDeg = 1, delCount = 0;

        for (Vertex v : vertices) {
            if (v.isDeleted()) {
                v.setVisible(false);
                delCount++;
                continue;
            }

            boolean visible = v.getDegree() >= filterMinDegree;
            v.setVisible(visible);

            if (visible) {
                visV++;
            }

            maxDeg = Math.max(maxDeg, v.getDegree());
        }

        for (Edge e : edges) {
            boolean endpointsOk = e.getStart().isVisible() && e.getEnd().isVisible();
            boolean wOk = e.getWeight() >= filterMinEdgeWeight;
            boolean visible = endpointsOk && wOk;

            e.setVisible(visible);

            if (visible) {
                visE++;
            }
        }

        visibleVertexCount.set(visV);
        visibleEdgeCount.set(visE);
        maxDegree.set(maxDeg);
        deletedVerticesCount.set(delCount);
    }

    // ----------------------------------------------------------------------
    // Recherche spatiale (lecture lock-free directe)
    // ----------------------------------------------------------------------

    /**
     * Recherche un sommet aux coordonnées écran spécifiées.
     * <p>
     * Utilise la distance Euclidienne : $dist = \sqrt{(worldX - vX)^2 + (worldY -
     * vY)^2}$
     * </p>
     *
     * @param screenX Position X de la souris
     * @param screenY Position Y de la souris
     * @param camera  Caméra utilisée pour convertir l'écran en coordonnées monde
     * @return Le sommet trouvé, ou null si aucun ne correspond.
     */
    public Vertex findVertexAt(int screenX, int screenY, Camera2D camera) {
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        for (Vertex v : vertices) {
            if (v.isDeleted() || !v.isVisible())
                continue;
            double dx = worldX - v.getX();
            double dy = worldY - v.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            // On considère le zoom de la caméra pour ajuster la zone de clic
            double r = (v.getDiameter() / 2.0) / camera.getZoom();
            if (dist <= r)
                return v;
        }
        return null;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}