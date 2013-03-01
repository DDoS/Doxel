package me.ddos.doxel.polygonizer;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import me.ddos.doxel.Model;
import me.ddos.doxel.NoiseSource;
import org.lwjgl.util.vector.Vector3f;

/**
 * Generates a triangle mesh from grid cells.
 *
 * @author DDoS
 * @see GridCell
 */
public abstract class Polygonizer {
	/**
	 * Generates the model's mesh from the noise source using the provided polygonizer.
	 *
	 * @param noise The noise source to polygonize.
	 * @param meshResolution The resolution to mesh at.
	 * @param position The starting point in the noise.
	 * @param size The size of the sample to polygonize.
	 * @return The meshed but uncreated model.
	 */
	public Model createModel(NoiseSource noise, float meshResolution,
			Vector3f position, Vector3f size) {
		if (noise == null) {
			throw new IllegalArgumentException("Noise source must be defined first.");
		}
		final Model model = new Model();
		int index = 0;
		final GridCell cell = new GridCell();
		final float x = position.x;
		final float y = position.y;
		final float z = position.z;
		final float sizeX = size.x;
		final float sizeY = size.y;
		final float sizeZ = size.z;
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
					cell.v0 = noise.noise(xx, yy, zz);
					cell.v1 = noise.noise(xx + meshResolution, yy, zz);
					cell.v2 = noise.noise(xx + meshResolution, yy, zz + meshResolution);
					cell.v3 = noise.noise(xx, yy, zz + meshResolution);
					cell.v4 = noise.noise(xx, yy + meshResolution, zz);
					cell.v5 = noise.noise(xx + meshResolution, yy + meshResolution, zz);
					cell.v6 = noise.noise(xx + meshResolution, yy + meshResolution, zz + meshResolution);
					cell.v7 = noise.noise(xx, yy + meshResolution, zz + meshResolution);
					index = polygonize(cell, model.positions(), model.normals(), model.indices(), index);
				}
			}
		}
		return model;
	}

	/**
	 * Polygonizes the grid cell. Stores the position normal data and of the triangle's vertices in
	 * the two appropriate lists. Both data are three component vectors (x, y, z). Indices for the
	 * winding order of the vertices are stored in the indices list. The index passed to the method
	 * is the current available index. This methods returns the new current available index which
	 * will be passed in future calls. <p> Position and normal data needs to be ordered by vector
	 * component order (x, y, z) in the lists, but actual vector order does not matter as long as
	 * both list have the same order and correct order is properly defined in the indices list. <p>
	 * Vertices should be in the clock-wise winding order.
	 *
	 * @param cell The grid cell to polygonize.
	 * @param positions The list of position components.
	 * @param normals The list or normal components.
	 * @param indices The list of indices for the order of the vertices in the position and normal
	 * list.
	 * @param index The current available index.
	 * @return The next current available index.
	 */
	protected abstract int polygonize(GridCell cell, TFloatList positions, TFloatList normals,
			TIntList indices, int index);
}
