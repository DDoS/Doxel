package me.ddos.doxel;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import me.ddos.doxel.math.Quaternion;
import me.ddos.doxel.math.Vector3;
import me.ddos.doxel.polygonizer.MarchingCubesPolygonizer;
import me.ddos.doxel.polygonizer.Polygonizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
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
	private static float mouseSensitivity = 0.1f;
	private static float cameraSpeed = 0.4f;
	private static String windowTitle = "Doxel";
	private static int windowWidth = 1200;
	private static int windowHeight = 800;
	private static float fieldOfView = 75;
	private static float meshResolution = 0.5f;
	private static Color modelColor;
	// Model data
	private static NoiseSource noiseSource;
	private static Polygonizer polygonizer = new MarchingCubesPolygonizer();
	private static Vector3 modelPosition = new Vector3();
	private static Vector3 modelSize = new Vector3();
	// Input
	private static boolean mouseGrabbed = true;
	private static float cameraPitch = 0;
	private static float cameraYaw = 0;
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
			deploy();
			loadConfiguration();
			loadPlugin();
			if (plugin != null) {
				plugin.load();
			}
			System.out.print("Generating the model mesh from the noise source...");
			Model model = polygonizer.createModel(noiseSource, meshResolution, modelPosition, modelSize);
			System.out.println(" done.");
			Doxel.create(windowTitle, windowWidth, windowHeight, fieldOfView);
			model.position(modelPosition);
			model.color(modelColor);
			model.create();
			Doxel.addModel(model);
			Mouse.setGrabbed(true);
			while (!Display.isCloseRequested()) {
				final long start = System.nanoTime();
				processInput();
				Doxel.render();
				Thread.sleep(Math.max(50 - (long) Math.round((System.nanoTime() - start) / 1000000d), 0));
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

	/**
	 * Sets the noise source used to generate the model mesh.
	 *
	 * @param source The source to mesh for visualization.
	 */
	public static void noiseSource(NoiseSource source) {
		noiseSource = source;
	}

	/**
	 * Sets the polygonizer to use.
	 *
	 * @param polygonizer The polygonizer to use.
	 */
	public static void polygonizer(Polygonizer polygonizer) {
		DoxelApp.polygonizer = polygonizer;
	}

	private static void deploy() throws Exception {
		final File configFile = new File("config.yml");
		if (!configFile.exists()) {
			FileUtils.copyInputStreamToFile(DoxelApp.class.getResourceAsStream("/config.yml"), configFile);
		}
		final String osPath;
		final String[] nativeLibs;
		if (SystemUtils.IS_OS_WINDOWS) {
			nativeLibs = new String[]{
				"jinput-dx8_64.dll", "jinput-dx8.dll", "jinput-raw_64.dll", "jinput-raw.dll",
				"jinput-wintab.dll", "lwjgl.dll", "lwjgl64.dll", "OpenAL32.dll", "OpenAL64.dll"
			};
			osPath = "windows/";
		} else if (SystemUtils.IS_OS_MAC) {
			nativeLibs = new String[]{
				"libjinput-osx.jnilib", "liblwjgl.jnilib", "openal.dylib"
			};
			osPath = "mac/";
		} else if (SystemUtils.IS_OS_LINUX) {
			nativeLibs = new String[]{
				"liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so", "libjinput-linux.so",
				"libjinput-linux64.so"
			};
			osPath = "linux/";
		} else {
			throw new IllegalStateException("Could not get lwjgl natives for OS \"" + SystemUtils.OS_NAME + "\".");
		}
		final File nativesDir = new File("natives" + File.separator + osPath);
		nativesDir.mkdirs();
		for (String nativeLib : nativeLibs) {
			final File nativeFile = new File(nativesDir, nativeLib);
			if (!nativeFile.exists()) {
				FileUtils.copyInputStreamToFile(DoxelApp.class.getResourceAsStream("/" + nativeLib), nativeFile);
			}
		}
		final String nativesPath = nativesDir.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativesPath);
		System.setProperty("net.java.games.input.librarypath", nativesPath);
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
			windowTitle = appearanceConfig.get("WindowTitle").toString();
			final String[] windowSize = ((String) appearanceConfig.get("WindowSize")).split(",");
			windowWidth = Integer.parseInt(windowSize[0].trim());
			windowHeight = Integer.parseInt(windowSize[1].trim());
			fieldOfView = ((Number) appearanceConfig.get("FieldOfView")).floatValue();
			Doxel.backgroundColor(parseColor(((String) appearanceConfig.get("BackgroundColor")), 0));
			modelColor = (parseColor(((String) appearanceConfig.get("ModelColor")), 1));
			Doxel.diffuseIntensity(((Number) appearanceConfig.get("DiffuseIntensity")).floatValue());
			Doxel.specularIntensity(((Number) appearanceConfig.get("SpecularIntensity")).floatValue());
			Doxel.ambientIntensity(((Number) appearanceConfig.get("AmbientIntensity")).floatValue());
			Doxel.lightAttenuation(((Number) appearanceConfig.get("LightAttenuation")).floatValue());
			modelPosition = parseVector3(((String) modelConfig.get("ModelPosition")));
			modelSize = parseVector3(((String) modelConfig.get("ModelSize")));
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
		if (mouseGrabbed) {
			cameraYaw += Mouse.getDY() * mouseSensitivity;
			cameraPitch -= Mouse.getDX() * mouseSensitivity;
			final Quaternion yaw = new Quaternion(cameraYaw, 1, 0, 0);
			final Quaternion pitch = new Quaternion(cameraPitch, 0, 1, 0);
			Doxel.cameraRotation(pitch.mul(yaw));
		}
		final Vector3 right = Doxel.cameraRight();
		final Vector3 up = Doxel.cameraUp();
		final Vector3 forward = Doxel.cameraForward();
		Vector3 position = Doxel.cameraPosition();
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			position = position.add(forward.scale(cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			position = position.add(forward.scale(-cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			position = position.add(right.scale(cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			position = position.add(right.scale(-cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			position = position.add(up.scale(cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			position = position.add(up.scale(-cameraSpeed));
		}
		Doxel.cameraPosition(position);
		Doxel.lightPosition(position.negate());
	}

	private static Color parseColor(String s, float alpha) {
		final String[] ss = s.split(",");
		return new Color(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()),
				alpha);
	}

	private static Vector3 parseVector3(String s) {
		final String[] ss = s.split(",");
		return new Vector3(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()));
	}
}
