package me.ddos.doxel;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.yaml.snakeyaml.Yaml;

/**
 * The main class of this application.
 *
 * @author DDoS
 */
public class DoxelApp {
	// Settings
	private static float mouseSensitivity = 0.01f;
	private static float cameraSpeed = 0.4f;
	private static String windowTitle = "Doxel";
	private static int windowWidth = 1200;
	private static int windowHeight = 800;
	private static float fieldOfView = 75;
	private static final Vector3f modelPosition = new Vector3f();
	private static final Vector3f modelSize = new Vector3f();
	// Input
	private static final Vector2f cameraRotation = new Vector2f();
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
			Doxel.generateModelMesh(modelPosition.x, modelPosition.y, modelPosition.z,
					modelSize.x, modelSize.y, modelSize.z);
			System.out.println(" done.");
			Doxel.create(windowTitle, windowWidth, windowHeight, fieldOfView);
			Doxel.createModel();
			Mouse.setGrabbed(true);
			while (!Display.isCloseRequested()) {
				processInput();
				Doxel.doLogic();
				Doxel.render();
				Thread.sleep(50);
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
		final String nativePath = nativesDir.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativePath);
		System.setProperty("net.java.games.input.librarypath", nativePath);
	}

	@SuppressWarnings("unchecked")
	private static void loadConfiguration() throws Exception {
		try {
			final Map<String, Object> config =
					(Map<String, Object>) new Yaml().load(new FileInputStream("config.yml"));
			final Map<String, Object> input = (Map<String, Object>) config.get("Input");
			final Map<String, Object> appearance = (Map<String, Object>) config.get("Appearance");
			final Map<String, Object> model = (Map<String, Object>) config.get("Model");
			final Map<String, Object> noiseSource = (Map<String, Object>) config.get("Plugin");
			mouseSensitivity = ((Number) input.get("MouseSensitivity")).floatValue();
			cameraSpeed = ((Number) input.get("CameraSpeed")).floatValue();
			windowTitle = appearance.get("WindowTitle").toString();
			final String[] windowSize = ((String) appearance.get("WindowSize")).split(",");
			windowWidth = Integer.parseInt(windowSize[0].trim());
			windowHeight = Integer.parseInt(windowSize[1].trim());
			fieldOfView = ((Number) appearance.get("FieldOfView")).floatValue();
			Doxel.backgroundColor(parseColor(((String) appearance.get("BackgroundColor")), 0));
			Doxel.modelColor(parseColor(((String) appearance.get("ModelColor")), 1));
			Doxel.diffuseIntensity(((Number) appearance.get("DiffuseIntensity")).floatValue());
			Doxel.specularIntensity(((Number) appearance.get("SpecularIntensity")).floatValue());
			Doxel.ambientIntensity(((Number) appearance.get("AmbientIntensity")).floatValue());
			Doxel.lightAttenuation(((Number) appearance.get("LightAttenuation")).floatValue());
			parseVector(((String) model.get("ModelPosition")), modelPosition);
			parseVector(((String) model.get("ModelSize")), modelSize);
			Doxel.meshResolution(((Number) model.get("MeshResolution")).floatValue());
			pluginPath = noiseSource.get("Path").toString();
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
		cameraRotation.x += Mouse.getDY() * mouseSensitivity;
		cameraRotation.y -= Mouse.getDX() * mouseSensitivity;
		final Quaternion yaw = new Quaternion();
		yaw.setFromAxisAngle(new Vector4f(1, 0, 0, cameraRotation.x));
		final Quaternion pitch = new Quaternion();
		pitch.setFromAxisAngle(new Vector4f(0, 1, 0, cameraRotation.y));
		Quaternion.mul(pitch, yaw, Doxel.modelRotation());
		final Vector3f right = Doxel.cameraRight();
		final Vector3f up = Doxel.cameraUp();
		final Vector3f forward = Doxel.cameraForward();
		final Vector3f position = Doxel.modelPosition();
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			Vector3f.add(position, scale(forward, cameraSpeed), position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			Vector3f.add(position, scale(forward, -cameraSpeed), position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			Vector3f.add(position, scale(right, cameraSpeed), position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			Vector3f.add(position, scale(right, -cameraSpeed), position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			Vector3f.add(position, scale(up, cameraSpeed), position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			Vector3f.add(position, scale(up, -cameraSpeed), position);
		}
		Doxel.lightPosition(position.negate(null));
	}

	private static Vector3f scale(Vector3f v, float s) {
		return new Vector3f(v.x * s, v.y * s, v.z * s);
	}

	private static Color parseColor(String s, float alpha) {
		final String[] ss = s.split(",");
		return new Color(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()),
				alpha);
	}

	private static void parseVector(String s, Vector3f v) {
		final String[] ss = s.split(",");
		v.set(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()));
	}
}