package me.ddos.doxel;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;

/**
 * Generates a triangle mesh from grid cells.
 *
 * @author DDoS
 * @see GridCell
 */
public interface Polygonizer {
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
	public int polygonize(GridCell cell, TFloatList positions, TFloatList normals,
			TIntList indices, int index);
}
