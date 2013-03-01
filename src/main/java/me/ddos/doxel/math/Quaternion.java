package me.ddos.doxel.math;

import java.io.Serializable;

/**
 *
 * @author DDoS
 */
public class Quaternion implements Comparable<Quaternion>, Serializable, Cloneable {
	private static final long serialVersionUID = 1;
	public static final Quaternion IDENTITY = new Quaternion();
	private final float x;
	private final float y;
	private final float z;
	private final float w;

	public Quaternion() {
		this(0, 0, 0, 1, false);
	}

	public Quaternion(Quaternion q) {
		this(q.x, q.y, q.z, q.w, false);
	}

	public Quaternion(double x, double y, double z, double w, boolean ignored) {
		this((float) x, (float) y, (float) z, (float) w, ignored);
	}

	public Quaternion(float x, float y, float z, float w, boolean ingored) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Quaternion(double pitch, double yaw, double roll) {
		this((float) pitch, (float) yaw, (float) roll);
	}

	public Quaternion(float pitch, float yaw, float roll) {
		this(new Quaternion(yaw, Vector3.UNIT_Y).mul(new Quaternion(pitch, Vector3.UNIT_X)).
				mul(new Quaternion(roll, Vector3.UNIT_Z)));
	}

	public Quaternion(Vector3 from, Vector3 to) {
		this(Math.toDegrees(Math.acos(from.dot(to) / (from.length() * to.length()))), from.cross(to));
	}

	public Quaternion(double angle, Vector3 axis) {
		this((float) angle, axis);
	}

	public Quaternion(float angle, Vector3 axis) {
		this(angle, axis.getX(), axis.getY(), axis.getZ());
	}

	public Quaternion(double angle, double x, double y, double z) {
		this((float) angle, (float) x, (float) y, (float) z);
	}

	public Quaternion(float angle, float x, float y, float z) {
		final double halfAngle = Math.toRadians(angle) / 2;
		final double q = Math.sin(halfAngle) / Math.sqrt(x * x + y * y + z * z);
		this.x = (float) (x * q);
		this.y = (float) (y * q);
		this.z = (float) (z * q);
		this.w = (float) Math.cos(halfAngle);
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public float getW() {
		return w;
	}

	public Quaternion mul(double pitch, double yaw, double roll) {
		return mul(new Quaternion(pitch, yaw, roll));
	}

	public Quaternion mul(float pitch, float yaw, float roll) {
		return mul(new Quaternion(pitch, yaw, roll));
	}

	public Quaternion mul(Vector3 from, Vector3 to) {
		return mul(new Quaternion(from, to));
	}

	public Quaternion mul(double angle, Vector3 axis) {
		return mul(new Quaternion(angle, axis));
	}

	public Quaternion mul(float angle, Vector3 axis) {
		return mul(new Quaternion(angle, axis));
	}

	public Quaternion mul(double angle, double x, double y, double z) {
		return mul(new Quaternion(angle, x, y, z));
	}

	public Quaternion mul(float angle, float x, float y, float z) {
		return mul(new Quaternion(angle, x, y, z));
	}

	public Quaternion mul(Quaternion q) {
		return mul(q.x, q.y, q.z, q.w, false);
	}

	public Quaternion mul(double x, double y, double z, double w, boolean ignored) {
		return mul((float) x, (float) y, (float) z, (float) w, ignored);
	}

	public Quaternion mul(float x, float y, float z, float w, boolean ignored) {
		return new Quaternion(
				this.w * x + this.x * w + this.y * z - this.z * y,
				this.w * y + this.y * w + this.z * x - this.x * z,
				this.w * z + this.z * w + this.x * y - this.y * x,
				this.w * w - this.x * x - this.y * y - this.z * z,
				false);
	}

	public Vector3 getDirection() {
		return toRotationMatrix(3).transform(Vector3.UNIT_Z);
	}

	public Vector3 getAxisAngles() {
		final double r1;
		final double r2;
		final double r3;
		final double test = w * x - y * z;
		if (Math.abs(test) < 0.4999) {
			r1 = Math.atan2(2 * (w * z + x * y), 1 - 2 * (x * x + z * z));
			r2 = Math.asin(2 * test);
			r3 = Math.atan2(2 * (w * y + z * x), 1 - 2 * (x * x + y * y));
		} else {
			int sign = (test < 0) ? -1 : 1;
			r1 = 0;
			r2 = sign * Math.PI / 2;
			r3 = -sign * 2 * Math.atan2(z, w);
		}
		final double roll = Math.toDegrees(r1);
		final double pitch = Math.toDegrees(r2);
		double yaw = Math.toDegrees(r3);
		if (yaw > 180) {
			yaw -= 360;
		} else if (yaw < -180) {
			yaw += 360;
		}
		return new Vector3(pitch, yaw, roll);
	}

	public float lengthSquared() {
		return MathHelper.lengthSquared(x, y, z, w);
	}

	public float length() {
		return MathHelper.length(x, y, z, w);
	}

	public Quaternion normalize() {
		final float length = length();
		return new Quaternion(x / length, y / length, z / length, w / length, false);
	}

	public Matrix toRotationMatrix(int size) {
		return new Matrix(size, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Quaternion)) {
			return false;
		}
		final Quaternion other = (Quaternion) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) {
			return false;
		}
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y)) {
			return false;
		}
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z)) {
			return false;
		}
		if (Float.floatToIntBits(w) != Float.floatToIntBits(other.w)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + Float.floatToIntBits(x);
		hash = 59 * hash + Float.floatToIntBits(y);
		hash = 59 * hash + Float.floatToIntBits(z);
		hash = 59 * hash + Float.floatToIntBits(w);
		return hash;
	}

	@Override
	public int compareTo(Quaternion q) {
		return (int) (lengthSquared() - q.lengthSquared());
	}

	@Override
	public Quaternion clone() {
		return new Quaternion(this);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ", " + w + ")";
	}
}
