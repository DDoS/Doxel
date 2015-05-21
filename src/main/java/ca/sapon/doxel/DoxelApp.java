package ca.sapon.doxel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import com.flowpowered.caustic.api.Camera;
import com.flowpowered.caustic.api.data.VertexData;
import com.flowpowered.caustic.lwjgl.LWJGLUtil;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;

import ca.sapon.doxel.polygonizer.MarchingCubesPolygonizer;
import ca.sapon.doxel.polygonizer.Polygonizer;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.yaml.snakeyaml.Yaml;

/**
 * The main class of this application.
 *
 * @author DDoS
 */
public class DoxelApp {
    // Settings
    private static int fps = 60;
    private static float mouseSensitivity = 0.1f;
    private static float cameraSpeed = 0.4f;
    private static int windowWidth = 1200;
    private static int windowHeight = 800;
    private static float fieldOfView = 75;
    private static float meshResolution = 0.5f;
    private static Vector3f modelColor;
    // Model data
    private static NoiseSource noiseSource;
    private static Polygonizer polygonizer = new MarchingCubesPolygonizer();
    private static Vector3f modelPosition = Vector3f.ZERO;
    private static Vector3f modelSize = Vector3f.ONE;
    // Input
    private static boolean mouseGrabbed = true;
    private static float cameraPitch = 0;
    private static float cameraYaw = 0;
    private static long lastUpdateTime = -1;
    // Plugin
    private static String pluginPath;
    private static DoxelPlugin plugin;

    /**
     * The entry point for the application.
     *
     * @param args Unused.
     */
    public static void main(String[] args) {
        try {
            LWJGLUtil.deployNatives(null);
            loadConfiguration();
            loadPlugin();
            if (plugin != null) {
                plugin.load();
            }
            System.out.print("Generating the model mesh from the noise source...");
            final VertexData mesh = polygonizer.createMesh(noiseSource, meshResolution, modelPosition, modelSize);
            System.out.println(" done.");
            Doxel.create(windowWidth, windowHeight, fieldOfView);
            if (Doxel.createModel(mesh, modelColor) == null) {
                System.out.println("Generated mesh is empty.");
            }
            Doxel.fps(fps);
            Mouse.setGrabbed(true);
            while (!Display.isCloseRequested()) {
                processInput();
                if (plugin != null) {
                    plugin.preRender();
                }
                Doxel.render();
                if (plugin != null) {
                    plugin.postRender();
                }
            }
            if (plugin != null) {
                plugin.unload();
            }
            Mouse.setGrabbed(false);
            Doxel.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
            final String name = ex.getClass().getSimpleName();
            final String message = ex.getMessage();
            Sys.alert("Error: " + name, message == null || message.trim().equals("") ? name : message);
            System.exit(-1);
        }
    }

    private static void processInput() {
        final boolean mouseGrabbedBefore = mouseGrabbed;
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
                    mouseGrabbed ^= true;
                }
            }
        }
        if (mouseGrabbed != mouseGrabbedBefore) {
            Mouse.setGrabbed(mouseGrabbed);
        }
        final long currentTime = System.nanoTime();
        final float timeCoefficient = (currentTime - lastUpdateTime) / (1f / fps * 1e9f);
        if (lastUpdateTime < 0) {
            lastUpdateTime = currentTime;
            return;
        } else {
            lastUpdateTime = currentTime;
        }
        final Camera camera = Doxel.camera();
        if (mouseGrabbed) {
            final float correctedMouseSensitivity = mouseSensitivity * timeCoefficient;
            cameraYaw += Mouse.getDY() * correctedMouseSensitivity;
            cameraPitch -= Mouse.getDX() * correctedMouseSensitivity;
            final Quaternionf yaw = Quaternionf.fromAngleDegAxis(cameraYaw, 1, 0, 0);
            final Quaternionf pitch = Quaternionf.fromAngleDegAxis(cameraPitch, 0, 1, 0);
            camera.setRotation(pitch.mul(yaw));
        }
        final Vector3f right = camera.getRight();
        final Vector3f up = camera.getUp();
        final Vector3f forward = camera.getForward();
        Vector3f position = camera.getPosition();
        final float correctedCameraSpeed = cameraSpeed * timeCoefficient;
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            position = position.add(forward.mul(correctedCameraSpeed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            position = position.add(forward.mul(-correctedCameraSpeed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            position = position.add(right.mul(correctedCameraSpeed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            position = position.add(right.mul(-correctedCameraSpeed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            position = position.add(up.mul(correctedCameraSpeed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            position = position.add(up.mul(-correctedCameraSpeed));
        }
        camera.setPosition(position);
        Doxel.lightPosition(position.negate());
    }

    /**
     * Sets the noise source used to generate the model mesh.
     *
     * @param source The source to mesh for visualization.
     */
    public static void noiseSource(NoiseSource source) {
        noiseSource = source;
    }

    /**
     * Gets the polygonizer in use.
     *
     * @return The polygonizer in use.
     */
    public static Polygonizer polygonizer() {
        return polygonizer;
    }

    /**
     * Sets the polygonizer to use.
     *
     * @param polygonizer The polygonizer to use.
     */
    public static void polygonizer(Polygonizer polygonizer) {
        DoxelApp.polygonizer = polygonizer;
    }

    @SuppressWarnings("unchecked")
    private static void loadConfiguration() throws Exception {
        try {
            final Map<String, Object> config =
                    (Map<String, Object>) new Yaml().load(new FileInputStream("config.yml"));
            final Map<String, Object> inputConfig = (Map<String, Object>) config.get("Input");
            final Map<String, Object> appearanceConfig = (Map<String, Object>) config.get("Appearance");
            final Map<String, Object> modelConfig = (Map<String, Object>) config.get("Model");
            final Map<String, Object> noiseSourceConfig = (Map<String, Object>) config.get("Plugin");
            mouseSensitivity = ((Number) inputConfig.get("MouseSensitivity")).floatValue();
            cameraSpeed = ((Number) inputConfig.get("CameraSpeed")).floatValue();
            final String[] windowSize = ((String) appearanceConfig.get("WindowSize")).split(",");
            windowWidth = Integer.parseInt(windowSize[0].trim());
            windowHeight = Integer.parseInt(windowSize[1].trim());
            fps = ((Number) appearanceConfig.get("FPS")).intValue();
            fieldOfView = ((Number) appearanceConfig.get("FieldOfView")).floatValue();
            Doxel.backgroundColor(parseVector3f(((String) appearanceConfig.get("BackgroundColor"))));
            modelColor = (parseVector3f(((String) appearanceConfig.get("ModelColor"))));
            Doxel.diffuseIntensity(((Number) appearanceConfig.get("DiffuseIntensity")).floatValue());
            Doxel.specularIntensity(((Number) appearanceConfig.get("SpecularIntensity")).floatValue());
            Doxel.ambientIntensity(((Number) appearanceConfig.get("AmbientIntensity")).floatValue());
            modelPosition = parseVector3f(((String) modelConfig.get("ModelPosition")));
            modelSize = parseVector3f(((String) modelConfig.get("ModelSize")));
            meshResolution = (((Number) modelConfig.get("MeshResolution")).floatValue());
            pluginPath = noiseSourceConfig.get("Path").toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Malformed config.yml: \"" + ex.getMessage() + "\".");
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadPlugin() throws Exception {
        final File pluginFile = new File(pluginPath);
        if (!pluginFile.exists()) {
            throw new IllegalStateException("Plugin \"" + pluginPath + "\" could not be found.");
        }
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{pluginFile.toURI().toURL()});
        final InputStream infoStream = classLoader.getResourceAsStream("plugin.yml");
        if (infoStream == null) {
            throw new IllegalStateException("Plugin \"" + pluginPath + "\" is missing its plugin.yml.");
        }
        final Map info = (Map) new Yaml().load(infoStream);
        if (!info.containsKey("MainClass")) {
            throw new IllegalStateException("Plugin \"" + pluginPath + "\" has an invalid plugin.yml.");
        }
        final String mainClassName = info.get("MainClass").toString();
        final Object mainClass = classLoader.loadClass(mainClassName).newInstance();
        if (!(mainClass instanceof DoxelPlugin)) {
            throw new IllegalStateException("Main class \'" + mainClassName + "\" from plugin \""
                    + pluginPath + "\" does not implement \"DoxelPlugin\".");
        }
        plugin = (DoxelPlugin) mainClass;
    }

    private static Vector3f parseVector3f(String s) {
        final String[] ss = s.split(",");
        return new Vector3f(
                Float.parseFloat(ss[0].trim()),
                Float.parseFloat(ss[1].trim()),
                Float.parseFloat(ss[2].trim()));
    }
}
