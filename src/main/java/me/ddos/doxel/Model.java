package me.ddos.doxel;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import me.ddos.doxel.math.Matrix;
import me.ddos.doxel.math.Quaternion;
import me.ddos.doxel.math.Vector3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * An OpenGL rendering model. Used by {@link Doxel}. Can be translated and rotated independently.
 * The model must first be meshed by adding data to the positions, normals and indices list. It can
 * then be created using {@link #create()}. Then the model can be rendered by adding it to {@link Doxel}.
 *
 * @author DDoS
 */
public class Model {
	// Vertex info
	private static final byte POSITION_COMPONENT_COUNT = 3;
	private static final byte NORMAL_COMPONENT_COUNT = 3;
	// State
	private boolean created = false;
	// Vertex data
	private final TFloatList positions = new TFloatArrayList();
	private final TFloatList normals = new TFloatArrayList();
	private final TIntList indices = new TIntArrayList();
	private int renderingIndicesCount;
	// OpenGL pointers
	private int vertexArrayID = 0;
	private int positionsBufferID = 0;
	private int normalsBufferID = 0;
	private int vertexIndexBufferID = 0;
	// Properties
	private Vector3 position = new Vector3(0, 0, 0);
	private Quaternion rotation = new Quaternion();
	private Matrix matrix = new Matrix(4);
	private boolean updateMatrix = true;
	private Color modelColor = new Color(1, 0.1f, 0.1f, 1);

	/**
	 * Creates the model from it's mesh. It can now be rendered.
	 *
	 * @throws IllegalStateException If the display wasn't created first. If the model has already
	 * been created.
	 */
	public void create() {
		if (!Doxel.created()) {
			throw new IllegalStateException("Display needs to be created first.");
		}
		if (created) {
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
		created = true;
		Doxel.checkForOpenGLError("createModel");
	}

	/**
	 * Destroys the model's resources. It can no longer be rendered.
	 */
	public void destroy() {
		if (!created) {
			return;
		}
		deleteMesh();
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
		created = false;
		Doxel.checkForOpenGLError("destroyModel");
	}

	/**
	 * Delete all the model mesh generated so far.
	 */
	public void deleteMesh() {
		positions.clear();
		normals.clear();
		indices.clear();
	}

	/**
	 * Returns the model's matrix; updating it if necessary.
	 *
	 * @return The model matrix.
	 */
	protected Matrix matrix() {
		if (updateMatrix) {
			final Matrix rotationMatrix = Matrix.createRotation(4, rotation);
			final Matrix positionMatrix = Matrix.createTranslation(4, position);
			matrix = rotationMatrix.mul(positionMatrix);
			updateMatrix = false;
		}
		return matrix;
	}

	/**
	 * Displays the current model with the proper rotation and position to the render window.
	 */
	protected void render() {
		GL30.glBindVertexArray(vertexArrayID);
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vertexIndexBufferID);
		GL11.glDrawElements(GL11.GL_TRIANGLES, renderingIndicesCount, GL11.GL_UNSIGNED_INT, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);
		Doxel.checkForOpenGLError("renderModel");
	}

	/**
	 * Returns the list of indices used by OpenGL to draw to pick the order of vertices to draw the
	 * object. Use it to add mesh data.
	 *
	 * @return The indices list.
	 */
	public TIntList indices() {
		return indices;
	}

	/**
	 * Returns the list of three component positions (x, y, z) for rendering. Use it to add mesh
	 * data.
	 *
	 * @return The position list.
	 */
	public TFloatList positions() {
		return positions;
	}

	/**
	 * Returns the list of three component normals (x, y, z) for lighting. Use it to add mesh data.
	 *
	 * @return The normal list.
	 */
	public TFloatList normals() {
		return normals;
	}

	/**
	 * Returns true if the display was created and is ready for rendering, false if otherwise.
	 *
	 * @return True if the model can be rendered, false if not.
	 */
	public boolean created() {
		return created;
	}

	/**
	 * Gets the model color.
	 *
	 * @return The model color.
	 */
	public Color color() {
		return modelColor;
	}

	/**
	 * Sets the model color.
	 *
	 * @param color The model color.
	 */
	public void color(Color color) {
		modelColor = color;
	}

	/**
	 * Gets the model position.
	 *
	 * @return The model position.
	 */
	public Vector3 position() {
		updateMatrix = true;
		return position;
	}

	/**
	 * Sets the model position.
	 *
	 * @param position The model position.
	 */
	public void position(Vector3 position) {
		this.position = position;
		updateMatrix = true;
	}

	/**
	 * Gets the model rotation.
	 *
	 * @return The model rotation.
	 */
	public Quaternion rotation() {
		updateMatrix = true;
		return rotation;
	}

	/**
	 * Sets the model rotation.
	 *
	 * @param rotation The model rotation.
	 */
	public void rotation(Quaternion rotation) {
		this.rotation = rotation;
		updateMatrix = true;
	}

	private FloatBuffer positionsBuffer() {
		final FloatBuffer positionsBuffer = BufferUtils.createFloatBuffer(positions.size());
		positionsBuffer.put(positions.toArray());
		positionsBuffer.flip();
		return positionsBuffer;
	}

	private FloatBuffer normalsBuffer() {
		final FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(normals.size());
		verticesBuffer.put(normals.toArray());
		verticesBuffer.flip();
		return verticesBuffer;
	}

	private IntBuffer indicesBuffer() {
		final IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.size());
		indicesBuffer.put(indices.toArray());
		indicesBuffer.flip();
		return indicesBuffer;
	}
}
