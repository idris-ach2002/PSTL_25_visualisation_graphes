package com.mongraphe.graphui.view;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.huskerdev.grapl.gl.GLProfile;
import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import com.mongraphe.graphui.export.OpenGLFXPngExporter;
import com.mongraphe.graphui.export.OpenGLGraphImageExporter;
import com.mongraphe.graphui.interaction.InteractionService;
import com.mongraphe.graphui.interaction.JavaFxInputHandler;
import com.mongraphe.graphui.interfaces.GraphImageExporter;
import com.mongraphe.graphui.rendering.GraphRenderer;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Surface JavaFX/OpenGLFX basée sur LWJGL, sans JogAmp ni Swing/AWT.
 *
 * <p>Cette version évite un thread Java supplémentaire pour planifier les
 * repaint. Un seul {@link AnimationTimer}, exécuté sur le pulse JavaFX, vérifie
 * s'il existe une nouvelle version de données, de positions ou un export PNG en
 * attente. Si rien n'a changé, aucun {@code GLCanvas.repaint()} n'est demandé.</p>
 *
 * <p>Le but est de réduire l'overhead observé dans les profils sous
 * {@code RenderJob}, {@code Platform.runLater}, {@code GLCanvas.fireRenderEvent}
 * et dans les chemins natifs GTK/Prism. La simulation peut publier beaucoup plus
 * vite que l'écran, le panel ne garde que la dernière version disponible.</p>
 */
public final class GraphPanel {

    private static final long TARGET_FRAME_PERIOD_NANOS = 1_000_000_000L / 60L;

    private final GLCanvas canvas;
    private final StackPane container;
    private final GraphRenderer renderer;
    private final JavaFxInputHandler inputHandler;
    private final AtomicReference<PendingPngExport> pendingPngExport = new AtomicReference<>();
    private final AtomicBoolean explicitFrameRequested = new AtomicBoolean(false);
    private final AtomicBoolean repaintQueued = new AtomicBoolean(false);

    private volatile boolean disposed = false;
    private volatile boolean renderingEnabled = false;
    private volatile long lastRequestedRenderVersion = Long.MIN_VALUE;
    private volatile long lastRequestedPositionVersion = Long.MIN_VALUE;
    private volatile long lastRequestedCameraVersion = Long.MIN_VALUE;
    private volatile long lastRequestedRendererStateVersion = Long.MIN_VALUE;

    private final AnimationTimer renderTimer = new AnimationTimer() {
        private long lastFrameNanos = 0L;

        @Override
        public void handle(long now) {
            if (disposed || !renderingEnabled) {
                return;
            }
            if (now - lastFrameNanos < TARGET_FRAME_PERIOD_NANOS) {
                return;
            }
            if (shouldRepaint()) {
                lastFrameNanos = now;
                canvas.repaint();
            }
        }
    };

    /**
     * Construit le panel OpenGLFX.
     *
     * @param renderer renderer LWJGL chargé de dessiner le graphe
     * @param interaction service d'interaction souris/clavier à connecter au canvas
     */
    public GraphPanel(GraphRenderer renderer, InteractionService interaction) {
        this.renderer = renderer;
        this.canvas = createCanvas();
        this.container = createContainer(canvas);
        this.inputHandler = new JavaFxInputHandler(interaction, this::requestFrame);

        configureCanvasEvents();
        configureInitialPaintHooks();
        inputHandler.attach(canvas);
    }

    /**
     * Crée le canvas OpenGLFX en mode LWJGL/Core Profile.
     *
     * @return canvas OpenGLFX prêt à être intégré dans JavaFX
     */
    private GLCanvas createCanvas() {
        GLCanvas.Builder.ContextDescription.New context = new GLCanvas.Builder.ContextDescription.New()
                .setProfile(GLProfile.CORE)
                .setMajorVersion(4)
                .setMinorVersion(1);

        return new GLCanvas.Builder()
                .setExecutor(LWJGLExecutor.LWJGL_MODULE)
                .setContextDescription(context)
                .setMSAA(0)
                .setFps(0.0)
                .setSwapBuffers(1)
                .build();
    }

    /**
     * Crée le conteneur JavaFX stable autour du canvas.
     *
     * @param canvas canvas OpenGLFX
     * @return conteneur exposé aux contrôleurs FXML
     */
    private StackPane createContainer(GLCanvas canvas) {
        StackPane pane = new StackPane(canvas);
        pane.setStyle("-fx-background-color: white;");
        pane.setMinSize(0, 0);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        pane.setPickOnBounds(true);
        pane.setVisible(true);
        pane.setOpacity(1.0);

        canvas.setStyle("-fx-background-color: white;");
        canvas.setMinSize(0, 0);
        canvas.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        canvas.setFocusTraversable(true);
        canvas.prefWidthProperty().bind(pane.widthProperty());
        canvas.prefHeightProperty().bind(pane.heightProperty());

        pane.widthProperty().addListener((obs, oldValue, newValue) -> requestFrame());
        pane.heightProperty().addListener((obs, oldValue, newValue) -> requestFrame());
        return pane;
    }

    /** Configure les callbacks OpenGLFX exécutés sur le thread de rendu OpenGL. */
    private void configureCanvasEvents() {
        canvas.addOnInitEvent(event -> renderer.init());

        canvas.addOnReshapeEvent(event -> {
            int w = Math.max(1, canvas.getScaledWidth());
            int h = Math.max(1, canvas.getScaledHeight());
            renderer.reshape(w, h);
            requestFrame();
        });

        canvas.addOnRenderEvent(event -> {
            int w = Math.max(1, canvas.getScaledWidth());
            int h = Math.max(1, canvas.getScaledHeight());
            renderer.reshape(w, h);
            renderer.display();

            PendingPngExport export = pendingPngExport.getAndSet(null);
            if (export != null) {
                export.run(renderer);
            }
        });

        canvas.addOnDisposeEvent(event -> renderer.dispose());
    }

    /** Demande quelques frames au moment où la surface devient visible. */
    private void configureInitialPaintHooks() {
        canvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            requestFrame();
            Platform.runLater(this::requestFrame);
            newScene.windowProperty().addListener((wObs, oldWindow, newWindow) -> {
                if (newWindow != null) {
                    newWindow.showingProperty().addListener((sObs, wasShowing, isShowing) -> {
                        if (isShowing) {
                            requestFrame();
                            Platform.runLater(this::requestFrame);
                        }
                    });
                }
            });
        });
        canvas.visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            if (isVisible) {
                requestFrame();
            }
        });
    }

    /** Détache le contenu du parent JavaFX courant. */
    public void detachContent() {
        Runnable detach = () -> {
            try {
                if (container.getParent() instanceof javafx.scene.layout.Pane pane) {
                    pane.getChildren().remove(container);
                }
            } catch (Exception ignored) {
            }
        };

        if (Platform.isFxApplicationThread()) {
            detach.run();
        } else {
            Platform.runLater(detach);
        }
    }

    /** @return exporteur PNG lié à cette surface OpenGLFX. */
    public GraphImageExporter createExporter() {
        return new OpenGLGraphImageExporter(this);
    }

    /** @return nœud JavaFX à insérer dans le FXML. */
    public Node canvas() {
        return container;
    }

    /** @return renderer associé au panel. */
    public GraphRenderer renderer() {
        return renderer;
    }

    /**
     * Démarre le scheduler de rendu borné à 60 Hz.
     *
     * La surface reste visible pendant le changement d'onglet. Le micro-lag est
     * évité en forçant quelques repaints après le démarrage et les changements de
     * taille, sans masquer le canvas par opacité nulle.
     */
    public void start() {
        if (disposed) {
            return;
        }
        renderingEnabled = true;
        container.setVisible(true);
        container.setOpacity(1.0);
        lastRequestedRenderVersion = Long.MIN_VALUE;
        lastRequestedPositionVersion = Long.MIN_VALUE;
        lastRequestedCameraVersion = Long.MIN_VALUE;
        lastRequestedRendererStateVersion = Long.MIN_VALUE;
        canvas.setFps(0.0);
        renderTimer.start();
        forceRenderBurst();
    }

    /** Arrête le scheduler de rendu sans détruire les ressources OpenGL. */
    public void stop() {
        renderingEnabled = false;
        renderTimer.stop();
        if (!disposed) {
            canvas.setFps(0.0);
        }
    }

    /** Alias utilisé par les contrôleurs pour suspendre le rendu. */
    public void pause() {
        stop();
    }

    /**
     * Indique si une frame doit être demandée à OpenGLFX.
     *
     * @return {@code true} si les données, les positions, une interaction ou un
     *         export nécessitent une nouvelle frame
     */
    private boolean shouldRepaint() {
        if (pendingPngExport.get() != null || explicitFrameRequested.getAndSet(false)) {
            updateRequestedVersions();
            return true;
        }

        long renderVersion = rendererVersion();
        long positionVersion = rendererPositionVersion();
        long cameraVersion = renderer.engineForScheduling().camera().getViewVersion();
        long stateVersion = renderer.renderStateVersion();
        if (renderVersion == lastRequestedRenderVersion
                && positionVersion == lastRequestedPositionVersion
                && cameraVersion == lastRequestedCameraVersion
                && stateVersion == lastRequestedRendererStateVersion) {
            return false;
        }
        lastRequestedRenderVersion = renderVersion;
        lastRequestedPositionVersion = positionVersion;
        lastRequestedCameraVersion = cameraVersion;
        lastRequestedRendererStateVersion = stateVersion;
        return true;
    }

    private void updateRequestedVersions() {
        lastRequestedRenderVersion = rendererVersion();
        lastRequestedPositionVersion = rendererPositionVersion();
        lastRequestedCameraVersion = renderer.engineForScheduling().camera().getViewVersion();
        lastRequestedRendererStateVersion = renderer.renderStateVersion();
    }

    private long rendererVersion() {
        return renderer.engineForScheduling().renderDataVersion();
    }

    private long rendererPositionVersion() {
        return renderer.engineForScheduling().renderPositionVersion();
    }

    /** Demande une frame sans empiler de {@code Platform.runLater}. */
    public void requestFrame() {
        explicitFrameRequested.set(true);
        queueRepaint();
    }

    public void forceRenderBurst() {
        requestFrame();
        Platform.runLater(this::requestFrame);
        Thread.ofVirtual().start(() -> {
            sleepQuietly(45);
            requestFrame();
            sleepQuietly(90);
            requestFrame();
        });
    }

    private void queueRepaint() {
        if (disposed || !renderingEnabled || !repaintQueued.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            repaintQueued.set(false);
            if (!disposed && renderingEnabled) {
                canvas.repaint();
            }
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Libère les ressources JavaFX et OpenGL associées au panel. */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        stop();

        Runnable disposeAction = () -> {
            try {
                inputHandler.detach(canvas);
            } catch (Exception ignored) {
            }
            try {
                canvas.dispose();
            } catch (Exception ignored) {
            }
        };

        if (Platform.isFxApplicationThread()) {
            disposeAction.run();
        } else {
            Platform.runLater(disposeAction);
        }
    }

    /**
     * Redimensionne la surface côté JavaFX.
     *
     * <p>Les propriétés {@code prefWidth}/{@code prefHeight} du canvas sont liées
     * au conteneur. On ne les modifie donc jamais directement pour éviter
     * l'exception JavaFX "bound value cannot be set".</p>
     *
     * @param width largeur demandée en pixels JavaFX
     * @param height hauteur demandée en pixels JavaFX
     */
    public void resize(int width, int height) {
        Runnable resizeAction = () -> {
            int w = Math.max(1, width);
            int h = Math.max(1, height);
            container.setPrefSize(w, h);
            container.resize(w, h);
            requestFrame();
            if (renderingEnabled && !disposed) {
                canvas.repaint();
            }
        };

        if (Platform.isFxApplicationThread()) {
            resizeAction.run();
        } else {
            Platform.runLater(resizeAction);
        }
    }


    /**
     * Variante historique conservée pour {@link OpenGLGraphImageExporter}.
     *
     * @param file fichier de sortie
     * @param width largeur exportée
     * @param height hauteur exportée
     */
    public void exportPngSync(File file, int width, int height) {
        exportPng(file, width, height);
    }

    /**
     * Planifie un export PNG. L'export est exécuté dans le callback de rendu afin
     * de disposer d'un contexte OpenGL courant.
     *
     * @param file fichier de sortie
     * @param width largeur exportée
     * @param height hauteur exportée
     * @return {@code true} si la demande est acceptée. Depuis le thread JavaFX,
     *         l'écriture est exécutée au prochain callback OpenGL pour ne pas
     *         bloquer le pulse de l'interface.
     */
    public boolean exportPng(File file, int width, int height) {
        if (file == null || width <= 0 || height <= 0 || disposed) {
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean(false);
        pendingPngExport.set(new PendingPngExport(file, width, height, ok, latch));
        requestFrame();

        if (Platform.isFxApplicationThread()) {
            canvas.repaint();
            return true;
        }

        Platform.runLater(canvas::repaint);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return ok.get();
    }

    /** Requête d'export PNG exécutée lorsque le contexte OpenGL est courant. */
    private static final class PendingPngExport {
        private final File file;
        private final int width;
        private final int height;
        private final AtomicBoolean ok;
        private final CountDownLatch latch;

        PendingPngExport(File file, int width, int height, AtomicBoolean ok, CountDownLatch latch) {
            this.file = file;
            this.width = width;
            this.height = height;
            this.ok = ok;
            this.latch = latch;
        }

        void run(GraphRenderer renderer) {
            try {
                new OpenGLFXPngExporter().export(renderer, file, width, height);
                ok.set(true);
            } catch (Exception ignored) {
                ok.set(false);
            } finally {
                latch.countDown();
            }
        }
    }
}
