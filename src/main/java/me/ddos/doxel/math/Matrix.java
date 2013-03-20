package me.ddos.doxel.math;

import java.io.Serializable;
import java.util.Arrays;
import org.lwjgl.util.vector.Matrix2f;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

/**
 *
 * @author DDoS
 */
public class Matrix implements Serializable, Cloneable {
	private static final long serialVersionUID = 1;
	public static final Matrix IDENTITY_2 = new ImmutableIdentityMatrix(2);
	public static final Matrix IDENTITY_3 = new ImmutableIdentityMatrix(3);
	public static final Matrix IDENTITY_4 = new ImmutableIdentityMatrix(4);
	private final float[][] mat;

	public Matrix(int size) {
		if (size < 2) {
			throw new IllegalArgumentException("Minimum matrix size is 2");
		}
		mat = new float[size][size];
		setIdentity();
	}

	public Matrix(Matrix m) {
		mat = deepClone(m.mat);
	}

	public Matrix(int size, double scale) {
		this(size, (float) scale);
	}

	public Matrix(int size, float scale) {
		this(size, false, adjustSize(new float[0], size, scale));
	}

	public Matrix(int size, boolean translate, Vector2 v) {
		this(size, translate, v.getX(), v.getY());
	}

	public Matrix(int size, boolean translate, Vector3 v) {
		this(size, translate, v.getX(), v.getY(), v.getZ());
	}

	public Matrix(int size, boolean translate, Vector4 v) {
		this(size, translate, v.getX(), v.getY(), v.getZ(), v.getW());
	}

	public Matrix(int size, boolean translate, Vector v) {
		this(size, translate, v.toArray());
	}

	public Matrix(int size, boolean translate, float... vec) {
		this(size);
		if (translate) {
			vec = adjustSize(vec, size, 0);
			vec[size - 1] = 0;
			for (int row = 0; row < size; row++) {
				float dot = 0;
				for (int col = 0; col < size; col++) {
					dot += mat[col][row] * vec[col];
				}
				mat[size - 1][row] += dot;
			}
		} else {
			vec = adjustSize(vec, size, 1);
			for (int colrow = 0; colrow < size; colrow++) {
				mat[colrow][colrow] *= vec[colrow];
			}
		}
	}

	public Matrix(int size, Complex rot) {
		this(size);
		if (size < 2) {
			throw new IllegalArgumentException("Minimum matrix size is 2");
		}
		rot = rot.normalize();
		mat[0][0] = rot.getX();
		mat[0][1] = -rot.getY();
		mat[1][0] = rot.getY();
		mat[1][1] = rot.getX();
	}

	public Matrix(int size, Quaternion rot) {
		this(size);
		if (size < 3) {
			throw new IllegalArgumentException("Minimum matrix size is 3");
		}
		rot = rot.normalize();
		mat[0][0] = 1 - 2 * rot.getY() * rot.getY() - 2 * rot.getZ() * rot.getZ();
		mat[0][1] = 2 * rot.getX() * rot.getY() - 2 * rot.getW() * rot.getZ();
		mat[0][2] = 2 * rot.getX() * rot.getZ() + 2 * rot.getW() * rot.getY();
		mat[1][0] = 2 * rot.getX() * rot.getY() + 2 * rot.getW() * rot.getZ();
		mat[1][1] = 1 - 2 * rot.getX() * rot.getX() - 2 * rot.getZ() * rot.getZ();
		mat[1][2] = 2 * rot.getY() * rot.getZ() - 2 * rot.getW() * rot.getX();
		mat[2][0] = 2 * rot.getX() * rot.getZ() - 2 * rot.getW() * rot.getY();
		mat[2][1] = 2 * rot.getY() * rot.getZ() + 2.f * rot.getX() * rot.getW();
		mat[2][2] = 1 - 2 * rot.getX() * rot.getX() - 2 * rot.getY() * rot.getY();
	}

	public int size() {
		return mat.length;
	}

	public float get(int col, int row) {
		return mat[col][row];
	}

	public void set(int col, int row, double val) {
		set(col, row, (float) val);
	}

	public void set(int col, int row, float val) {
		mat[col][row] = val;
	}

	public final void setIdentity() {
		final int size = size();
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				if (col == row) {
					mat[col][row] = 1;
				} else {
					mat[col][row] = 0;

				}
			}
		}
	}

	public Matrix resize(int size) {
		final Matrix d = new Matrix(size);
		size = Math.min(size, size());
		for (int col = 0; col < size; col++) {
			System.arraycopy(mat[col], 0, d.mat[col], 0, size);
		}
		return d;
	}

	public Matrix add(Matrix m) {
		final int size = size();
		if (size != m.size()) {
			throw new IllegalArgumentException("Matrix sizes must be the same");
		}
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = mat[col][row] + m.mat[col][row];
			}
		}
		return d;
	}

	public Matrix sub(Matrix m) {
		final int size = size();
		if (size != m.size()) {
			throw new IllegalArgumentException("Matrix sizes must be the same");
		}
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = mat[col][row] - m.mat[col][row];
			}
		}
		return d;
	}

	public Matrix mul(Matrix m) {
		final int size = size();
		if (size != m.size()) {
			throw new IllegalArgumentException("Matrix sizes must be the same");
		}
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				float dot = 0;
				for (int i = 0; i < size; i++) {
					dot += mat[i][row] * m.mat[col][i];
				}
				d.mat[col][row] = dot;
			}
		}
		return d;
	}

	public Matrix translate(Vector2 v) {
		return translate(v.getX(), v.getY());
	}

	public Matrix translate(Vector3 v) {
		return translate(v.getX(), v.getY(), v.getZ());
	}

	public Matrix translate(Vector4 v) {
		return translate(v.getX(), v.getY(), v.getZ(), v.getW());
	}

	public Matrix translate(Vector v) {
		return translate(v.toArray());
	}

	public Matrix translate(float... v) {
		return new Matrix(size(), true, v).mul(this);
	}

	public Matrix scale(Vector2 v) {
		return scale(v.getX(), v.getY());
	}

	public Matrix scale(Vector3 v) {
		return scale(v.getX(), v.getY(), v.getZ());
	}

	public Matrix scale(Vector4 v) {
		return scale(v.getX(), v.getY(), v.getZ(), v.getW());
	}

	public Matrix scale(Vector v) {
		return scale(v.toArray());
	}

	public Matrix scale(float... v) {
		return new Matrix(size(), false, v).mul(this);
	}

	public Matrix rotate(Complex rot) {
		return new Matrix(size(), rot).mul(this);
	}

	public Matrix rotate(Quaternion rot) {
		return new Matrix(size(), rot).mul(this);
	}

	public Vector2 transform(Vector2 v) {
		return transform(v.getX(), v.getY()).toVector2();
	}

	public Vector3 transform(Vector3 v) {
		return transform(v.getX(), v.getY(), v.getZ()).toVector3();
	}

	public Vector4 transform(Vector4 v) {
		return transform(v.getX(), v.getY(), v.getZ(), v.getW()).toVector4();
	}

	public Vector transform(Vector v) {
		return transform(v.toArray());
	}

	public Vector transform(float... vec) {
		final int originalSize = vec.length;
		final int size = size();
		vec = adjustSize(vec, size, 1);
		final Vector d = new Vector(size);
		for (int row = 0; row < size; row++) {
			float dot = 0;
			for (int col = 0; col < size; col++) {
				dot += mat[col][row] * vec[col];
			}
			d.set(row, dot);
		}
		return d.resize(originalSize);
	}

	public Matrix floor() {
		final int size = size();
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = (float) Math.floor(mat[col][row]);
			}
		}
		return d;
	}

	public Matrix ceil() {
		final int size = size();
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = (float) Math.ceil(mat[col][row]);
			}
		}
		return d;
	}

	public Matrix round() {
		final int size = size();
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = Math.round(mat[col][row]);
			}
		}
		return d;
	}

	public Matrix abs() {
		final int size = size();
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = Math.abs(mat[col][row]);
			}
		}
		return d;
	}

	public Matrix negate() {
		final int size = size();
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = -mat[col][row];
			}
		}
		return d;
	}

	public Matrix transpose() {
		final int size = size();
		final Matrix d = new Matrix(size);
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				d.mat[col][row] = mat[row][col];
			}
		}
		return d;
	}

	public float determinant() {
		final int size = size();
		final float[][] m = deepClone(mat);
		float det;
		for (int i = 0; i < size - 1; i++) {
			for (int col = i + 1; col < size; col++) {
				det = m[col][i] / m[i][i];
				for (int row = i; row < size; row++) {
					m[col][row] -= det * m[i][row];
				}
			}
		}
		det = 1;
		for (int i = 0; i < size; i++) {
			det *= m[i][i];
		}
		return det;
	}

	public Matrix invert() {
		if (determinant() == 0) {
			return null;
		}
		final int size = size();
		final AugmentedMatrix augMat = new AugmentedMatrix(this);
		final int augmentedSize = augMat.getAugmentedSize();
		for (int i = 0; i < size; i++) {
			for (int col = 0; col < size; col++) {
				if (i != col) {
					final float ratio = augMat.get(col, i) / augMat.get(i, i);
					for (int row = 0; row < augmentedSize; row++) {
						augMat.set(col, row, augMat.get(col, row) - ratio * augMat.get(i, row));
					}
				}
			}
		}
		for (int col = 0; col < size; col++) {
			final float div = augMat.get(col, col);
			for (int row = 0; row < augmentedSize; row++) {
				augMat.set(col, row, augMat.get(col, row) / div);
			}
		}
		return augMat.getAugmentation();
	}

	public Matrix2f toMatrix2f() {
		final Matrix2f d = new Matrix2f();
		d.m00 = mat[0][0];
		d.m01 = mat[0][1];
		d.m10 = mat[1][0];
		d.m11 = mat[1][1];
		return d;
	}

	public Matrix3f toMatrix3f() {
		final Matrix3f d = new Matrix3f();
		d.m00 = mat[0][0];
		d.m01 = mat[0][1];
		d.m10 = mat[1][0];
		d.m11 = mat[1][1];
		if (size() > 2) {
			d.m02 = mat[0][2];
			d.m12 = mat[1][2];
			d.m20 = mat[2][0];
			d.m21 = mat[2][1];
			d.m22 = mat[2][2];
		}
		return d;
	}

	public Matrix4f toMatrix4f() {
		final Matrix4f d = new Matrix4f();
		d.m00 = mat[0][0];
		d.m01 = mat[0][1];
		d.m10 = mat[1][0];
		d.m11 = mat[1][1];
		if (size() > 2) {
			d.m02 = mat[0][2];
			d.m12 = mat[1][2];
			d.m20 = mat[2][0];
			d.m21 = mat[2][1];
			d.m22 = mat[2][2];
		}
		if (size() > 3) {
			d.m03 = mat[0][3];
			d.m13 = mat[1][3];
			d.m23 = mat[2][3];
			d.m30 = mat[3][0];
			d.m31 = mat[3][1];
			d.m32 = mat[3][2];
			d.m33 = mat[3][3];
		}
		return d;
	}

	public float[] toArray() {
		final int size = size();
		final float[] array = new float[size * size];
		for (int col = 0; col < size; col++) {
			System.arraycopy(mat[col], 0, array, col * size, size);
		}
		return array;
	}

	@Override
	public String toString() {
		final int size = size();
		final StringBuilder builder = new StringBuilder();
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				builder.append(mat[col][row]);
				if (col < size - 1) {
					builder.append(' ');
				}
			}
			if (row < size - 1) {
				builder.append('\n');
			}
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Matrix)) {
			return false;
		}
		return Arrays.deepEquals(mat, ((Matrix) obj).mat);
	}

	@Override
	public int hashCode() {
		return 79 * 5 + Arrays.deepHashCode(mat);
	}

	@Override
	public Matrix clone() {
		return new Matrix(this);
	}

	private static float[] adjustSize(float[] array, int toSize, float filler) {
		if (array.length == toSize) {
			return array;
		}
		final float[] d = new float[toSize];
		final int size = Math.min(array.length, toSize);
		System.arraycopy(array, 0, d, 0, size);
		Arrays.fill(d, size, toSize, filler);
		return d;
	}

	private static float[][] deepClone(float[][] array) {
		final int size = array.length;
		float[][] clone = array.clone();
		for (int i = 0; i < size; i++) {
			clone[i] = array[i].clone();
		}
		return clone;
	}

	private static class ImmutableIdentityMatrix extends Matrix {
		public ImmutableIdentityMatrix(int size) {
			super(size);
		}

		@Override
		public void set(int row, int col, float val) {
			throw new UnsupportedOperationException("You may not alter this matrix");
		}
	}

	private static class AugmentedMatrix {
		private final Matrix mat;
		private final Matrix aug;
		private final int size;

		public AugmentedMatrix(Matrix mat) {
			this.mat = mat.clone();
			this.size = mat.size();
			aug = new Matrix(size);
		}

		public Matrix getMatrix() {
			return mat;
		}

		public Matrix getAugmentation() {
			return aug;
		}

		public int getSize() {
			return size;
		}

		public int getAugmentedSize() {
			return getSize() * 2;
		}

		public float get(int col, int row) {
			if (row < size) {
				return mat.get(col, row);
			} else {
				return aug.get(col, row - size);
			}
		}

		public void set(int col, int row, float val) {
			if (row < size) {
				mat.set(col, row, val);
			} else {
				aug.set(col, row - size, val);
			}
		}
	}
}
