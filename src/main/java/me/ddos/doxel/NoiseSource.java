package me.ddos.doxel;

/**
 * A source of real scalar noise values located in 3D space.
 *
 * @author DDoS
 */
public interface NoiseSource {
	/**
	 * Gets the noise value at the particular position in 3D space.
	 *
	 * @param x The x coordinate of the position.
	 * @param y The y coordinate of the position.
	 * @param z The z coordinate of the position.
	 * @return The noise value.
	 */
	public double noise(float x, float y, float z);
}
