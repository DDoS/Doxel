package me.ddos.doxel.math;

import java.io.Serializable;

/**
 *
 * @author DDoS
 */
public class Complex implements Comparable<Complex>, Serializable, Cloneable {
	private static final long serialVersionUID = 1;
	public static final Complex IDENTITY = new Complex();
	private final float x;
	private final float y;

	public Complex() {
		this(1, 0);
	}

	public Complex(double x, double y) {
		this((float) x, (float) y);
	}

	public Complex(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Complex(Complex c) {
		this.x = c.x;
		this.y = c.y;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public Complex mul(Complex c) {
		return mul(c.x, c.y);
	}

	public Complex mul(double x, double y) {
		return mul((float) x, (float) y);
	}

	public Complex mul(float x, float y) {
		return new Complex(
				this.x * x - this.y * y,
				this.x * y + this.y * x);
	}

	public Vector2 getDirection() {
		return new Vector2(x, y);
	}

	public float getAngleRad() {
		return (float) Math.atan2(x, y);
	}

	public float getAngleDeg() {
		return (float) Math.toDegrees(getAngleRad());
	}

	public float lengthSquared() {
		return MathHelper.lengthSquared(x, y);
	}

	public float length() {
		return MathHelper.length(x, y);
	}

	public Complex normalize() {
		final float length = length();
		return new Complex(x / length, y / length);
	}

	public Matrix toRotationMatrix(int size) {
		return Matrix.createRotation(size, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Complex)) {
			return false;
		}
		final Complex other = (Complex) obj;
		if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
			return false;
		}
		if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 97 * hash + Float.floatToIntBits(this.x);
		hash = 97 * hash + Float.floatToIntBits(this.y);
		return hash;
	}

	@Override
	public int compareTo(Complex c) {
		return (int) (lengthSquared() - c.lengthSquared());
	}

	@Override
	public Complex clone() {
		return new Complex(this);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	public static Complex fromRotationTo(Vector2 from, Vector2 to) {
		return fromAngleRad(Math.acos(from.dot(to) / (from.length() * to.length())));
	}

	public static Complex fromAngleDeg(double angle) {
		return fromAngleRad(Math.toRadians(angle));
	}

	public static Complex fromAngleRad(double angle) {
		return fromAngleRad((float) angle);
	}

	public static Complex fromAngleDeg(float angle) {
		return fromAngleRad((float) Math.toRadians(angle));
	}

	public static Complex fromAngleRad(float angle) {
		return new Complex((float) Math.cos(angle), (float) Math.sin(angle));
	}
}
