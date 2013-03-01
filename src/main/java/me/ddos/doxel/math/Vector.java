package me.ddos.doxel.math;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author DDoS
 */
public class Vector implements Comparable<Vector>, Serializable, Cloneable {
	private static final long serialVersionUID = 1;
	private final float[] vec;

	public Vector(int size) {
		if (size < 2) {
			throw new IllegalArgumentException("Minimum vector size is 2");
		}
		vec = new float[size];
	}

	public Vector(Vector2 v) {
		this(v.getX(), v.getY());
	}

	public Vector(Vector3 v) {
		this(v.getX(), v.getY(), v.getZ());
	}

	public Vector(Vector4 v) {
		this(v.getX(), v.getY(), v.getZ(), v.getW());
	}

	public Vector(Vector v) {
		this(v.vec);
	}

	public Vector(float... v) {
		vec = v.clone();
	}

	public int size() {
		return vec.length;
	}

	public float get(int comp) {
		return vec[comp];
	}

	public int getFloored(int comp) {
		return MathHelper.floor(get(comp));
	}

	public void set(int comp, float val) {
		vec[comp] = val;
	}

	public void setZero() {
		Arrays.fill(vec, 0);
	}

	public Vector resize(int size) {
		final Vector d = new Vector(size);
		System.arraycopy(vec, 0, d.vec, 0, Math.min(size, size()));
		return d;
	}

	public Vector add(Vector v) {
		return add(v.vec);
	}

	public Vector add(float... v) {
		final int size = Math.min(size(), v.length);
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = vec[comp] + v[comp];
		}
		return d;
	}

	public Vector sub(Vector v) {
		return sub(v.vec);
	}

	public Vector sub(float... v) {
		final int size = Math.min(size(), v.length);
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = vec[comp] - v[comp];
		}
		return d;
	}

	public Vector mul(Vector v) {
		return mul(v.vec);
	}

	public Vector mul(float... v) {
		final int size = Math.min(size(), v.length);
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = vec[comp] * v[comp];
		}
		return d;
	}

	public Vector div(Vector v) {
		return div(v.vec);
	}

	public Vector div(float... v) {
		final int size = Math.min(size(), v.length);
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = vec[comp] / v[comp];
		}
		return d;
	}

	public float dot(Vector v) {
		return dot(v.vec);
	}

	public float dot(float... v) {
		final int size = Math.min(size(), v.length);
		float d = 0;
		for (int comp = 0; comp < size; comp++) {
			d += vec[comp] * v[comp];
		}
		return d;
	}

	public Vector scale(double scale) {
		return scale((float) scale);
	}

	public Vector scale(float scale) {
		final int size = size();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = vec[comp] * scale;
		}
		return d;
	}

	public Vector pow(double power) {
		return scale((float) power);
	}

	public Vector pow(float power) {
		final int size = size();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = (float) Math.pow(vec[comp], power);
		}
		return d;
	}

	public Vector ceil() {
		final int size = size();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = (float) Math.ceil(vec[comp]);
		}
		return d;
	}

	public Vector floor() {
		final int size = size();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = MathHelper.floor(vec[comp]);
		}
		return d;
	}

	public Vector round() {
		final int size = size();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = Math.round(vec[comp]);
		}
		return d;
	}

	public Vector abs() {
		final int size = size();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = Math.abs(vec[comp]);
		}
		return d;
	}

	public Vector min(Vector v) {
		return min(v.vec);
	}

	public Vector min(float... v) {
		final int size = Math.min(size(), v.length);
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = Math.min(vec[comp], v[comp]);
		}
		return d;
	}

	public Vector max(Vector v) {
		return max(v.vec);
	}

	public Vector max(float... v) {
		final int size = Math.min(size(), v.length);
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = Math.max(vec[comp], v[comp]);
		}
		return d;
	}

	public float distanceSquared(Vector v) {
		return distanceSquared(v.vec);
	}

	public float distanceSquared(float... v) {
		final int size = Math.min(size(), v.length);
		final float[] d = new float[size];
		for (int comp = 0; comp < size; comp++) {
			d[comp] = vec[comp] - v[comp];
		}
		return MathHelper.lengthSquared(d);
	}

	public float distance(Vector v) {
		return distanceSquared(v.vec);
	}

	public float distance(float... v) {
		final int size = Math.min(size(), v.length);
		final float[] d = new float[size];
		for (int comp = 0; comp < size; comp++) {
			d[comp] = vec[comp] - v[comp];
		}
		return MathHelper.length(d);
	}

	public float lengthSquared() {
		return MathHelper.lengthSquared(vec);
	}

	public float length() {
		return MathHelper.length(vec);
	}

	public Vector normalize() {
		final int size = size();
		final float length = length();
		final Vector d = new Vector(size);
		for (int comp = 0; comp < size; comp++) {
			d.vec[comp] = vec[comp] / length;
		}
		return d;
	}

	public Vector2 toVector2() {
		return new Vector2(vec[0], vec[1]);
	}

	public Vector3 toVector3() {
		return new Vector3(vec[0], vec[1], size() > 2 ? vec[2] : 0);
	}

	public Vector4 toVector4() {
		final int size = size();
		return new Vector4(vec[0], vec[1], size > 2 ? vec[2] : 0, size > 3 ? vec[3] : 0);
	}

	public Matrix toScalingMatrix(int size) {
		return new Matrix(size, false, this);
	}

	public Matrix toTranslationMatrix(int size) {
		return new Matrix(size, true, this);
	}

	@Override
	public int compareTo(Vector v) {
		return (int) (lengthSquared() - v.lengthSquared());
	}

	@Override
	public Vector clone() {
		return new Vector(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector)) {
			return false;
		}
		return Arrays.equals(vec, ((Vector) obj).vec);
	}

	@Override
	public int hashCode() {
		return 67 * 5 + Arrays.hashCode(vec);
	}

	public float[] toArray() {
		return vec.clone();
	}

	@Override
	public String toString() {
		return Arrays.toString(vec).replace('[', '(').replace(']', ')');
	}
}
