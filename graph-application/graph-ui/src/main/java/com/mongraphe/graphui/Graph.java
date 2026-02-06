package com.mongraphe.graphui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLRunnable;

import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.mongraphe.graphui.view.GraphView;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

public class Graph implements GLEventListener, GraphSettings {
    private GraphView graphVue;

    public Graph(GraphView graphVue, double graphWidth, double graphHeight) {
        this.graphWidth = graphWidth;
        this.graphHeight = graphHeight;
        this.graphVue = graphVue;
    }

    static {
        System.loadLibrary("native");
   }
   
    // Méthodes JNI
    public native double[][] startsProgram(String filename);

    public native Metadata computeThreshold(int modeSimilitude, int edge_factor);

    /**
     * the calculation depends on how big the window is
     * 
     * @param width  positive real number
     * @param height positive real number
     */
    public native void setDimension(double width, double height); // TODO

    public native boolean updatePositions();

    public native Vertex[] getPositions();

    public native void setNodePosition(int index, double x, double y);

    public native EdgeC[] getEdges();

    public native int[] getCommunities();

    public native float[][] getClusterColors();

    public native void setSaut(int saut);

    public native void setThresholdS(double thresholdS);

    public native void setFriction(double friction);

    public native void setModeRepulsion(int mode);

    public native void setAntiRepulsion(double antiedge_repulsion);

    public native void setAttractionCoeff(double attraction_coeff);

    public native void setThresholdA(double thresholdA);

    public native void setSeuilRep(double seuilrep);

    public native void setAmortissement(double amortissement);

    public native void SetNumberClusters(int new_number_of_clusters);

    public native void freeAllocatedMemory();

    /**
     * ignores node for the algorithm
     * 
     * @param index index of node to delete
     */
    public native void deleteNode(int index);

    /**
     * restores deleted node for the algorithm
     * 
     * @param index index of node to restore
     */
    public native void restoreNode(int index);

    /**
     * set cluster mode
     * 
     * @param md 0 == grid_clustering && 1 ==
     */
    public native void setKmeansMode(boolean md);

    /**
     * 
     * @return histogram
     */
    public native int[] getHistogram();

    public native Metadata initializeDot(String filepath, int md);

    public native Metadata initializeGraph(int modeCommunity, double threshold, double anti_threshold);

    // Variables graphiques
    public FPSAnimator animator; // Animation du rendu OpenGL

    // Variables liées au graphe
    public List<Vertex> vertices;
    public List<Edge> edges;

    public float bg_color_r = 1.0f;
    public float bg_color_g = 1.0f;
    public float bg_color_b = 1.0f;

    public static double WIDTH = 1500; // Largeur de la fenêtre
    public static double HEIGHT = 800; // Hauteur de la fenêtre
    public static int GRAPH_UPSCALE = 5; // Facteur d'agrandissment du graphe
    public static double CORRELATION_THRESHOLD = 0.5; // Définir la valeur seuil pour l'affichage de la corrélation

    // Propriétés pour les différents modes du graphe
    public static final BooleanProperty isRunMode = new SimpleBooleanProperty(true);
    public static final BooleanProperty isSelectionMode = new SimpleBooleanProperty(false);
    public static final BooleanProperty isMoveMode = new SimpleBooleanProperty(false);
    public static final BooleanProperty isDeleteMode = new SimpleBooleanProperty(false);

    // Propriété pour le degré minimum des sommets
    public static final IntegerProperty minimumDegree = new SimpleIntegerProperty(0);

    // Propriété pour la fréquence de mise à jour du graphe
    public static final DoubleProperty updateFrequency = new SimpleDoubleProperty(1.0);

    // Variables pour le déplacement des sommets et de la vue
    public boolean isDraggingVertex = false;
    public Vertex selectedVertex = null;
    public Vertex draggedVertex = null;
    public double dragStartX = 0;
    public double dragStartY = 0;
    public double viewOffsetX = 0;
    public double viewOffsetY = 0;
    public boolean isDraggingGraph = false;
    public double zoomFactor = 1.0;
    public final double zoomSensitivity = 0.1;

    public Metadata init_metadata;
    public Metadata metadata;
    public Pane root;
    public Scene scene;
    public Timeline timeline;
    public GLWindow glWindow;

    // Variables pour les buffers et shaders
    private FloatBuffer projectionMatrix;
    
	private int pointsShaderProgram;
	private int edgeBuffer;
	private int edgeColorBuffer;
	private int edgeSizeBuffer;
	private int edgeVisibilityBuffer;
	private float[] edgePoints;
	private float[] edgeSizes;
	private float[] edgeColors;
	private float[] edgeVisibility;
	
	private int edgesShaderProgram;
	private int vertexBuffer;
	private int vertexColorBuffer;
	private int vertexSizeBuffer;
	private int vertexVisibilityBuffer;
	private float[] vertexPoints;
	private float[] vertexSizes;
	private float[] vertexColors;
	private float[] vertexVisibility;

    // Variables pour le déplacement
    public double dragOffsetX = 0;
    public double dragOffsetY = 0;
    private double graphWidth;
    private double graphHeight;

    /**
     * Trouve le sommet à la position (x, y)
     * 
     * @param x Position x
     * @param y Position y
     * @return le sommet trouvé, ou null s'il n'y en a pas
     */
    public Vertex findVertexAt(double x, double y) {
        for (Vertex v : vertices) {
            if (v.isDeleted())
                continue;

            double dx = x - v.getX();
            double dy = y - v.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            double vertexDiameter = v.getDiameter();
            double margin = (vertexDiameter < 3) ? 3 : 0;
            double selectionRadius = ((v.getDiameter() / 2) + margin) / zoomFactor;

            if (distance <= selectionRadius) {
                return v;
            }
        }
        return null;
    }

    /**
     * Ajoute les listeners pour la souris
     */
    public void addMouseListeners() {

        glWindow.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Calculer les coordonnées ajustées avec le décalage de vue
                double x = (e.getX() - graphWidth / 2.0) / zoomFactor + viewOffsetX;
                double y = (graphHeight / 2.0 - e.getY()) / zoomFactor + viewOffsetY;

                // Vérifier si un sommet est cliqué
                selectedVertex = findVertexAt(x, y);

                // Déplacer un sommet
                if (isSelectionMode.get() && selectedVertex != null) {
                    isDraggingVertex = true;
                    dragOffsetX = selectedVertex.getX() - x;
                    dragOffsetY = selectedVertex.getY() - y;
                }

                // Se déplacer dans le graphe
                else if (isMoveMode.get()) {
                    isDraggingGraph = true;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                }
            }

            // Gère le relâchement du clic de la souris
            @Override
            public void mouseReleased(MouseEvent e) {
                // Déplacer un sommet
                if (isSelectionMode.get() && isDraggingVertex && selectedVertex != null) {
                    isDraggingVertex = false;
                    System.out.println("Déplacement du sommet vers (" + selectedVertex.getX() + ", "
                            + selectedVertex.getY() + ")");
                    setNodePosition(selectedVertex.getId(), selectedVertex.getX() / GRAPH_UPSCALE,
                            selectedVertex.getY() / GRAPH_UPSCALE);
                    vertexPoints[selectedVertex.getId() * 2] = (float) selectedVertex.getX();
                    vertexPoints[selectedVertex.getId() * 2 + 1] = (float) selectedVertex.getY();
                    selectedVertex = null;
                }
                isDraggingGraph = false;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                double x = (e.getX() - graphWidth / 2.0) / zoomFactor + viewOffsetX;
                double y = (graphHeight / 2.0 - e.getY()) / zoomFactor + viewOffsetY;

                // Vérifier si un sommet est cliqué
                selectedVertex = findVertexAt(x, y);

                // Obtenir les informations sur un sommet
                if (isSelectionMode.get() && selectedVertex != null) {
                    System.out.println("Sommet sélectionné : " + selectedVertex);
                }

                // Supprimer un sommet
                else if (isDeleteMode.get() && selectedVertex != null) {
                    selectedVertex.delete();
                    System.out.println("Sommet supprimé : " + selectedVertex);
                    deleteNode(selectedVertex.getId());
                    SwingUtilities.invokeLater(() -> glWindow.display());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                boolean updated = false;
                // Déplacer un sommet
                if (isSelectionMode.get() && isDraggingVertex && selectedVertex != null) {
                    // Calculer les coordonnées ajustées pour le drag
                    double x = (e.getX() - graphWidth / 2.0) / zoomFactor + viewOffsetX;
                    double y = (graphHeight / 2.0 - e.getY()) / zoomFactor + viewOffsetY;

                    // Appliquer l'offset du drag
                    double newX = x + dragOffsetX;
                    double newY = y + dragOffsetY;

                    selectedVertex.updatePosition(newX, newY);
                    vertexPoints[selectedVertex.getId() * 2] = (float) selectedVertex.getX();
                    vertexPoints[selectedVertex.getId() * 2 + 1] = (float) selectedVertex.getY();
                    updated = true;
                }

                // Se déplacer dans le graphe
                else if (isMoveMode.get() && isDraggingGraph) {
                    double deltaX = e.getX() - dragStartX;
                    double deltaY = e.getY() - dragStartY;

                    double speedFactor = 0.8 * 1 / zoomFactor;

                    viewOffsetX -= deltaX * speedFactor;
                    viewOffsetY += deltaY * speedFactor;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    updated = true;
                }

                // Forcer l'affichage sur le thread UI pour éviter les lags visuels pendant le
                // drag
                if (updated)
                    SwingUtilities.invokeLater(() -> glWindow.display());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Calculer les coordonnées ajustées
                double x = (e.getX() - graphWidth / 2.0) / zoomFactor + viewOffsetX;
                double y = (graphHeight / 2.0 - e.getY()) / zoomFactor + viewOffsetY;

                Vertex hoveredVertex = findVertexAt(x, y);
                if (hoveredVertex != null) {
                    Platform.runLater(() -> {
                        graphVue.setHoveredVertex(hoveredVertex);
                    });
                }
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                if (isMoveMode.get()) {
                    float[] rotation = e.getRotation(); // [x, y]
                    float scrollY = rotation[1];
                    double zoomAmount = 1.1;

                    if (scrollY == 0)
                        return;

                    // Calculer les coordonnées avant zoom
                    double mouseXBefore = (e.getX() - graphWidth / 2.0) / zoomFactor + viewOffsetX;
                    double mouseYBefore = (graphHeight / 2.0 - e.getY()) / zoomFactor + viewOffsetY;

                    if (scrollY > 0) {
                        zoomFactor *= zoomAmount;
                    } else {
                        zoomFactor /= zoomAmount;
                    }

                    // Limite du facteur de zoom pour éviter les zooms extrêmes
                    zoomFactor = Math.max(0.1, Math.min(zoomFactor, 10.0));

                    // Calculer les coordonnées après zoom
                    double mouseXAfter = (e.getX() - graphWidth / 2.0) / zoomFactor + viewOffsetX;
                    double mouseYAfter = (graphHeight / 2.0 - e.getY()) / zoomFactor + viewOffsetY;

                    // Ajuster le décalage pour maintenir le point sous la souris
                    viewOffsetX += (mouseXBefore - mouseXAfter);
                    viewOffsetY += (mouseYBefore - mouseYAfter);

                    updateProjectionMatrix();
                    SwingUtilities.invokeLater(() -> glWindow.display());
                }
            }
        });
    }

    /**
     * Ajoute les listeners pour le clavier
     */
    public void addKeyListeners() {
        glWindow.addKeyListener(new KeyListener() {

        	@Override
        	public void keyPressed(KeyEvent e) {
        	    char keyChar = e.getKeyChar(); // Récupère le caractère associé à la touche pressée

        	    // Afficher le caractère pour le débogage
        	    System.out.println("Touche pressée : " + keyChar);
        	    
        	    // Vérifier si la touche pressée est un chiffre entre 1 et 9
        	    if (keyChar >= '1' && keyChar <= '9') {
        	        int keyNumber = keyChar - '0';  // Convertit le caractère en un nombre entier
        	        System.out.println("Touche " + keyNumber + " pressée");

        	        // Switch en fonction de la touche pressée
        	        switch (keyNumber) {
        	            case 1:
        	                setMode(GraphData.GraphMode.SELECTION);
        	                System.out.println("Switch to " + getMode() + " - Vous pouvez sélectionner et déplacer des sommets");
        	                break;
        	            case 2:
        	                setMode(GraphData.GraphMode.DELETE);
        	                System.out.println("Switch to " + getMode() + " - Vous pouvez supprimer des sommets");
        	                break;
        	            case 3:
        	                setMode(GraphData.GraphMode.RUN);
        	                System.out.println("Back to " + getMode() + " - Exécution du graphe (en mouvement)");
        	                break;
        	            case 4:
        	                setMode(GraphData.GraphMode.MOVE);
        	                System.out.println("Back to " + getMode() + " - Vous pouvez vous déplacer dans le graphe");
        	                break;
        	            case 5:
        	            	if (getMinimumDegree() > 0) {
        	            		setMinimumDegree(0);
        	            		System.out.println("Minimum degree set to 0");
        	            	} else {
        	            		setMinimumDegree(1);
        	            		System.out.println("Minimum degree set to 1");
        	            	}
        	                break;
        	                
        	            case 6:
        	                scheduleExportToPng("capture/graph.png");
        	                break;
        	        }
        	    }

            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }

    /**
     * Initialise OpenGL
     * 
     * @param drawable Objet OpenGL
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4(); // Utiliser GL4 au lieu de GL
        gl.glClearColor(bg_color_r, bg_color_g, bg_color_b, 1.0f); // Couleur de fond de l'écran
        System.out.println("dans init:" + bg_color_r + " " + bg_color_g + " " + bg_color_b);
        gl.glEnable(GL4.GL_DEPTH_TEST); // Activer le test de profondeur pour les objets 3D
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);

        // Initialisation des buffers pour les sommets et arêtes
        initializeArrays();

        // Créer des buffers pour les positions, tailles et couleurs
        createBuffers(gl);

	    String vertexShaderSourcePoints =
				"""
				#version 400 core
				layout(location = 0) in vec2 position;
				layout(location = 1) in float size;
				layout(location = 2) in vec3 color;
				layout(location = 3) in float visibility;
				uniform mat4 u_transform;
				out vec3 fragColor;
				out float fragVisibility;
				void main() {
				   vec4 pos = vec4(position, 0.0, 1.0);
				   gl_Position = u_transform * pos;
				   gl_PointSize = size;
				   fragColor = color;
				   fragVisibility = visibility;
				}
						""";

	    String fragmentShaderSourcePoints =
				"""
				#version 400 core
				in vec3 fragColor;
				in float fragVisibility;
				out vec4 color;
				void main() {
				   if (fragVisibility == 0.0) {
			           discard;
			       }
				   float dist = length(gl_PointCoord - vec2(0.5, 0.5));
				   if (dist < 0.5) {
				       color = vec4(fragColor, 1.0);
				   } else {
				       discard;
				   }
				}
				""";

	    createEdgeBuffers(gl);

	    String vertexShaderSourceEdges =
	    		"""
				#version 400 core
				layout(location = 0) in vec2 position;
				layout(location = 1) in vec3 color;
				layout(location = 2) in float size;
				layout(location = 3) in float visibility;
				uniform mat4 u_transform;
				out vec3 fragColor;
				out float fragVisibility;
				void main() {
				    vec4 pos = vec4(position, 0.0, 1.0);
				    gl_Position = u_transform * pos;
				    fragColor = color;
				    fragVisibility = visibility;
				}
	    		""";

	    String fragmentShaderSourceEdges =
				"""
				#version 400 core
				in vec3 fragColor;
				in float fragVisibility;
				out vec4 color;
				void main() {
				    if (fragVisibility == 0.0) {
				        discard;
				    }
				    color = vec4(fragColor, 1.0);
				}
				""";

        pointsShaderProgram = createShaderProgram(gl, vertexShaderSourcePoints, fragmentShaderSourcePoints);
        edgesShaderProgram = createShaderProgram(gl, vertexShaderSourceEdges, fragmentShaderSourceEdges);
        // gl.glHint(GL4.GL_POINT_SMOOTH_HINT, GL4.GL_NICEST);
    }

    /**
     * Libère les ressources OpenGL
     * 
     * @param drawable Objet OpenGL
     */
    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    private int visibleNodes = 0;
    private int visibleEdges = 0;
    private int hiddenNodes = 0;
    private int hiddenEdges = 0;
    private int minDegree = 0;
    private int deletedNodes = 0;

    /**
     * Affiche le graphe avec OpenGL
     * 
     * @param drawable Objet OpenGL
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        // Met à jour la matrice de transformation avec les offsets actuels
        updateProjectionMatrix();

        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        // Réinitialisation des compteurs
        visibleNodes = 0;
        hiddenNodes = 0;
        visibleEdges = 0;
        hiddenEdges = 0;
        deletedNodes = 0;
        minDegree = minimumDegree.get();

        if (isRunMode.get()) {
            boolean is_running = updatePositions();
            List<Vertex> updatedVertices = List.of(getPositions());
            for (int i = 0; i < updatedVertices.size(); i++) {
                Vertex v = vertices.get(i);
                // Mise à jour des coordonnées des sommets
                v.updatePosition(updatedVertices.get(i).getX(), updatedVertices.get(i).getY());

                if (v.getDegree() >= minDegree) {
                    visibleNodes++;
                    /*
                     * System.out.println("Sommet afficher : " + selectedVertex);
                     * SwingUtilities.invokeLater(() -> glWindow.display());
                     */
                } else {
                    hiddenNodes++;
                }

                if (v.isDeleted()) {
                    deletedNodes++;
                    visibleNodes--;
                }
            }
            // Deuxième passe : filtrer les arêtes
            for (Edge e : edges) {
                if (e.getStart().getDegree() >= minDegree &&
                        e.getEnd().getDegree() >= minDegree) {
                    visibleEdges++;
                } else {
                    hiddenEdges++;
                }
            }

            // Mise à jour finale
            Platform.runLater(() -> {
                graphVue.updateNodeStats(visibleNodes, hiddenNodes, visibleEdges, hiddenEdges, deletedNodes);
            });
        }

        prepareVertexRenderData();
        prepareEdgeRenderData();

        // === Envoi des données GPU ===
        // Sommets
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexPoints.length * Float.BYTES,
                FloatBuffer.wrap(vertexPoints));

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexSizeBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexSizes.length * Float.BYTES,
                FloatBuffer.wrap(vertexSizes));

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexColorBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexColors.length * Float.BYTES,
                FloatBuffer.wrap(vertexColors));
        
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexVisibilityBuffer);
	    gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexVisibility.length * Float.BYTES, 
                FloatBuffer.wrap(vertexVisibility));

        // === Affichage des sommets ===
        gl.glUseProgram(pointsShaderProgram);

        gl.glEnableVertexAttribArray(0); // position
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBuffer);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(1); // size
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexSizeBuffer);
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(2); // color
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexColorBuffer);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(3); // visibility
	    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexVisibilityBuffer);
	    gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

        // Matrice de projection (identité ou zoom plus tard)
        int transformLoc = gl.glGetUniformLocation(pointsShaderProgram, "u_transform");
        gl.glUniformMatrix4fv(transformLoc, 1, false, projectionMatrix);

        // Dessin des points
        gl.glDrawArrays(GL4.GL_POINTS, 0, vertices.size());

        // Arêtes
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgePoints.length * Float.BYTES,
                FloatBuffer.wrap(edgePoints));

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeColorBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgeColors.length * Float.BYTES,
                FloatBuffer.wrap(edgeColors));

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeSizeBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgeSizes.length * Float.BYTES, FloatBuffer.wrap(edgeSizes));

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeVisibilityBuffer);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgeVisibility.length * Float.BYTES,
                FloatBuffer.wrap(edgeVisibility));

        // === Affichage des arêtes ===
        gl.glUseProgram(edgesShaderProgram);

        gl.glEnableVertexAttribArray(0); // position
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeBuffer);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(1); // color
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeColorBuffer);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(2); // size
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeSizeBuffer);
        gl.glVertexAttribPointer(2, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(3); // visibility
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeVisibilityBuffer);
        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

        int transformLocEdges = gl.glGetUniformLocation(edgesShaderProgram, "u_transform");
        gl.glUniformMatrix4fv(transformLocEdges, 1, false, projectionMatrix);

        // Dessin des lignes (2 points par arête)
        gl.glDrawArrays(GL4.GL_LINES, 0, edges.size() * 2);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int graphWidth, int height) {
        // Réajustement de la matrice de projection pour tenir compte de la taille de la
        // fenêtre
        float left = -graphWidth / 2f;
        float right = graphWidth / 2f;
        float bottom = -height / 2f;
        float top = height / 2f;
        float near = -1f;
        float far = 1f;

        float[] orthoMatrix = new float[] {
                2f / (right - left), 0, 0, 0,
                0, 2f / (top - bottom), 0, 0,
                0, 0, -2f / (far - near), 0,
                -(right + left) / (right - left), -(top + bottom) / (top - bottom), -(far + near) / (far - near), 1f
        };

        // Sauvegarder la matrice dans un buffer pour l'envoyer comme uniforme
        this.projectionMatrix = FloatBuffer.wrap(orthoMatrix);
    }

    public void stop() {
        if (animator != null) {
            animator.stop();
        }
        freeAllocatedMemory();
    }

    // -------------------------------------------------------------------------
    // Exemples d'initialisation et d'actions sur le graphe
    // -------------------------------------------------------------------------

    // /**
    // * Exemple d'initialisation du graphe (à remplacer par l'interface graphique)
    // *
    // * @see GraphData.SimilitudeMode
    // * @see GraphData.NodeCommunity
    // */
    // public void testInit() {
    // // Initialisation du graphe avec le fichier à charger, la méthode de
    // similitude
    // // et la méthode de détection de communautés
    // String sample2 = "samples/predicancerNUadd9239.csv";
    // initGraph(sample2, GraphData.SimilitudeMode.CORRELATION,
    // GraphData.NodeCommunity.LOUVAIN);

    // setScreenSize(graphWidth, HEIGHT); // Taille de l'écran du graphe
    // setBackgroundColor(0.0f, 0.0f, 0.0f); // Couleur de fond du graphe
    // setUpscale(5); // Facteur d'agrandissement pour le graphe
    // setInitialNodeSize(3); // Taille initiale d'un sommet
    // setDegreeScaleFactor(0.3); // Facteur d'agrandissement selon le degré d'un
    // sommet
    // }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Initialise le graphe avec les données du fichier .csv
     * 
     * @param path      Chemin du fichier .csv à charger
     * @param mode      Mode de similitude à utiliser
     * @param community Mode de détection de communautés à utiliser
     * @return les données du fichier .csv
     * @see GraphData.SimilitudeMode
     * @see GraphData.NodeCommunity
     */
    @Override
    public double[][] initGraph(String path, GraphData.SimilitudeMode mode, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("initGraph : Chemin du fichier non spécifié.");

        // Appeler startsProgram avant d'utiliser les données natives
        double[][] data = startsProgram(path);

        // Déterminer le mode de similitude à utiliser
        if (mode == null)
            throw new RuntimeException("initGraph : Mode de similitude non spécifié.");
        int modeSimilitude = getModeSimilitude(mode);

        init_metadata = computeThreshold(modeSimilitude, 50);
        if (init_metadata == null)
            throw new RuntimeException("initGraph : Une erreur est survenue lors du calcul des seuils.");

        double recommendedThreshold = init_metadata.getEdgeThreshold();
        double recommendedAntiThreshold = init_metadata.getAntiThreshold();

        System.out.println("Seuil recommandé pour les arêtes : " + recommendedThreshold);
        System.out.println("Seuil recommandé pour les anti-arêtes : " + recommendedAntiThreshold);

        // Valeurs imposées pour le moment (à modifier)
        // recommendedThreshold = 0.966;
        // recommendedAntiThreshold = 0.6;

        // Déterminer le mode de détection de communautés à utiliser
        if (community == null)
            throw new RuntimeException("initGraph : Mode de détection de communautés non spécifié.");
        int modeCommunity = getModeCommunity(community);

        metadata = initializeGraph(modeCommunity, recommendedThreshold, recommendedAntiThreshold);

        return data;
    }

    /**
     * Initialise le graphe avec les données du fichier .csv
     * 
     * @param path      Chemin du fichier .csv à charger
     * @param mode      Mode de similitude à utiliser
     * @param community Mode de détection de communautés à utiliser
     * @return les données du fichier .csv
     * @see GraphData.SimilitudeMode
     * @see GraphData.NodeCommunity
     */
    @Override
    public double[][] initGraphCsv(String path, GraphData.SimilitudeMode mode, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("initGraphCsv : Chemin du fichier non spécifié.");

        // Appeler startsProgram avant d'utiliser les données natives
        double[][] data = startsProgram(path);

        // Déterminer le mode de similitude à utiliser
        if (mode == null)
            throw new RuntimeException("initGraphCsv : Mode de similitude non spécifié.");
        int modeSimilitude = getModeSimilitude(mode);

        init_metadata = computeThreshold(modeSimilitude, 5);
        if (init_metadata == null)
            throw new RuntimeException("initGraphCsv : Une erreur est survenue lors du calcul des seuils.");

        double recommendedThreshold = init_metadata.getEdgeThreshold();
        double recommendedAntiThreshold = init_metadata.getAntiThreshold();

        System.out.println("Seuil recommandé pour les arêtes : " + recommendedThreshold);
        System.out.println("Seuil recommandé pour les anti-arêtes : " + recommendedAntiThreshold);

        // Valeurs imposées pour le moment (à modifier)
        // recommendedThreshold = 0.966;
        // recommendedAntiThreshold = 0.6;

        // Déterminer le mode de détection de communautés à utiliser
        if (community == null)
            throw new RuntimeException("initGraphCsv : Mode de détection de communautés non spécifié.");
        int modeCommunity = getModeCommunity(community);

        metadata = initializeGraph(modeCommunity, recommendedThreshold, recommendedAntiThreshold);

        return data;
    }

    /**
     * Initialise le graphe avec les données du fichier .dot
     * 
     * @param path      Chemin du fichier .dot à charger
     * @param community Mode de détection de communautés à utiliser
     * @see GraphData.SimilitudeMode
     * @see GraphData.NodeCommunity
     */
    @Override
    public void initGraphDot(String path, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("initGraphDot : Chemin du fichier non spécifié.");

        // Déterminer le mode de détection de communautés à utiliser
        if (community == null)
            throw new RuntimeException("initGraphDot : Mode de détection de communautés non spécifié.");
        int modeCommunity = getModeCommunity(community);

        metadata = initializeDot(path, modeCommunity);
    }

    /**
     * Initialise la taille de l'écran du graphe
     * 
     * @param graphWidth Largeur de l'écran (en px)
     * @param height     Hauteur de l'écran (en px)
     */
    @Override
    public void setScreenSize(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new RuntimeException("setScreenSize : Taille de l'écran (" + width + "x" + height + ") non valide.");
        graphWidth = width;
        graphHeight = height;
    }

    /**
     * Modifie la couleur de fond du graphe
     * 
     * @param color_r Composante rouge de la couleur
     * @param color_g Composante verte de la couleur
     * @param color_b Composante bleue de la couleur
     */
    @Override
    public void setBackgroundColor(float color_r, float color_g, float color_b) {
        if (color_r < 0 || color_r > 1 || color_g < 0 || color_g > 1 || color_b < 0 || color_b > 1)
            throw new RuntimeException(
                    "setBackgroundColor : Couleur (" + color_r + ", " + color_g + ", " + color_b + ") non valide.");
        this.bg_color_r = color_r;
        this.bg_color_g = color_g;
        this.bg_color_b = color_b;
        System.out.println(" setBackgroundColor : " + color_r + " " + color_g + " " + color_b);
    }

    /**
     * @param upscale Facteur d'agrandissement pour le graphe
     */
    @Override
    public void setUpscale(int upscale) {
        if (upscale < 0)
            throw new RuntimeException("setUpscale : Facteur d'agrandissement (" + upscale + ") non valide.");
        Vertex.upscale = upscale;
    }

    /**
     * @param size Taille initiale d'un sommet
     */
    @Override
    public void setInitialNodeSize(double size) {
        System.out.println("setInitialNodeSize : " + size);
        if (size <= 0)
            throw new RuntimeException("setInitialNodeSize : Taille initiale d'un sommet (" + size + ") non valide.");
        Vertex.initial_node_size = size;
    }

    /**
     * @param factor Facteur d'agrandissement selon le degré d'un sommet (0 pour que
     *               la taille soit identique pour tous les sommets, > 0 pour faire
     *               varier la taille proportionnellement au degré)
     */
    @Override
    public void setDegreeScaleFactor(double factor) {
        if (factor < 0)
            throw new RuntimeException(
                    "setDegreeScaleFactor : Facteur d'agrandissement selon le degré (" + factor + ") non valide.");
        Vertex.degree_scale_factor = factor;
    }

    // -------------------------------------------------------------------------
    // Mode de sélection
    // -------------------------------------------------------------------------

    /**
     * @return le mode actuel du graphe
     * @see GraphData.GraphMode
     */
    @Override
    public GraphData.GraphMode getMode() {
        if (isRunMode.get()) {
            return GraphData.GraphMode.RUN;
        } else if (isSelectionMode.get()) {
            return GraphData.GraphMode.SELECTION;
        } else if (isMoveMode.get()) {
            return GraphData.GraphMode.MOVE;
        } else if (isDeleteMode.get()) {
            return GraphData.GraphMode.DELETE;
        }
        return null;
    }

    /**
     * Définit le mode du graphe
     * 
     * @param mode le mode à définir
     * @see GraphData.GraphMode
     */
    @Override
    public void setMode(GraphData.GraphMode mode) {
        if (mode == null)
            throw new RuntimeException("setMode : Mode non spécifié.");

        // Utiliser Platform.runLater pour s'assurer que les modifications
        // des propriétés JavaFX sont faites sur le thread JavaFX
        Platform.runLater(() -> {
            System.out.println("Changement de mode vers: " + mode);

            // Définir le nouveau mode
            isRunMode.set(mode == GraphData.GraphMode.RUN);
            isSelectionMode.set(mode == GraphData.GraphMode.SELECTION);
            isMoveMode.set(mode == GraphData.GraphMode.MOVE);
            isDeleteMode.set(mode == GraphData.GraphMode.DELETE);

            // Afficher un message selon le mode activé
            if (mode == GraphData.GraphMode.RUN) {
                System.out.println("Back to RUN - Exécution du graphe (en mouvement)");
            } else if (mode == GraphData.GraphMode.SELECTION) {
                System.out.println("Switch to SELECTION - Vous pouvez sélectionner et déplacer des sommets");
            } else if (mode == GraphData.GraphMode.MOVE) {
                System.out.println("Switch to MOVE - Vous pouvez vous déplacer dans le graphe");
            } else if (mode == GraphData.GraphMode.DELETE) {
                System.out.println("Switch to DELETE - Vous pouvez supprimer des sommets");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Paramètres de la simulation
    // -------------------------------------------------------------------------

    /**
     * @return le seuil recommandé pour les arêtes
     */
    @Override
    public double getRecommendedThreshold() {
        if (init_metadata == null)
            throw new RuntimeException(
                    "getRecommendedThreshold : Métadonnées non initialisées. Veuillez appeler initGraph() avant.");
        return init_metadata.getEdgeThreshold();
    }

    /**
     * @return le seuil actuel pour les arêtes
     */
    @Override
    public double getThreshold() {
        if (init_metadata == null)
            throw new RuntimeException(
                    "getThreshold : Métadonnées non initialisées. Veuillez appeler initGraph() avant.");
        return metadata.getEdgeThreshold();
    }

    /**
     * @return le seuil recommandé pour les anti-arêtes
     */
    @Override
    public double getRecommendedAntiThreshold() {
        if (init_metadata == null)
            throw new RuntimeException(
                    "getRecommendedAntiThreshold : Métadonnées non initialisées. Veuillez appeler initGraph() avant.");
        return init_metadata.getAntiThreshold();
    }

    /**
     * @return le seuil actuel pour les anti-arêtes
     */
    @Override
    public double getAntiThreshold() {
        if (metadata == null)
            throw new RuntimeException(
                    "getAntiThreshold : Métadonnées non initialisées. Veuillez appeler initGraph() avant.");
        return metadata.getAntiThreshold();
    }

    /**
     * Le seuil de stabilité indique quand le graphe doit s'arrêter (si le mouvement
     * est inférieur au seuil et que suffisamment de temps s'est écoulé, alors le
     * graphe s'arrête de bouger)
     * 
     * @param threshold Nouveau seuil à appliquer
     */
    @Override
    public void setStabilizedThreshold(double threshold) {
        setThresholdS(threshold);
    }

    /**
     * Le seuil d'attraction correspond à la distance minimum pour appliquer une
     * force d'attraction entre deux points
     * 
     * @param threshold Seuil d'attraction entre les sommets
     */
    @Override
    public void setAttractionThreshold(double threshold) {
        setThresholdA(threshold);
    }

    /**
     * @param freq Fréquence à laquelle les clusters sont mis à jour
     */
    @Override
    public void setUpdatedFrequence(int freq) {
        setSaut(freq);
    }

    /**
     * @param friction Friction à appliquer
     */
    @Override
    public void setNewFriction(double friction) {
        setFriction(friction);
    }

    /**
     * Choisir le mode de répulsion à utiliser pour mettre à jour les positions
     * 
     * @param mode Mode de répulsion à utiliser
     * @see GraphData.RepulsionMode
     */
    @Override
    public void setRepulsionMode(GraphData.RepulsionMode mode) {
        setModeRepulsion(getModeRepulsion(mode));
    }

    /**
     * @param antiedge_repulsion Force de répulsion des anti-arêtes
     */
    @Override
    public void setAntiEdgesRepulsion(double antiedge_repulsion) {
        setAntiRepulsion(antiedge_repulsion);
    }

    /**
     * @param attraction_coeff Force d'attraction entre les sommets
     */
    @Override
    public void setAttractionCoefficient(double attraction_coeff) {
        setAttractionCoeff(attraction_coeff);
    }

    /**
     * @param threshold Seuil de répulsion entre les sommets
     */
    @Override
    public void setRepulsionThreshold(double threshold) {
        setSeuilRep(threshold);
    }

    /**
     * @param amortissement Amortissement à appliquer (facteur dictant comment la
     *                      friction évolue après chaque mise à jour du graphe)
     */
    @Override
    public void setNewAmortissement(double amortissement) {
        setAmortissement(amortissement);
    }

    /**
     * @param new_number_of_clusters Nombre de clusters à considérer
     */
    @Override
    public void setNbClusters(int new_number_of_clusters) {
        SetNumberClusters(new_number_of_clusters);
    }

    /**
     * @param isEnabled <code>true</code> pour utiliser les Kmeans,
     *                  <code>false</code> pour utiliser le grid clustering
     */
    @Override
    public void enableKmeans(boolean isEnabled) {
        setKmeansMode(isEnabled);
    }

    /**
     * @return l'histogramme // TODO
     */
    @Override
    public int[] getHistogramme() {
        return getHistogram();
    }

    /**
     * @return le degré minimum des sommets à afficher
     */
    @Override
    public int getMinimumDegree() {
        return minimumDegree.get();
    }

	/**
	 * Affiche les sommets dont le degré est supérieur ou égal à degree
	 * @param degree Degré minimum des sommets à afficher
	 */
	@Override
	public void setMinimumDegree(int degree) {
		if (degree < 0)
			throw new RuntimeException("setMiniumDegree : Degré minimum (" + degree + ") non valide.");
		minimumDegree.set(degree);
		for (Vertex v : vertices) {
			v.setVisibility(v.getDegree() >= degree);
		}
	}

    /**
     * @param vertex_id Identifiant du sommet à supprimer
     */
    public void removeVertex(int vertex_id) {
        // TODO
    }

	/**
	 * Schedule the export to happen in the OpenGL thread
	 * This ensures the GL context is valid when we do the export
	 */
	private void scheduleExportToPng(final String filename) {
	    // Create a directory for the captures if it doesn't exist
	    File captureDir = new File("capture");
	    if (!captureDir.exists()) {
	        captureDir.mkdir();
	    }
	    
	    // Ensure we're on the OpenGL thread
	    if (animator != null) {
	        // Request a one-time rendering action that will run the export
	        glWindow.invoke(true, new GLRunnable() {
	            @Override
	            public boolean run(GLAutoDrawable drawable) {
	                System.out.println("Executing export on OpenGL thread");
	                exportToPng(drawable.getGL().getGL4(), filename);
	                return true;
	            }
	        });
	    } else {
	        System.err.println("Animation is not active, cannot export");
	    }
	}

	/**
	 * Exports the current OpenGL rendering to a PNG file without disturbing the 
	 * current display state. Creates an offscreen framebuffer for rendering.
	 * 
	 * @param gl the OpenGL context
	 * @param path the file path where the PNG will be saved
	 */
	public void exportToPng(GL4 gl, String path) {
	    // Store current viewport dimensions
	    int[] viewport = new int[4];
	    gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
	    int viewportWidth = viewport[2];
	    int viewportHeight = viewport[3];

	    // Set the export size for higher resolution
	    int exportWidth = viewportWidth * 1;
	    int exportHeight = viewportHeight * 1;

	    // Remember the current framebuffer
	    int[] currentFBO = new int[1];
	    gl.glGetIntegerv(GL4.GL_FRAMEBUFFER_BINDING, currentFBO, 0);

	    // Arrays to hold generated OpenGL objects
	    int[] fbo = new int[1];
	    int[] textureId = new int[1];
	    int[] rbo = new int[1];

	    try {
	        // Create a framebuffer object (FBO)
	        gl.glGenFramebuffers(1, fbo, 0);
	        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, fbo[0]);

	        // Create a texture to render to
	        gl.glGenTextures(1, textureId, 0);
	        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId[0]);

	        // Make sure we use a supported internal format
	        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
	                        exportWidth, exportHeight,
	                        0, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, null);

	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);

	        // Attach the texture to the FBO
	        gl.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0,
	                                  GL4.GL_TEXTURE_2D, textureId[0], 0);

	        // Create a renderbuffer object for depth
	        gl.glGenRenderbuffers(1, rbo, 0);
	        gl.glBindRenderbuffer(GL4.GL_RENDERBUFFER, rbo[0]);
	        gl.glRenderbufferStorage(GL4.GL_RENDERBUFFER, GL4.GL_DEPTH_COMPONENT24,
	                                 exportWidth, exportHeight);

	        // Attach the renderbuffer to the FBO
	        gl.glFramebufferRenderbuffer(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT,
	                                     GL4.GL_RENDERBUFFER, rbo[0]);

	        // Explicitly define which draw buffers to use
	        int[] drawBuffers = {GL4.GL_COLOR_ATTACHMENT0};
	        gl.glDrawBuffers(1, drawBuffers, 0);

	        // Check if framebuffer is complete
	        int status = gl.glCheckFramebufferStatus(GL4.GL_FRAMEBUFFER);
	        if (status != GL4.GL_FRAMEBUFFER_COMPLETE) {
	            System.err.println("Framebuffer is not complete! Status: 0x" + Integer.toHexString(status));

	            // Display more details about the error
	            switch(status) {
	                case GL4.GL_FRAMEBUFFER_UNDEFINED:
	                    System.err.println("GL_FRAMEBUFFER_UNDEFINED");
	                    break;
	                case GL4.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
	                    System.err.println("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
	                    break;
	                case GL4.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
	                    System.err.println("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
	                    break;
	                case GL4.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
	                    System.err.println("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
	                    break;
	                case GL4.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
	                    System.err.println("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
	                    break;
	                case GL4.GL_FRAMEBUFFER_UNSUPPORTED:
	                    System.err.println("GL_FRAMEBUFFER_UNSUPPORTED");
	                    break;
	                case GL4.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
	                    System.err.println("GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE");
	                    break;
	                case GL4.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
	                    System.err.println("GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS");
	                    break;
	                case 0:
	                    System.err.println("Status is 0, which indicates a previous OpenGL error");
	                    break;
	                default:
	                    System.err.println("Unknown framebuffer status error");
	            }
	            return;
	        }

	        // Clear the framebuffer with  background
	        gl.glViewport(0, 0, exportWidth, exportHeight);
	        gl.glClearColor(bg_color_r, bg_color_g, bg_color_b, 1.0f);
	        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

	        // Render the graph to the offscreen buffer
	        // Update projection matrix (if needed)
	        updateProjectionMatrix();

	        // Prepare data for rendering
	        prepareVertexRenderData();
	        prepareEdgeRenderData();

	        // === Render vertices ===
	        gl.glUseProgram(pointsShaderProgram);

	        gl.glEnableVertexAttribArray(0);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexPoints.length * Float.BYTES, FloatBuffer.wrap(vertexPoints));
	        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

	        gl.glEnableVertexAttribArray(1);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexSizeBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexSizes.length * Float.BYTES, FloatBuffer.wrap(vertexSizes));
	        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, 0);

	        gl.glEnableVertexAttribArray(2);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexColorBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexColors.length * Float.BYTES, FloatBuffer.wrap(vertexColors));
	        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, 0, 0);

	        gl.glEnableVertexAttribArray(3);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexVisibilityBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexVisibility.length * Float.BYTES, FloatBuffer.wrap(vertexVisibility));
	        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

	        int transformLoc = gl.glGetUniformLocation(pointsShaderProgram, "u_transform");
	        gl.glUniformMatrix4fv(transformLoc, 1, false, projectionMatrix);

	        gl.glDrawArrays(GL4.GL_POINTS, 0, vertices.size());

	        // === Render edges ===
	        gl.glUseProgram(edgesShaderProgram);

	        gl.glEnableVertexAttribArray(0);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgePoints.length * Float.BYTES, FloatBuffer.wrap(edgePoints));
	        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

	        gl.glEnableVertexAttribArray(1);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeColorBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgeColors.length * Float.BYTES, FloatBuffer.wrap(edgeColors));
	        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0);

	        gl.glEnableVertexAttribArray(2);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeSizeBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgeSizes.length * Float.BYTES, FloatBuffer.wrap(edgeSizes));
	        gl.glVertexAttribPointer(2, 1, GL4.GL_FLOAT, false, 0, 0);

	        gl.glEnableVertexAttribArray(3);
	        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeVisibilityBuffer);
	        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) edgeVisibility.length * Float.BYTES, FloatBuffer.wrap(edgeVisibility));
	        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

	        int transformLocEdges = gl.glGetUniformLocation(edgesShaderProgram, "u_transform");
	        gl.glUniformMatrix4fv(transformLocEdges, 1, false, projectionMatrix);

	        gl.glDrawArrays(GL4.GL_LINES, 0, edges.size() * 2);
	        gl.glFinish();

	        // Read the pixels from the framebuffer
	        ByteBuffer buffer = ByteBuffer.allocateDirect(exportWidth * exportHeight * 4).order(ByteOrder.nativeOrder());
	        gl.glReadBuffer(GL4.GL_COLOR_ATTACHMENT0);
	        gl.glReadPixels(0, 0, exportWidth, exportHeight, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, buffer);

	        // Flip the image vertically (OpenGL has origin at bottom left, most image formats at top left)
	        BufferedImage image = new BufferedImage(exportWidth, exportHeight, BufferedImage.TYPE_INT_ARGB);
	        byte[] row = new byte[exportWidth * 4];
	        for (int y = 0; y < exportHeight; y++) {
	            int rowStart = (exportHeight - 1 - y) * exportWidth * 4;
	            buffer.position(rowStart);
	            buffer.get(row);
	            for (int x = 0; x < exportWidth; x++) {
	                int i = x * 4;
	                int r = row[i] & 0xFF;
	                int g = row[i + 1] & 0xFF;
	                int b = row[i + 2] & 0xFF;
	                int a = row[i + 3] & 0xFF;
	                int argb = (a << 24) | (r << 16) | (g << 8) | b;
	                image.setRGB(x, y, argb);
	            }
	        }

	        try {
	            File outputFile = new File(path);
	            ImageIO.write(image, "png", outputFile);
	            System.out.println("Successfully exported to: " + path);
	        } catch (IOException e) {
	            System.err.println("Error writing PNG file: " + e.getMessage());
	            e.printStackTrace();
	        }

	    } catch (Exception e) {
	        System.err.println("Exception in exportToPng: " + e.getMessage());
	        e.printStackTrace();
	    } finally {
	        // Clean up resources
	        if (textureId[0] > 0) {
	            gl.glDeleteTextures(1, textureId, 0);
	        }
	        if (rbo[0] > 0) {
	            gl.glDeleteRenderbuffers(1, rbo, 0);
	        }
	        if (fbo[0] > 0) {
	            gl.glDeleteFramebuffers(1, fbo, 0);
	        }

	        // Restore original framebuffer
	        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, currentFBO[0]);

	        // Restore viewport
	        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
	    }
	}



    // -------------------------------------------------------------------------
    // Outils
    // -------------------------------------------------------------------------

    /**
     * @param mode Mode de similitude
     * @return l'identifiant du mode de similitude
     * @see GraphData.SimilitudeMode
     */
    public int getModeSimilitude(GraphData.SimilitudeMode mode) {
        int modeSimilitude = -1;
        switch (mode) {
            case GraphData.SimilitudeMode.CORRELATION -> modeSimilitude = 0;
            case GraphData.SimilitudeMode.DISTANCE_COSINE -> modeSimilitude = 1;
            case GraphData.SimilitudeMode.DISTANCE_EUCLIDIENNE -> modeSimilitude = 2;
            case GraphData.SimilitudeMode.NORME_L1 -> modeSimilitude = 3;
            case GraphData.SimilitudeMode.NORME_LINF -> modeSimilitude = 4;
            case GraphData.SimilitudeMode.KL_DIVERGENCE -> modeSimilitude = 5;
        }
        if (modeSimilitude == -1)
            throw new RuntimeException("Mode de similitude non reconnu.");
        return modeSimilitude;
    }

    /**
     * @param community Mode de détection de communautés
     * @return l'identifiant du mode de détection de communautés
     * @see GraphData.NodeCommunity
     */
    public int getModeCommunity(GraphData.NodeCommunity community) {
        int modeCommunity = -1;
        switch (community) {
            case GraphData.NodeCommunity.LOUVAIN -> modeCommunity = 0;
            case GraphData.NodeCommunity.LOUVAIN_PAR_COMPOSANTE -> modeCommunity = 1;
            case GraphData.NodeCommunity.LEIDEN -> modeCommunity = 2;
            case GraphData.NodeCommunity.LEIDEN_CPM -> modeCommunity = 3;
            case GraphData.NodeCommunity.COULEURS_SPECIALES -> modeCommunity = 4;
        }
        if (modeCommunity == -1)
            throw new RuntimeException("Mode de détection de communautés non reconnu.");
        return modeCommunity;
    }

    /**
     * @param mode Mode de répulsion
     * @return l'identifiant du mode de répulsion
     */
    public int getModeRepulsion(GraphData.RepulsionMode mode) {
        int modeRepulsion = -1;
        switch (mode) {
            case GraphData.RepulsionMode.REPULSION_BY_DEGREE -> modeRepulsion = 0;
            case GraphData.RepulsionMode.REPULSION_BY_EDGES -> modeRepulsion = 1;
            case GraphData.RepulsionMode.REPULSION_BY_COMMUNITIES -> modeRepulsion = 2;
        }
        if (modeRepulsion == -1)
            throw new RuntimeException("Mode de répulsion non reconnu.");
        return modeRepulsion;
    }

    // -------------------------------------------------------------------------
    // Buffer utilities
    // -------------------------------------------------------------------------

    public void createBuffers(GL4 gl) {
        // Créer les buffers
        int[] buffers = new int[4]; // Pour vertex, size, color et visibility
        gl.glGenBuffers(4, buffers, 0); // Génère 4 buffers à la fois

        vertexBuffer = buffers[0];
        vertexSizeBuffer = buffers[1];
        vertexColorBuffer = buffers[2];
        vertexVisibilityBuffer = buffers[3];

        // Buffer pour les positions des sommets
        FloatBuffer vertexData = FloatBuffer.wrap(vertexPoints);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) vertexData.limit() * Float.BYTES, vertexData, GL4.GL_STATIC_DRAW);

        // Buffer pour les tailles des sommets
        FloatBuffer sizeData = FloatBuffer.wrap(vertexSizes);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexSizeBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) sizeData.limit() * Float.BYTES, sizeData, GL4.GL_STATIC_DRAW);

        // Buffer pour les couleurs des sommets
        FloatBuffer colorData = FloatBuffer.wrap(vertexColors);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexColorBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) colorData.limit() * Float.BYTES, colorData, GL4.GL_STATIC_DRAW);

        // Buffer pour la visibilité des sommets
	    FloatBuffer visibilityData = FloatBuffer.wrap(vertexVisibility);
	    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexVisibilityBuffer);
	    gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) visibilityData.limit() * Float.BYTES, visibilityData, GL4.GL_STATIC_DRAW);
    }

    public void createEdgeBuffers(GL4 gl) {
        int[] buffers = new int[4];
        gl.glGenBuffers(4, buffers, 0);

        edgeBuffer = buffers[0];
        edgeColorBuffer = buffers[1];
        edgeSizeBuffer = buffers[2];
        edgeVisibilityBuffer = buffers[3];

        // La taille des buffers doit correspondre à la quantité de données
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgePoints.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeColorBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgeColors.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeSizeBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgeSizes.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeVisibilityBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgeVisibility.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
    }

    public void initializeArrays() {
        int vertexCount = vertices.size();
        int edgeCount = edges.size();

        vertexPoints = new float[vertexCount * 2]; // x, y pour chaque sommet
        vertexSizes = new float[vertexCount]; // taille pour chaque sommet
        vertexColors = new float[vertexCount * 3]; // RGB pour chaque sommet
        vertexVisibility = new float[vertexCount];   // visibilité pour chaque sommet

        // Initialisation par défaut des sommets visibles
	    for (int i = 0; i < vertexCount; i++) {
	        vertexVisibility[i] = 1.0f;
	    }

        // Pour les arêtes, nous avons 2 points par arête
        edgePoints = new float[edgeCount * 4]; // x1,y1,x2,y2 pour chaque arête
        edgeColors = new float[edgeCount * 6]; // 3 composantes (RGB) pour chaque point de l'arête
        edgeSizes = new float[edgeCount * 2]; // taille pour chaque point de l'arête
        edgeVisibility = new float[edgeCount * 2]; // visibilité pour chaque point de l'arête

        // Initialisation des valeurs par défaut
        for (int i = 0; i < edgeCount; i++) {
            edgeVisibility[i * 2] = 1.0f;
            edgeVisibility[i * 2 + 1] = 1.0f;
        }
    }

	/**
	 * Prépare les données de rendu pour les sommets
	 */
	private void prepareVertexRenderData() {
	    Vertex currentVertex;
	    Community currentCommunity;

	    for (int i = 0; i < vertices.size(); i++) {
	        currentVertex = vertices.get(i);
	        currentCommunity = currentVertex.getCommunity();  // léger nettoyage

	        // Mise à jour des buffers
	        vertexPoints[i * 2] = (float) currentVertex.getX();
	        vertexPoints[i * 2 + 1] = (float) currentVertex.getY();
	        vertexSizes[i] = (float) currentVertex.getDiameter();
	        vertexColors[i * 3] = currentCommunity.getR();
	        vertexColors[i * 3 + 1] = currentCommunity.getG();
	        vertexColors[i * 3 + 2] = currentCommunity.getB();

	        // Mise à jour de la visibilité
	        vertexVisibility[i] = (currentVertex.isDeleted() || !currentVertex.isVisible()) ? 0.0f : 1.0f;
	    }
	}

	
	/**
	 * Prépare les données de rendu pour les arêtes
	 */
	private void prepareEdgeRenderData() {
	    for (int i = 0; i < edges.size(); i++) {
	        Edge currentEdge = edges.get(i);
	        
	    	if (currentEdge.getWeight() > CORRELATION_THRESHOLD) {
		        Vertex startVertex = currentEdge.getStart();
		        Vertex endVertex = currentEdge.getEnd();
		        Community startCommunity = startVertex.getCommunity();
		        Community endCommunity = endVertex.getCommunity();
	
		        // Points de début de l'arête
		        edgePoints[i * 4] = (float) startVertex.getX();
		        edgePoints[i * 4 + 1] = (float) startVertex.getY();
		        
		        // Points de fin de l'arête
		        edgePoints[i * 4 + 2] = (float) endVertex.getX();
		        edgePoints[i * 4 + 3] = (float) endVertex.getY();
		        
		        // Couleur moyenne entre les deux communautés
		        float r = (startCommunity.getR() + endCommunity.getR()) / 2.0f;
		        float g = (startCommunity.getG() + endCommunity.getG()) / 2.0f;
		        float b = (startCommunity.getB() + endCommunity.getB()) / 2.0f;
		        
		        // Couleur pour les deux points de l'arête
		        edgeColors[i * 6] = r;     // Début R
		        edgeColors[i * 6 + 1] = g; // Début G
		        edgeColors[i * 6 + 2] = b; // Début B
		        edgeColors[i * 6 + 3] = r; // Fin R
		        edgeColors[i * 6 + 4] = g; // Fin G
		        edgeColors[i * 6 + 5] = b; // Fin B
		        
		        // Taille pour les deux points
		        float size = (float) currentEdge.getWeight();
		        edgeSizes[i * 2] = size;
		        edgeSizes[i * 2 + 1] = size;
		        
		        // Visibilité - 0.0 si supprimé, 1.0 si visible
		        boolean isHidden = 
		        	    startVertex.isDeleted() || 
		        	    endVertex.isDeleted() || 
		        	    !startVertex.isVisible() || 
		        	    !endVertex.isVisible();
		        float visibility = isHidden ? 0.0f : 1.0f;
		        edgeVisibility[i * 2] = visibility;
		        edgeVisibility[i * 2 + 1] = visibility;
		    }
	    }
	}

    // -------------------------------------------------------------------------
    // Shader utilities
    // -------------------------------------------------------------------------

    public int createShaderProgram(GL4 gl, String vertexSource, String fragmentSource) {
        // Compiler les shaders
        int vertexShader = compileShader(gl, GL4.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentSource);

        // Créer un programme shader
        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glLinkProgram(program);

        // Vérifier si le programme a bien été lié
        IntBuffer linkStatus = IntBuffer.allocate(1);
        gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, linkStatus);
        if (linkStatus.get(0) != GL4.GL_TRUE) {
            System.err.println("Erreur de liaison du programme de shaders.");
        }

        return program;
    }

    public int compileShader(GL4 gl, int type, String source) {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[] { source }, null);
        gl.glCompileShader(shader);

        // Vérification de la compilation
        IntBuffer compiled = IntBuffer.allocate(1);
        gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, compiled);
        if (compiled.get(0) != GL4.GL_TRUE) {
            System.err.println("Erreur de compilation du shader");
            System.err.println(getShaderInfoLog(gl, shader));
        }

        return shader;
    }

    public String getShaderInfoLog(GL4 gl, int shader) {
        IntBuffer logLength = IntBuffer.allocate(1);
        gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, logLength);
        byte[] log = new byte[logLength.get(0)];
        gl.glGetShaderInfoLog(shader, logLength.get(0), null, 0, log, 0);
        return new String(log);
    }

    public void updateProjectionMatrix() {
        float left = (float) (-graphWidth / 2.0 / zoomFactor + viewOffsetX);
        float right = (float) (graphWidth / 2.0 / zoomFactor + viewOffsetX);
        float bottom = (float) (-graphHeight / 2.0 / zoomFactor + viewOffsetY);
        float top = (float) (graphHeight / 2.0 / zoomFactor + viewOffsetY);

        float[] mat = new float[] {
                2f / (right - left), 0, 0, 0,
                0, 2f / (top - bottom), 0, 0,
                0, 0, -1, 0,
                -(right + left) / (right - left), -(top + bottom) / (top - bottom), 0, 1
        };

        projectionMatrix.clear();
        projectionMatrix.put(mat);
        projectionMatrix.flip();
    }

    /**
     * @see com.mongraphe.graphui.GraphSettings#setScreenSize(double, double)
     */
    @Override
    public void setScreenSize(double width, double height) {
        // TODO Auto-generated method stub

    }

}
