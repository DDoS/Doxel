package ca.sapon.doxel.polygonizer;

import com.flowpowered.caustic.api.data.VertexData;
import com.flowpowered.math.vector.Vector3f;

import ca.sapon.doxel.NoiseSource;

/**
 * Generates a triangle mesh from grid cells.
 *
 * @author DDoS
 * @see GridCell
 */
public abstract class Polygonizer {
    protected double threshold = 0.5;

    /**
     * Generates the model's mesh from the noise source using the provided polygonizer.
     *
     * @param noise The noise source to polygonize.
     * @param meshResolution The resolution to mesh at.
     * @param position The starting point in the noise.
     * @param size The size of the sample to polygonize.
     * @return The meshed but uncreated model.
     */
    public VertexData createMesh(NoiseSource noise, float meshResolution, Vector3f position, Vector3f size) {
        if (noise == null) {
            throw new IllegalArgumentException("Noise source must be defined first.");
        }
        final float x = position.getX();
        final float y = position.getY();
        final float z = position.getZ();
        final float sizeX = size.getX();
        final float sizeY = size.getY();
        final float sizeZ = size.getZ();
        final int densitiesSizeX = (int) Math.ceil(sizeX / meshResolution);
        final int densitiesSizeY = (int) Math.ceil(sizeY / meshResolution);
        final int densitiesSizeZ = (int) Math.ceil(sizeZ / meshResolution);
        final double[] densities = new double[densitiesSizeX * densitiesSizeY * densitiesSizeZ];
        for (float zz = 0; zz < sizeZ; zz += meshResolution) {
            for (float yy = 0; yy < sizeY; yy += meshResolution) {
                for (float xx = 0; xx < sizeX; xx += meshResolution) {
                    densities[(int) ((zz * densitiesSizeX * densitiesSizeY + yy * densitiesSizeX + xx) / meshResolution)] = noise.noise(x + xx, y + yy, z + zz);
                }
            }
        }
        return generateMesh(densities, densitiesSizeX, densitiesSizeY, densitiesSizeZ);
    }

    protected abstract VertexData generateMesh(double[] densities, int xSize, int ySize, int zSize);

    /**
     * Gets the threshold which is the minimum value before a value is considered as being outside the mesh.
     *
     * @return The threshold value.
     */
    public double threshold() {
        return threshold;
    }

    /**
     * Sets the threshold which is the minimum value before a value is considered as being outside the mesh.
     *
     * @param threshold The threshold value.
     */
    public void threshold(double threshold) {
        this.threshold = threshold;
    }
}
