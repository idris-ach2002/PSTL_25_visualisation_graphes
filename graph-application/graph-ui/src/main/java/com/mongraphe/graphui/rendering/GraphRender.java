import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.glsl.ShaderProgram;

public class GraphRender implements GLEventListener {

    private final GraphScene scene;
    private final GraphSimulation simulation;
    private final GraphVisibilityFilter visibility;
    private final VertexGpuBuffer vertexGpu;
    private final EdgeGpuBuffer edgeGpu;
    private final Camera2D camera;
    private final ShaderProgram pointsShader;
    private final ShaderProgram edgesShader;

    public GraphRender(GraphScene scene,
            GraphSimulation simulation,
            GraphVisibilityFilter visibility,
            VertexGpuBuffer vertexGpu,
            EdgeGpuBuffer edgeGpu,
            Camera2D camera,
            ShaderProgram pointsShader,
            ShaderProgram edgesShader) {

        this.scene = scene;
        this.simulation = simulation;
        this.visibility = visibility;
        this.vertexGpu = vertexGpu;
        this.edgeGpu = edgeGpu;
        this.camera = camera;
        this.pointsShader = pointsShader;
        this.edgesShader = edgesShader;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glEnable(GL4.GL_DEPTH_TEST);

        vertexGpu.init(gl);
        edgeGpu.init(gl);
        pointsShader.init(gl);
        edgesShader.init(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        simulation.update(scene);
        visibility.apply(scene);

        vertexGpu.updateCpu(scene);
        edgeGpu.updateCpu(scene);

        vertexGpu.upload(gl);
        edgeGpu.upload(gl);

        pointsShader.bind(gl);
        pointsShader.setMatrix(gl, "u_transform", camera.getProjection());
        vertexGpu.draw(gl);

        edgesShader.bind(gl);
        edgesShader.setMatrix(gl, "u_transform", camera.getProjection());
        edgeGpu.draw(gl);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        camera.resize(w, h);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }
}