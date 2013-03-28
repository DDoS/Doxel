package me.ddos.doxel.math;

/**
 *
 * @author DDoS
 */
public class MathHelper {
	public static int floor(float x) {
		int y = (int) x;
		if (x < y) {
			return y - 1;
		}
		return y;
	}

	public static float lengthSquared(float... vals) {
		float lengthSquared = 0;
		for (int i = 0; i < vals.length; i++) {
			lengthSquared += vals[i] * vals[i];
		}
		return lengthSquared;
	}

	public static float length(float... vals) {
		return (float) Math.sqrt(lengthSquared(vals));
	}
}
