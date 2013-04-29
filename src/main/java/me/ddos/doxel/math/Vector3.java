package me.ddos.doxel.math;

import java.io.Serializable;

/**
 *
 * @author DDoS
 */
public class Vector3 implements Comparable<Vector3>, Serializable, Cloneable {
	private static final long serialVersionUID = 1;
	public static final Vector3 UNIT_X = new Vector3(1, 0, 0);
	public static final Vector3 UNIT_Y = new Vector3(0, 1, 0);
	public static final Vector3 UNIT_Z = new Vector3(0, 0, 1);
	private final float x;
	private final float y;
	private final float z;

	public Vector3() {
		this(0, 0, 0);
	}

	public Vector3(Vector2 v) {
		this(v.getX(), v.getY(), 0);
	}

	public Vector3(Vector3 v) {
		this(v.x, v.y, v.z);
	}

	public Vector3(Vector4 v) {
		this(v.getX(), v.getY(), v.getZ());
	}

	public Vector3(double x, double y, double z) {
		this((float) x, (float) y, (float) z);
	}

	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
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

	public int getFloorX() {
		return MathHelper.floor(x);
	}

	public int getFloorY() {
		return MathHelper.floor(y);
	}

	public int getFloorZ() {
		return MathHelper.floor(z);
	}

	public Vector3 add(Vector3 v) {
		return add(v.x, v.y, v.z);
	}

	public Vector3 add(double x, double y, double z) {
		return add((float) x, (float) y, (float) z);
	}

	public Vector3 add(float x, float y, float z) {
		return new Vector3(this.x + x, this.y + y, this.z + z);
	}

	public Vector3 sub(Vector3 v) {
		return sub(v.x, v.y, v.z);
	}

	public Vector3 sub(double x, double y, double z) {
		return sub((float) x, (float) y, (float) z);
	}

	public Vector3 sub(float x, float y, float z) {
		return new Vector3(this.x - x, this.y - y, this.z - z);
	}

	public Vector3 mul(Vector3 v) {
		return mul(v.x, v.y, v.z);
	}

	public Vector3 mul(double x, double y, double z) {
		return mul((float) x, (float) y, (float) z);
	}

	public Vector3 mul(float x, float y, float z) {
		return new Vector3(this.x * x, this.y * y, this.z * z);
	}

	public Vector3 div(Vector3 v) {
		return div(v.x, v.y, v.z);
	}

	public Vector3 div(double x, double y, double z) {
		return div((float) x, (float) y, (float) z);
	}

	public Vector3 div(float x, float y, float z) {
		return new Vector3(this.x / x, this.y / y, this.z / z);
	}

	public float dot(Vector3 v) {
		return dot(v.x, v.y, v.z);
	}

	public float dot(double x, double y, double z) {
		return dot((float) x, (float) y, (float) z);
	}

	public float dot(float x, float y, float z) {
		return this.x * x + this.y * y + this.z * z;
	}

	public Vector3 cross(Vector3 v) {
		return cross(v.x, v.y, v.z);
	}

	public Vector3 cross(double x, double y, double z) {
		return cross((float) x, (float) y, (float) z);
	}

	public Vector3 cross(float x, float y, float z) {
		return new Vector3(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	public Vector3 scale(double scale) {
		return scale((float) scale);
	}

	public Vector3 scale(float scale) {
		return mul(scale, scale, scale);
	}

	public Vector3 pow(double power) {
		return pow((float) power);
	}

	public Vector3 pow(float power) {
		return new Vector3(Math.pow(x, power), Math.pow(y, power), Math.pow(z, power));
	}

	public Vector3 ceil() {
		return new Vector3(Math.ceil(x), Math.ceil(y), Math.ceil(z));
	}

	public Vector3 floor() {
		return new Vector3(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
	}

	public Vector3 round() {
		return new Vector3(Math.round(x), Math.round(y), Math.round(z));
	}

	public Vector3 abs() {
		return new Vector3(Math.abs(x), Math.abs(y), Math.abs(z));
	}

	public Vector3 negate() {
		return new Vector3(-x, -y, -z);
	}

	public Vector3 min(Vector3 v) {
		return min(v.x, v.y, v.z);
	}

	public Vector3 min(double x, double y, double z) {
		return min((float) x, (float) y, (float) z);
	}

	public Vector3 min(float x, float y, float z) {
		return new Vector3(Math.min(this.x, x), Math.min(this.y, y), Math.min(this.z, z));
	}

	public Vector3 max(Vector3 v) {
		return max(v.x, v.y, v.z);
	}

	public Vector3 max(double x, double y, double z) {
		return max((float) x, (float) y, (float) z);
	}

	public Vector3 max(float x, float y, float z) {
		return new Vector3(Math.max(this.x, x), Math.max(this.y, y), Math.max(this.z, z));
	}

	public float distanceSquared(Vector3 v) {
		return distanceSquared(v.x, v.y, v.z);
	}

	public float distanceSquared(double x, double y, double z) {
		return distanceSquared((float) x, (float) y, (float) z);
	}

	public float distanceSquared(float x, float y, float z) {
		return MathHelper.lengthSquared(this.x - x, this.y - y, this.z - z);
	}

	public float distance(Vector3 v) {
		return distance(v.x, v.y, v.z);
	}

	public float distance(double x, double y, double z) {
		return distance((float) x, (float) y, (float) z);
	}

	public float distance(float x, float y, float z) {
		return MathHelper.length(this.x - x, this.y - y, this.z - z);
	}

	public float lengthSquared() {
		return MathHelper.lengthSquared(x, y, z);
	}

	public float length() {
		return MathHelper.length(x, y, z);
	}

	public Vector3 normalize() {
		final float length = length();
		return new Vector3(x / length, y / length, z / length);
	}

	public Vector2 toVector2() {
		return new Vector2(x, y);
	}

	public Vector4 toVector4() {
		return new Vector4(x, y, z, 0);
	}

	public Vector toVector() {
		return new Vector(x, y, z);
	}

	public Matrix toScalingMatrix(int size) {
		return Matrix.createScaling(size, this);
	}

	public Matrix toTranslationMatrix(int size) {
		return Matrix.createTranslation(size, this);
	}

	@Override
	public int compareTo(Vector3 v) {
		return (int) (lengthSquared() - v.lengthSquared());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector3)) {
			return false;
		}
		final Vector3 other = (Vector3) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) {
			return false;
		}
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y)) {
			return false;
		}
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79 * hash + Float.floatToIntBits(x);
		hash = 79 * hash + Float.floatToIntBits(y);
		hash = 79 * hash + Float.floatToIntBits(z);
		return hash;
	}

	@Override
	public Vector3 clone() {
		return new Vector3(this);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
