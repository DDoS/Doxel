package me.ddos.doxel;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import org.apache.commons.io.FileUtils;
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

	/**
	 * The entry point for the application.
	 *
	 * @param args Unused.
	 */
	public static void main(String[] args) throws Exception {
		try {
			loadConfiguration();
			Doxel.generateModelMesh(modelPosition.x, modelPosition.y, modelPosition.z,
					modelSize.x, modelSize.y, modelSize.z);
			Doxel.create(windowTitle, windowWidth, windowHeight, fieldOfView);
			Doxel.createModel();
			Mouse.setGrabbed(true);
			while (!Display.isCloseRequested()) {
				processInput();
				Doxel.logic();
				Doxel.render();
				Thread.sleep(50);

			}
			Mouse.setGrabbed(false);
			Doxel.destroy();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	private static void loadConfiguration() throws Exception {
		final File configFile = new File("config.yml");
		if (!configFile.exists()) {
			FileUtils.copyURLToFile(DoxelApp.class.getResource("/config.yml"), configFile);
		}
		final Yaml yaml = new Yaml();
		final Map<String, Object> config = (Map<String, Object>) yaml.load(new FileInputStream(configFile));
		final Map<String, Object> input = (Map<String, Object>) config.get("Input");
		final Map<String, Object> appearance = (Map<String, Object>) config.get("Appearance");
		final Map<String, Object> model = (Map<String, Object>) config.get("Model");
		mouseSensitivity = ((Number) input.get("MouseSensitivity")).floatValue();
		cameraSpeed = ((Number) input.get("CameraSpeed")).floatValue();
		windowTitle = (String) appearance.get("WindowTitle");
		final String[] windowSize = ((String) appearance.get("WindowSize")).split(",");
		windowWidth = Integer.parseInt(windowSize[0].trim());
		windowHeight = Integer.parseInt(windowSize[1].trim());
		fieldOfView = ((Number) appearance.get("FieldOfView")).floatValue();
		Doxel.backgroundColor(parseColor(((String) appearance.get("BackgroundColor")), 0));
		Doxel.modelColor(parseColor(((String) appearance.get("ModelColor")), 1));
		Doxel.lightColor(parseColor(((String) appearance.get("LightIntensity")), 1));
		Doxel.ambientLightColor(parseColor(((String) appearance.get("AmbientIntensity")), 1));
		Doxel.lightAttenuation(((Number) appearance.get("LightAttenuation")).floatValue());
		parseVector(((String) model.get("ModelPosition")), modelPosition);
		parseVector(((String) model.get("ModelSize")), modelSize);
		Doxel.meshResolution(((Number) model.get("MeshResolution")).floatValue());
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