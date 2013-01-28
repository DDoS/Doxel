package me.ddos.doxel;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Doxel is a simple rendering engine used for rendering scalar data on an aligned grid. This is can
 * be used to render such things as coherent noise. To polygonize the scalar data, the marching
 * cubes algorithm is used by default, but other polygonizers can be used as long as they implement {@link Polygonizer}.
 * Noise sources must implement {@link NoiseSource} to be used. See the source of {@link DoxelApp}
 * (available on GitHub) for an example on how to use this class. <p> Doxel requires OpenGL 3.2.
 *
 * @author DDoS
 */
public class Doxel {
	// States
	private static boolean created = false;
	// Renderer data
	private static int windowWidth;
	private static int windowHeight;
	private static int vertexShaderID = 0;
	private static int fragementShaderID = 0;
	private static int programID = 0;
	// Model data
	private static final List<Model> models = new ArrayList<Model>();
	// Shader data
	private static int modelMatrixLocation;
	private static int cameraMatrixLocation;
	private static int projectionMatrixLocation;
	private static int modelColorLocation;
	private static int diffuseIntensityLocation;
	private static int specularIntensityLocation;
	private static int ambientIntensityLocation;
	private static int lightPositionLocation;
	private static int lightAttenuationLocation;
	// Camera
	private static final Matrix4f projectionMatrix = new Matrix4f();
	private static Vector3f cameraPosition = new Vector3f(0, 0, 0);
	private static Quaternion cameraRotation = new Quaternion();
	private static final Matrix4f cameraRotationMatrix = new Matrix4f();
	private static final Matrix4f cameraPositionMatrix = new Matrix4f();
	private static final Matrix4f cameraMatrix = new Matrix4f();
	private static boolean updateCameraMatrix = true;
	// Lighting
	private static Vector3f lightPosition = new Vector3f(0, 0, 0);
	private static float diffuseIntensity = 0.9f;
	private static float specularIntensity = 1;
	private static float ambientIntensity = 0.1f;
	private static Color backgroundColor = new Color(0.2f, 0.2f, 0.2f, 0);
	private static float lightAttenuation = 0.9f;

	private Doxel() {
	}

	/**
	 * Create the render window and basic resources. This excludes the model.
	 *
	 * @param title The title of the render window.
	 * @param windowWidth The width of the render window.
	 * @param windowHeight The height of the render window.
	 * @param fieldOfView The field of view in degrees. 75 is suggested.
	 */
	public static void create(String title, int windowWidth, int windowHeight, float fieldOfView)
			throws LWJGLException {
		if (created) {
			throw new IllegalStateException("Doxel has already been created.");
		}
		createDisplay(title, windowWidth, windowHeight);
		createProjection(fieldOfView);
		createShaders();
		created = true;
	}

	/**
	 * Destroys the render window and the models.
	 */
	public static void destroy() {
		if (!created) {
			throw new IllegalStateException("Doxel has not been created yet.");
		}
		destroyModels();
		destroyShaders();
		destroyDisplay();
		created = false;
	}

	private static void createDisplay(String title, int width, int height) throws LWJGLException {
		windowWidth = width;
		windowHeight = height;
		final PixelFormat pixelFormat = new PixelFormat();
		final ContextAttribs contextAtrributes = new ContextAttribs(3, 2).withProfileCore(true);
		Display.setDisplayMode(new DisplayMode(width, height));
		Display.setTitle(title);
		Display.create(pixelFormat, contextAtrributes);
		GL11.glViewport(0, 0, width, height);
		GL11.glClearColor(backgroundColor.getRed() / 255f, backgroundColor.getGreen() / 255f,
				backgroundColor.getBlue() / 255f, backgroundColor.getAlpha() / 255f);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL32.GL_DEPTH_CLAMP);
		GL11.glDepthMask(true);
		checkForOpenGLError("createDisplay");
	}

	private static void createProjection(float fieldOfView) {
		final float aspectRatio = windowWidth / windowHeight;
		final float near_plane = 0;
		final float far_plane = 100;
		final float y_scale = (float) (1 / Math.tan(Math.toRadians(fieldOfView / 2)));
		final float x_scale = y_scale / aspectRatio;
		final float frustum_length = far_plane - near_plane;
		projectionMatrix.m00 = x_scale;
		projectionMatrix.m11 = y_scale;
		projectionMatrix.m22 = -((far_plane + near_plane) / frustum_length);
		projectionMatrix.m23 = -1;
		projectionMatrix.m32 = -((2 * near_plane * far_plane) / frustum_length);
	}

	private static void createShaders() {
		vertexShaderID = loadShader("Doxel Vertex", Doxel.class.getResourceAsStream("/doxel.vert"),
				GL20.GL_VERTEX_SHADER);
		fragementShaderID = loadShader("Doxel Fragment", Doxel.class.getResourceAsStream("/doxel.frag"),
				GL20.GL_FRAGMENT_SHADER);
		programID = GL20.glCreateProgram();
		GL20.glAttachShader(programID, vertexShaderID);
		GL20.glAttachShader(programID, fragementShaderID);
		GL20.glLinkProgram(programID);
		modelMatrixLocation = GL20.glGetUniformLocation(programID, "modelMatrix");
		cameraMatrixLocation = GL20.glGetUniformLocation(programID, "cameraMatrix");
		projectionMatrixLocation = GL20.glGetUniformLocation(programID, "projectionMatrix");
		modelColorLocation = GL20.glGetUniformLocation(programID, "modelColor");
		diffuseIntensityLocation = GL20.glGetUniformLocation(programID, "diffuseIntensity");
		specularIntensityLocation = GL20.glGetUniformLocation(programID, "specularIntensity");
		ambientIntensityLocation = GL20.glGetUniformLocation(programID, "ambientIntensity");
		lightPositionLocation = GL20.glGetUniformLocation(programID, "lightPosition");
		lightAttenuationLocation = GL20.glGetUniformLocation(programID, "lightAttenuation");
		GL20.glValidateProgram(programID);
		checkForOpenGLError("createShaders");
	}

	private static void destroyDisplay() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_CULL_FACE);
		checkForOpenGLError("destroyDisplay");
		Display.destroy();
	}

	private static void destroyShaders() {
		GL20.glUseProgram(0);
		GL20.glDetachShader(programID, vertexShaderID);
		GL20.glDetachShader(programID, fragementShaderID);
		GL20.glDeleteShader(vertexShaderID);
		GL20.glDeleteShader(fragementShaderID);
		GL20.glDeleteProgram(programID);
		checkForOpenGLError("destroyShaders");
	}

	private static void destroyModels() {
		for (Model model : models) {
			model.destroy();
		}
		models.clear();
	}

	private static Matrix4f cameraMatrix() {
		if (updateCameraMatrix) {
			cameraRotationMatrix.setIdentity();
			cameraRotationMatrix.m00 = 1 - 2 * cameraRotation.y * cameraRotation.y - 2 * cameraRotation.z * cameraRotation.z;
			cameraRotationMatrix.m01 = 2 * cameraRotation.x * cameraRotation.y - 2 * cameraRotation.w * cameraRotation.z;
			cameraRotationMatrix.m02 = 2 * cameraRotation.x * cameraRotation.z + 2 * cameraRotation.w * cameraRotation.y;
			cameraRotationMatrix.m03 = 0;
			cameraRotationMatrix.m10 = 2 * cameraRotation.x * cameraRotation.y + 2 * cameraRotation.w * cameraRotation.z;
			cameraRotationMatrix.m11 = 1 - 2 * cameraRotation.x * cameraRotation.x - 2 * cameraRotation.z * cameraRotation.z;
			cameraRotationMatrix.m12 = 2 * cameraRotation.y * cameraRotation.z - 2 * cameraRotation.w * cameraRotation.x;
			cameraRotationMatrix.m13 = 0;
			cameraRotationMatrix.m20 = 2 * cameraRotation.x * cameraRotation.z - 2 * cameraRotation.w * cameraRotation.y;
			cameraRotationMatrix.m21 = 2 * cameraRotation.y * cameraRotation.z + 2.f * cameraRotation.x * cameraRotation.w;
			cameraRotationMatrix.m22 = 1 - 2 * cameraRotation.x * cameraRotation.x - 2 * cameraRotation.y * cameraRotation.y;
			cameraRotationMatrix.m23 = 0;
			cameraPositionMatrix.setIdentity();
			Matrix4f.translate(cameraPosition, cameraPositionMatrix, cameraPositionMatrix);
			Matrix4f.mul(cameraRotationMatrix, cameraPositionMatrix, cameraMatrix);
			updateCameraMatrix = false;
		}
		return cameraMatrix;
	}

	private static void shaderData() {
		final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
		cameraMatrix().store(matrixBuffer);
		matrixBuffer.flip();
		GL20.glUniformMatrix4(cameraMatrixLocation, false, matrixBuffer);
		matrixBuffer.clear();
		projectionMatrix.store(matrixBuffer);
		matrixBuffer.flip();
		GL20.glUniformMatrix4(projectionMatrixLocation, false, matrixBuffer);
		GL20.glUniform1f(diffuseIntensityLocation, diffuseIntensity);
		GL20.glUniform1f(specularIntensityLocation, specularIntensity);
		GL20.glUniform1f(ambientIntensityLocation, ambientIntensity);
		GL20.glUniform3f(lightPositionLocation, lightPosition.x, lightPosition.y, lightPosition.z);
		GL20.glUniform1f(lightAttenuationLocation, lightAttenuation);
		checkForOpenGLError("preRender");
	}

	private static void shaderData(Model model) {
		final FloatBuffer matrix44Buffer = BufferUtils.createFloatBuffer(16);
		model.matrix().store(matrix44Buffer);
		matrix44Buffer.flip();
		GL20.glUniformMatrix4(modelMatrixLocation, false, matrix44Buffer);
		GL20.glUniform4f(modelColorLocation,
				model.color().getRed() / 255f, model.color().getGreen() / 255f,
				model.color().getBlue() / 255f, model.color().getAlpha() / 255f);
		checkForOpenGLError("preRenderModel");
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
		if (models.isEmpty()) {
			throw new IllegalStateException("At least one model needs to be created first.");
		}
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL20.glUseProgram(programID);
		shaderData();
		for (Model model : models) {
			if (!model.created()) {
				continue;
			}
			shaderData(model);
			model.render();
		}
		GL20.glUseProgram(0);
		Display.sync(60);
		Display.update();
		checkForOpenGLError("render");
	}

	/**
	 * Returns true if the Doxel display has been created.
	 *
	 * @return True if the display and rendering resources have been creates, false if other wise.
	 */
	public static boolean created() {
		return created;
	}

	/**
	 * Adds a model to the list. If a non-created model is added to the list, it will not be
	 * rendered until it is created.
	 *
	 * @param model The model to add
	 */
	public static void addModel(Model model) {
		if (!models.contains(model)) {
			models.add(model);
		}
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
	public static Vector3f cameraPosition() {
		updateCameraMatrix = true;
		return cameraPosition;
	}

	/**
	 * Sets the camera position.
	 *
	 * @param position The camera position.
	 */
	public static void cameraPosition(Vector3f position) {
		cameraPosition = position;
		updateCameraMatrix = true;
	}

	/**
	 * Gets the camera rotation.
	 *
	 * @return The camera rotation.
	 */
	public static Quaternion cameraRotation() {
		updateCameraMatrix = true;
		return cameraRotation;
	}

	/**
	 * Sets the camera rotation.
	 *
	 * @param rotation The camera rotation.
	 */
	public static void cameraRotation(Quaternion rotation) {
		cameraRotation = rotation;
		updateCameraMatrix = true;
	}

	/**
	 * Gets the vector representing the right direction for the camera.
	 *
	 * @return The camera's right direction vector.
	 */
	public static Vector3f cameraRight() {
		return toCamrera(new Vector3f(1, 0, 0));
	}

	/**
	 * Gets the vector representing the up direction for the camera.
	 *
	 * @return The camera's up direction vector.
	 */
	public static Vector3f cameraUp() {
		return toCamrera(new Vector3f(0, 1, 0));
	}

	/**
	 * Gets the vector representing the forward direction for the camera.
	 *
	 * @return The camera's forward direction vector.
	 */
	public static Vector3f cameraForward() {
		return toCamrera(new Vector3f(0, 0, 1));
	}

	/**
	 * Gets the background color.
	 *
	 * @return The background color.
	 */
	public static Color backgroundColor() {
		return backgroundColor;
	}

	/**
	 * Sets the background color.
	 *
	 * @param color The background color.
	 */
	public static void backgroundColor(Color color) {
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
	}

	/**
	 * Sets the diffuse intensity.
	 *
	 * @param intensity The diffuse intensity.
	 */
	public static void diffuseIntensity(float intensity) {
		diffuseIntensity = intensity;
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
	}

	/**
	 * Gets the ambient intensity.
	 *
	 * @return The ambient intensity.
	 */
	public static float ambientIntensity() {
		return ambientIntensity;
	}

	/**
	 * Gets the light distance attenuation factor. In other terms, how much distance affects light
	 * intensity. Larger values affect it more. 0.9 is the default value.
	 *
	 * @return The light distance attenuation factor.
	 */
	public static float lightAttenuation() {
		return lightAttenuation;
	}

	/**
	 * Sets the light distance attenuation factor. In other terms, how much distance affects light
	 * intensity. Larger values affect it more. 0.12 is the default value.
	 *
	 * @param attenuation The light distance attenuation factor.
	 */
	public static void lightAttenuation(float attenuation) {
		lightAttenuation = attenuation;
	}

	/**
	 * Checks for an OpenGL exception. If one is found, this method will throw a {@link org.lwjgl.opengl.OpenGLException}
	 * which can be caught and handled.
	 *
	 * @param step The rendering step at which this method is being called.
	 */
	protected static void checkForOpenGLError(String step) {
		final int errorValue = GL11.glGetError();
		if (errorValue != GL11.GL_NO_ERROR) {
			throw new OpenGLException("OPEN GL ERROR: " + step + ": " + GLU.gluErrorString(errorValue));
		}
	}

	private static int loadShader(String name, InputStream shaderRessource, int type) {
		final StringBuilder shaderSource = new StringBuilder();
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(shaderRessource));
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
			shaderRessource.close();
		} catch (IOException e) {
			System.out.println("IO exception: " + e.getMessage());
		}
		final int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShader(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			throw new OpenGLException("OPEN GL ERROR: Could not compile shader \"" + name + "\"\n"
					+ GL20.glGetShaderInfoLog(shaderID, 1000));
		}
		checkForOpenGLError("loadShader");
		return shaderID;
	}

	private static Vector3f toCamrera(Vector3f v) {
		return transform(Matrix4f.invert(cameraRotationMatrix, null), v);
	}

	private static Vector3f transform(Matrix4f m, Vector3f v) {
		final Vector4f v4 = new Vector4f(v.x, v.y, v.z, 1);
		Matrix4f.transform(m, v4, v4);
		v.set(v4.x, v4.y, v4.z);
		return v;
	}
}
