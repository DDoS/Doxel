package me.ddos.doxel;

import java.util.Random;
import net.royawesome.jlibnoise.module.source.Perlin;

/**
 * A simple perlin noise source.
 *
 * @author DDoS
 */
public class SimplePerlinNoiseSource implements NoiseSource {
	private final Perlin perlin = new Perlin();

	/**
	 * Constructs a new simple perlin noise source. The perlin settings are the defaults of the {@link Perlin}
	 * class for the exception of the frequency.
	 *
	 * @param frequency The frequency of the perlin.
	 */
	public SimplePerlinNoiseSource(double frequency) {
		perlin.setFrequency(frequency);
		perlin.setSeed(new Random().nextInt());
	}

	@Override
	public double noise(float x, float y, float z) {
		return perlin.GetValue(x, y, z);
	}
}
