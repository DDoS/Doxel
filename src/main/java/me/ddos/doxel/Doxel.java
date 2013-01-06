package me.ddos.doxel;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
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
	// Vertex info
	private static final byte POSITION_COMPONENT_COUNT = 3;
	private static final byte NORMAL_COMPONENT_COUNT = 3;
	// States
	private static boolean isDisplayCreated = false;
	private static boolean isModelCreated = false;
	private static boolean logicRanOnce = false;
	// Renderer data
	private static int windowWidth;
	private static int windowHeight;
	private static int vertexArrayID = 0;
	private static int positionsBufferID = 0;
	private static int normalsBufferID = 0;
	private static int vertexIndexBufferID = 0;
	private static int vertexShaderID = 0;
	private static int fragementShaderID = 0;
	private static int programID = 0;
	// Model data
	private static float meshResolution = 0.5f;
	private static NoiseSource noiseSource;
	private static Polygonizer polygonizer = new MarchingCubesPolygonizer();
	private static final TFloatList positions = new TFloatArrayList();
	private static final TFloatList normals = new TFloatArrayList();
	private static final TIntList indices = new TIntArrayList();
	private static int index = 0;
	private static int renderingIndicesCount;
	// Shader data
	private static int modelToCameraMatrixLocation;
	private static int normalModelToCameraMatrixLocation;
	private static int cameraToClipMatrixLocation;
	private static int diffuseColorLocation;
	private static int lightIntensityLocation;
	private static int ambientIntensityLocation;
	private static int modelSpaceLightPositionLocation;
	private static int lightAttenuationLocation;
	private static Vector3f modelPosition = new Vector3f(0, 0, 0);
	private static Quaternion modelRotation = new Quaternion();
	private static final Matrix4f modelRotationMatrix = new Matrix4f();
	private static final Matrix4f modelPositionMatrix = new Matrix4f();
	private static final Matrix4f modelToCameraMatrix = new Matrix4f();
	private static final Matrix4f cameraToClipMatrix = new Matrix4f();
	private static Vector3f lightPosition = new Vector3f(0, 0, 0);
	private static Color lightIntensity = new Color(0.9f, 0.9f, 0.9f, 1);
	private static Color ambientIntensity = new Color(0.1f, 0.1f, 0.1f, 1);
	private static Color diffuseColor = new Color(1, 0.1f, 0.1f, 1);
	private static Color backgroundColor = new Color(0.2f, 0.2f, 0.2f, 0);
	private static float lightAttenuation = 0.12f;

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
		createDisplay(title, windowWidth, windowHeight);
		createProjection(fieldOfView);
		createShaders();
	}

	/**
	 * Destroys the render window and its resources.
	 */
	public static void destroy() {
		destroyModel();
		destroyShaders();
		destroyDisplay();
	}

	private static void createDisplay(String title, int width, int height) throws LWJGLException {
		if (isDisplayCreated) {
			throw new IllegalStateException("Display has already been created.");
		}
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
		isDisplayCreated = true;
		checkForOpenGLError("createDisplay");
	}

	private static void destroyDisplay() {
		if (!isDisplayCreated) {
			throw new IllegalStateException("Display has not been created yet.");
		}
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_CULL_FACE);
		checkForOpenGLError("destroyDisplay");
		Display.destroy();
		isDisplayCreated = false;
	}

	private static void createProjection(float fieldOfView) {
		final float aspectRatio = windowWidth / windowHeight;
		final float near_plane = 0;
		final float far_plane = 100;
		final float y_scale = (float) (1 / Math.tan(Math.toRadians(fieldOfView / 2)));
		final float x_scale = y_scale / aspectRatio;
		final float frustum_length = far_plane - near_plane;
		cameraToClipMatrix.m00 = x_scale;
		cameraToClipMatrix.m11 = y_scale;
		cameraToClipMatrix.m22 = -((far_plane + near_plane) / frustum_length);
		cameraToClipMatrix.m23 = -1;
		cameraToClipMatrix.m32 = -((2 * near_plane * far_plane) / frustum_length);
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
		modelToCameraMatrixLocation = GL20.glGetUniformLocation(programID, "modelToCameraMatrix");
		normalModelToCameraMatrixLocation = GL20.glGetUniformLocation(programID, "normalModelToCameraMatrix");
		cameraToClipMatrixLocation = GL20.glGetUniformLocation(programID, "cameraToClipMatrix");
		diffuseColorLocation = GL20.glGetUniformLocation(programID, "diffuseColor");
		lightIntensityLocation = GL20.glGetUniformLocation(programID, "lightIntensity");
		ambientIntensityLocation = GL20.glGetUniformLocation(programID, "ambientIntensity");
		modelSpaceLightPositionLocation = GL20.glGetUniformLocation(programID, "modelSpaceLightPosition");
		lightAttenuationLocation = GL20.glGetUniformLocation(programID, "lightAttenuation");
		GL20.glValidateProgram(programID);
		checkForOpenGLError("createShaders");
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

	/**
	 * Generates the model's mesh from the noise source using the provided polygonizer. Default
	 * noise source is a {@link SimplePerlinNoiseSource} with a frequeny of 0.05. Default
	 * polygonizer is the {@link MarchingCubesPolygonizer}.
	 *
	 * @param x The x coordinate of the position of the model.
	 * @param y The y coordinate of the position of the model.
	 * @param z The z coordinate of the position of the model.
	 * @param sizeX The size on the x axis of the model.
	 * @param sizeY The size on the y axis of the model.
	 * @param sizeZ The size on the z axis of the model.
	 */
	public static void generateModelMesh(float x, float y, float z, float sizeX, float sizeY, float sizeZ) {
		if (noiseSource == null) {
			throw new IllegalStateException("Noise source must be defined first.");
		}
		final GridCell cell = new GridCell();
		for (float xx = x; xx < x + sizeX; xx += meshResolution) {
			for (float yy = y; yy < y + sizeY; yy += meshResolution) {
				for (float zz = z; zz < z + sizeZ; zz += meshResolution) {
					cell.p0.set(xx, yy, zz);
					cell.p1.set(xx + meshResolution, yy, zz);
					cell.p2.set(xx + meshResolution, yy, zz + meshResolution);
					cell.p3.set(xx, yy, zz + meshResolution);
					cell.p4.set(xx, yy + meshResolution, zz);
					cell.p5.set(xx + meshResolution, yy + meshResolution, zz);
					cell.p6.set(xx + meshResolution, yy + meshResolution, zz + meshResolution);
					cell.p7.set(xx, yy + meshResolution, zz + meshResolution);
					cell.v0 = noiseSource.noise(xx, yy, zz);
					cell.v1 = noiseSource.noise(xx + meshResolution, yy, zz);
					cell.v2 = noiseSource.noise(xx + meshResolution, yy, zz + meshResolution);
					cell.v3 = noiseSource.noise(xx, yy, zz + meshResolution);
					cell.v4 = noiseSource.noise(xx, yy + meshResolution, zz);
					cell.v5 = noiseSource.noise(xx + meshResolution, yy + meshResolution, zz);
					cell.v6 = noiseSource.noise(xx + meshResolution, yy + meshResolution, zz + meshResolution);
					cell.v7 = noiseSource.noise(xx, yy + meshResolution, zz + meshResolution);
					index = polygonizer.polygonize(cell, positions, normals, indices, index);
				}
			}
		}
	}

	/**
	 * Delete all the model mesh generated so far.
	 */
	public static void deleteModelMesh() {
		positions.clear();
		normals.clear();
		indices.clear();
		index = 0;
	}

	/**
	 * Creates the model from it's mesh. Must be called after mesh is generated using {@link #generateModelMesh}.
	 *
	 * @throws IllegalStateException If the display wasn't created first. If the model has already
	 * been created.
	 */
	public static void createModel() {
		if (!isDisplayCreated) {
			throw new IllegalStateException("Display needs to be created first.");
		}
		if (isModelCreated) {
			throw new IllegalStateException("Model has already been created.");
		}
		vertexIndexBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vertexIndexBufferID);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		renderingIndicesCount = indices.size();
		positionsBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsBufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positionsBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		normalsBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsBufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalsBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		vertexArrayID = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vertexArrayID);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsBufferID);
		GL20.glVertexAttribPointer(0, POSITION_COMPONENT_COUNT, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsBufferID);
		GL20.glVertexAttribPointer(1, NORMAL_COMPONENT_COUNT, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		isModelCreated = true;
		checkForOpenGLError("createModel");
	}

	/**
	 * Updates the model. Deletes old model resources and creates new ones from the mesh. Does not
	 * delete the model mesh.
	 *
	 * @throws IllegalStateException If the display wasn't created first.
	 */
	public static void updateModel() {
		if (!isModelCreated) {
			throw new IllegalStateException("Model needs to be created first.");
		}
		GL30.glBindVertexArray(vertexArrayID);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(positionsBufferID);
		GL15.glDeleteBuffers(normalsBufferID);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vertexIndexBufferID);
		vertexIndexBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vertexIndexBufferID);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		renderingIndicesCount = indices.size();
		positionsBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsBufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positionsBuffer(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(0, POSITION_COMPONENT_COUNT, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		normalsBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsBufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalsBuffer(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(1, NORMAL_COMPONENT_COUNT, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		checkForOpenGLError("updateModel");
	}

	private static void destroyModel() {
		if (!isModelCreated) {
			return;
		}
		GL30.glBindVertexArray(vertexArrayID);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(positionsBufferID);
		GL15.glDeleteBuffers(normalsBufferID);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vertexIndexBufferID);
		GL30.glBindVertexArray(0);
		GL30.glDeleteVertexArrays(vertexArrayID);
		renderingIndicesCount = 0;
		isModelCreated = false;
		checkForOpenGLError("destroyModel");
	}

	/**
	 * The doLogic part of the rendering cycle. Generates the matrices for model to camera
	 * transformation.
	 *
	 * @throws IllegalStateException If the display wasn't created first.
	 */
	public static void doLogic() {
		if (!isDisplayCreated) {
			throw new IllegalStateException("Display needs to be created first.");
		}
		modelRotationMatrix.setIdentity();
		modelRotationMatrix.m00 = 1 - 2 * modelRotation.y * modelRotation.y - 2 * modelRotation.z * modelRotation.z;
		modelRotationMatrix.m01 = 2 * modelRotation.x * modelRotation.y - 2 * modelRotation.w * modelRotation.z;
		modelRotationMatrix.m02 = 2 * modelRotation.x * modelRotation.z + 2 * modelRotation.w * modelRotation.y;
		modelRotationMatrix.m03 = 0;
		modelRotationMatrix.m10 = 2 * modelRotation.x * modelRotation.y + 2 * modelRotation.w * modelRotation.z;
		modelRotationMatrix.m11 = 1 - 2 * modelRotation.x * modelRotation.x - 2 * modelRotation.z * modelRotation.z;
		modelRotationMatrix.m12 = 2 * modelRotation.y * modelRotation.z - 2 * modelRotation.w * modelRotation.x;
		modelRotationMatrix.m13 = 0;
		modelRotationMatrix.m20 = 2 * modelRotation.x * modelRotation.z - 2 * modelRotation.w * modelRotation.y;
		modelRotationMatrix.m21 = 2 * modelRotation.y * modelRotation.z + 2.f * modelRotation.x * modelRotation.w;
		modelRotationMatrix.m22 = 1 - 2 * modelRotation.x * modelRotation.x - 2 * modelRotation.y * modelRotation.y;
		modelRotationMatrix.m23 = 0;
		modelPositionMatrix.setIdentity();
		Matrix4f.translate(modelPosition, modelPositionMatrix, modelPositionMatrix);
		Matrix4f.mul(modelRotationMatrix, modelPositionMatrix, modelToCameraMatrix);
		logicRanOnce = true;
	}

	private static void preRender() {
		final FloatBuffer matrix44Buffer = BufferUtils.createFloatBuffer(16);
		modelToCameraMatrix.store(matrix44Buffer);
		matrix44Buffer.flip();
		GL20.glUniformMatrix4(modelToCameraMatrixLocation, false, matrix44Buffer);
		matrix44Buffer.clear();
		cameraToClipMatrix.store(matrix44Buffer);
		matrix44Buffer.flip();
		GL20.glUniformMatrix4(cameraToClipMatrixLocation, false, matrix44Buffer);
		final FloatBuffer matrix33Buffer = BufferUtils.createFloatBuffer(9);
		modelToCameraMatrix.store3f(matrix33Buffer);
		matrix33Buffer.flip();
		GL20.glUniformMatrix3(normalModelToCameraMatrixLocation, false, matrix33Buffer);
		GL20.glUniform4f(diffuseColorLocation, modelColor().getRed() / 255f, modelColor().getGreen() / 255f,
				modelColor().getBlue() / 255f, modelColor().getAlpha() / 255f);
		GL20.glUniform4f(lightIntensityLocation, lightIntensity.getRed() / 255f, lightIntensity.getGreen() / 255f,
				lightIntensity.getBlue() / 255f, lightIntensity.getAlpha() / 255f);
		GL20.glUniform4f(ambientIntensityLocation, ambientIntensity.getRed() / 255f, ambientIntensity.getGreen() / 255f,
				ambientIntensity.getBlue() / 255f, ambientIntensity.getAlpha() / 255f);
		GL20.glUniform3f(modelSpaceLightPositionLocation, lightPosition.x, lightPosition.y,
				lightPosition.z);
		GL20.glUniform1f(lightAttenuationLocation, lightAttenuation);
		checkForOpenGLError("preRender");
	}

	/**
	 * Displays the current model with the proper rotation and position to the render window.
	 *
	 * @throws IllegalStateException If the display wasn't created first. If the model wasn't
	 * created first. If the doLogic wasn't created ran at least once before.
	 */
	public static void render() {
		if (!isDisplayCreated) {
			throw new IllegalStateException("Display needs to be created first.");
		}
		if (!isModelCreated) {
			throw new IllegalStateException("Model needs to be created first.");
		}
		if (!logicRanOnce) {
			throw new IllegalStateException("Logic needs to be run first at least once.");
		}
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL20.glUseProgram(programID);
		preRender();
		GL30.glBindVertexArray(vertexArrayID);
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vertexIndexBufferID);
		GL11.glDrawElements(GL11.GL_TRIANGLES, renderingIndicesCount, GL11.GL_UNSIGNED_INT, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);
		GL20.glUseProgram(0);
		Display.sync(60);
		Display.update();
		checkForOpenGLError("render");
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
	 * Gets the model color
	 *
	 * @return The model color.
	 */
	public static Color modelColor() {
		return diffuseColor;
	}

	/**
	 * Sets the model color.
	 *
	 * @param color The model color.
	 */
	public static void modelColor(Color color) {
		diffuseColor = color;
	}

	/**
	 * Gets the model position.
	 *
	 * @return The model position.
	 */
	public static Vector3f modelPosition() {
		return modelPosition;
	}

	/**
	 * Sets the model position.
	 *
	 * @param position The model position.
	 */
	public static void modelPosition(Vector3f position) {
		modelPosition = position;
	}

	/**
	 * Gets the model rotation.
	 *
	 * @return The model rotation.
	 */
	public static Quaternion modelRotation() {
		return modelRotation;
	}

	/**
	 * Sets the model rotation.
	 *
	 * @param rotation The model rotation.
	 */
	public static void modelRotation(Quaternion rotation) {
		modelRotation = rotation;
	}

	/**
	 * Gets the vector representing the right direction for the camera
	 *
	 * @return The camera's right direction vector.
	 */
	public static Vector3f cameraRight() {
		return toCamera(new Vector3f(1, 0, 0));
	}

	/**
	 * Gets the vector representing the up direction for the camera
	 *
	 * @return The camera's up direction vector.
	 */
	public static Vector3f cameraUp() {
		return toCamera(new Vector3f(0, 1, 0));
	}

	/**
	 * Gets the vector representing the forward direction for the camera
	 *
	 * @return The camera's forward direction vector.
	 */
	public static Vector3f cameraForward() {
		return toCamera(new Vector3f(0, 0, 1));
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
	 * Sets the light color. This also represents its intensity.
	 *
	 * @param color The light color and intensity.
	 */
	public static void lightColor(Color color) {
		lightIntensity = color;
	}

	/**
	 * Gets the light color. This also represents its intensity.
	 *
	 * @return The light color and intensity.
	 */
	public static Color lightColor() {
		return lightIntensity;
	}

	/**
	 * Sets the ambient light color. This also represents its intensity.
	 *
	 * @param color The ambient light color and intensity.
	 */
	public static void ambientLightColor(Color color) {
		ambientIntensity = color;
	}

	/**
	 * Gets the ambient light color. This also represents its intensity.
	 *
	 * @return The ambient light color and intensity.
	 */
	public static Color ambientLightColor() {
		return ambientIntensity;
	}

	/**
	 * Gets the light distance attenuation factor. In other terms, how much distance affects light
	 * intensity. Larger values affect it more. 0.12 is the default value.
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
	 * Gets the mesh resolution. Used by {@link #generateModelMesh}. Smaller values mean higher
	 * resolution meshes. 0.5 is the default value.
	 *
	 * @return The mesh resolution.
	 */
	public static float meshResolution() {
		return meshResolution;
	}

	/**
	 * Sets the mesh resolution. Used by {@link #generateModelMesh}. Smaller values mean higher
	 * resolution meshes. 0.5 is the default value.
	 *
	 * @param resolution The mesh resolution.
	 */
	public static void meshResolution(float resolution) {
		meshResolution = resolution;
	}

	/**
	 * Gets the noise source. Used by {@link #generateModelMesh}.
	 *
	 * @return The noise source.
	 * @see NoiseSource
	 */
	public static NoiseSource noiseSource() {
		return noiseSource;
	}

	/**
	 * Sets the noise source. Used by {@link #generateModelMesh}.
	 *
	 * @param source The noise source.
	 * @see NoiseSource
	 */
	public static void noiseSource(NoiseSource source) {
		noiseSource = source;
	}

	/**
	 * Gets the polygonizer. Used by {@link #generateModelMesh}.
	 *
	 * @return The polygonizer.
	 * @see Polygonizer
	 */
	public static Polygonizer polygonizer() {
		return polygonizer;
	}

	/**
	 * Sets the polygonizer. Used by {@link #generateModelMesh}.
	 *
	 * @param polygonizer The polygonizer.
	 * @see Polygonizer
	 */
	public static void polygonizer(Polygonizer polygonizer) {
		Doxel.polygonizer = polygonizer;
	}

	private static void checkForOpenGLError(String step) {
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

	private static FloatBuffer positionsBuffer() {
		final FloatBuffer positionsBuffer = BufferUtils.createFloatBuffer(positions.size());
		positionsBuffer.put(positions.toArray());
		positionsBuffer.flip();
		return positionsBuffer;
	}

	private static FloatBuffer normalsBuffer() {
		final FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(normals.size());
		verticesBuffer.put(normals.toArray());
		verticesBuffer.flip();
		return verticesBuffer;
	}

	private static IntBuffer indicesBuffer() {
		final IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.size());
		indicesBuffer.put(indices.toArray());
		indicesBuffer.flip();
		return indicesBuffer;
	}

	private static Vector3f toCamera(Vector3f v) {
		return transform(Matrix4f.invert(modelRotationMatrix, null), v);
	}

	private static Vector3f transform(Matrix4f m, Vector3f v) {
		final Vector4f v4 = new Vector4f(v.x, v.y, v.z, 1);
		Matrix4f.transform(m, v4, v4);
		v.set(v4.x, v4.y, v4.z);
		return v;
	}
}
