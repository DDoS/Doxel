package ca.sapon.doxel;

import java.util.ArrayList;
import java.util.List;

import com.flowpowered.caustic.api.Camera;
import com.flowpowered.caustic.api.GLImplementation;
import com.flowpowered.caustic.api.Material;
import com.flowpowered.caustic.api.Pipeline;
import com.flowpowered.caustic.api.Pipeline.PipelineBuilder;
import com.flowpowered.caustic.api.data.ShaderSource;
import com.flowpowered.caustic.api.data.Uniform.FloatUniform;
import com.flowpowered.caustic.api.data.Uniform.Vector3Uniform;
import com.flowpowered.caustic.api.data.UniformHolder;
import com.flowpowered.caustic.api.data.VertexData;
import com.flowpowered.caustic.api.gl.Context;
import com.flowpowered.caustic.api.gl.Context.Capability;
import com.flowpowered.caustic.api.gl.Program;
import com.flowpowered.caustic.api.gl.Shader;
import com.flowpowered.caustic.api.gl.VertexArray;
import com.flowpowered.caustic.api.model.Model;
import com.flowpowered.caustic.lwjgl.LWJGLUtil;
import com.flowpowered.math.vector.Vector3f;

/**
 * Doxel is a simple rendering engine used for rendering scalar data on an aligned grid. This is can be used to render such things as coherent noise. See the source of {@link DoxelApp} (available on
 * GitHub) for an example on how to use this class. <p> Doxel requires OpenGL 3.2.
 *
 * @author DDoS
 */
public class Doxel {
    private static boolean created = false;
    private static Context context;
    private static final List<Model> models = new ArrayList<>();
    private static Material material;
    private static Pipeline pipeline;
    private static long sleepTime;
    // Lighting
    private static Vector3f lightPosition = new Vector3f(0, 0, 0);
    private static float diffuseIntensity = 0.6f;
    private static float specularIntensity = 1;
    private static float ambientIntensity = 0.1f;
    private static Vector3f backgroundColor = new Vector3f(0.2f, 0.2f, 0.2f);

    private Doxel() {
        fps(60);
    }

    /**
     * Create the render window and basic resources. This excludes the model.
     *
     * @param windowWidth The width of the render window.
     * @param windowHeight The height of the render window.
     * @param fieldOfView The field of view in degrees. At least 75 is suggested.
     */
    public static void create(int windowWidth, int windowHeight, float fieldOfView) {
        if (created) {
            throw new IllegalStateException("Doxel has already been created.");
        }
        // Context
        context = GLImplementation.get(LWJGLUtil.GL32_IMPL);
        context.setWindowSize(windowWidth, windowHeight);
        context.setWindowTitle("Doxel");
        context.create();
        context.enableCapability(Capability.DEPTH_TEST);
        context.enableCapability(Capability.DEPTH_CLAMP);
        context.setClearColor(backgroundColor.toVector4(1));
        final UniformHolder uniforms = context.getUniforms();
        uniforms.add(new Vector3Uniform("lightPosition", lightPosition));
        uniforms.add(new FloatUniform("diffuseIntensity", diffuseIntensity));
        uniforms.add(new FloatUniform("specularIntensity", specularIntensity));
        uniforms.add(new FloatUniform("ambientIntensity", ambientIntensity));
        // Vertex shader
        final Shader vertex = context.newShader();
        vertex.create();
        vertex.setSource(new ShaderSource(Doxel.class.getResourceAsStream("/doxel.vert")));
        vertex.compile();
        // Fragment shader
        final Shader fragment = context.newShader();
        fragment.create();
        fragment.setSource(new ShaderSource(Doxel.class.getResourceAsStream("/doxel.frag")));
        fragment.compile();
        // Program
        final Program program = context.newProgram();
        program.create();
        program.attachShader(vertex);
        program.attachShader(fragment);
        program.link();
        // Material
        material = new Material(program);
        // Pipeline
        pipeline = new PipelineBuilder().clearBuffer().renderModels(models).updateDisplay().build();
        // Camera
        final Camera camera = Camera.createPerspective(fieldOfView, windowWidth, windowHeight, 0.1f, 100f);
        context.setCamera(camera);
        created = true;
    }

    /**
     * Destroys the render window and the models.
     */
    public static void destroy() {
        if (!created) {
            throw new IllegalStateException("Doxel has not been created yet.");
        }
        models.clear();
        context.destroy();
        created = false;
    }

    /**
     * Displays the models with to the render window.
     *
     * @throws IllegalStateException If the display wasn't created first or if no models were added.
     */
    protected static void render() {
        if (!created) {
            throw new IllegalStateException("Display needs to be created first.");
        }
        final long start = System.nanoTime();
        pipeline.run(context);
        final long delta = Math.round((System.nanoTime() - start) / 1e6);
        try {
            Thread.sleep(Math.max(sleepTime - delta, 0));
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Returns true if the Doxel display has been created.
     *
     * @return True if the display and rendering resources have been creates, false if other wise.
     */
    public static boolean created() {
        return created;
    }

    public static void fps(int fps) {
        sleepTime = Math.round(1f / fps * 1000);
    }

    /**
     * Creates and adds a model to the list
     *
     * @param vertexData The mesh of the model
     */
    public static Model createModel(VertexData vertexData, Vector3f color) {
        if (vertexData.getIndicesCount() == 0) {
            return null;
        }
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(vertexData);
        final Model model = new Model(vertexArray, material);
        model.getUniforms().add(new Vector3Uniform("modelColor", color));
        models.add(model);
        return model;
    }

    /**
     * Removes a model from the list
     *
     * @param model The model to remove
     */
    public static void removeModel(Model model) {
        models.remove(model);
    }

    /**
     * Gets the camera position.
     *
     * @return The camera position.
     */
    public static Camera camera() {
        return context.getCamera();
    }

    /**
     * Gets the background color.
     *
     * @return The background color.
     */
    public static Vector3f backgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color.
     *
     * @param color The background color.
     */
    public static void backgroundColor(Vector3f color) {
        backgroundColor = color;
    }

    /**
     * Gets the light position.
     *
     * @return The light position.
     */
    public static Vector3f lightPosition() {
        return lightPosition;
    }

    /**
     * Sets the light position.
     *
     * @param position The light position.
     */
    public static void lightPosition(Vector3f position) {
        lightPosition = position;
        if (context != null) {
            final Vector3Uniform uniform = context.getUniforms().get("lightPosition");
            if (uniform != null) {
                uniform.set(lightPosition);
            }
        }
    }

    /**
     * Sets the diffuse intensity.
     *
     * @param intensity The diffuse intensity.
     */
    public static void diffuseIntensity(float intensity) {
        diffuseIntensity = intensity;
        if (context != null) {
            final FloatUniform uniform = context.getUniforms().get("diffuseIntensity");
            if (uniform != null) {
                uniform.set(diffuseIntensity);
            }
        }
    }

    /**
     * Gets the diffuse intensity.
     *
     * @return The diffuse intensity.
     */
    public static float diffuseIntensity() {
        return diffuseIntensity;
    }

    /**
     * Sets the specular intensity.
     *
     * @param intensity specular The intensity.
     */
    public static void specularIntensity(float intensity) {
        specularIntensity = intensity;
        if (context != null) {
            final FloatUniform uniform = context.getUniforms().get("specularIntensity");
            if (uniform != null) {
                uniform.set(specularIntensity);
            }
        }
    }

    /**
     * Gets specular intensity.
     *
     * @return The specular intensity.
     */
    public static float specularIntensity() {
        return specularIntensity;
    }

    /**
     * Sets the ambient intensity.
     *
     * @param intensity The ambient intensity.
     */
    public static void ambientIntensity(float intensity) {
        ambientIntensity = intensity;
        if (context != null) {
            final FloatUniform uniform = context.getUniforms().get("ambientIntensity");
            if (uniform != null) {
                uniform.set(ambientIntensity);
            }
        }
    }

    /**
     * Gets ambient intensity.
     *
     * @return The ambient intensity.
     */
    public static float ambientIntensity() {
        return ambientIntensity;
    }
}
