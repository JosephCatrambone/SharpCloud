package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;

/**
 * Created by josephcatrambone on 9/28/15.
 */
public class CameraMatrix extends DoubleMatrix {

	// Intrinsic params.
	public double focalX = 1.0;
	public double focalY = 1.0;
	public double principalPointX = 0.0;
	public double principalPointY = 0.0;

	// Extrinsic params.
	public double rotX = 0.0;
	public double rotY = 0.0;
	public double rotZ = 0.0;
	public double translationX = 0.0;
	public double translationY = 0.0;
	public double translationZ = 0.0;

	/***
	 * Given a 4xn array of augmented points with one row = [x, y, z, 1], return 3xn un-normalized outputs. [x, y, ?]
	 * @param points
	 * @return Nx3 row = point, with z unnormalized.
	 */
	public DoubleMatrix projectPoints(DoubleMatrix points) {
		assert(points.getColumns() == 4);
		final DoubleMatrix combined = getCombinedMatrix();
		DoubleMatrix projected = combined.mmul(points.transpose()).transpose();
		return projected;
	}

	public DoubleMatrix getCombinedMatrix() {
		DoubleMatrix proj = new DoubleMatrix(new double[][] {
			{1, 0, 0, 0},
			{0, 1, 0, 0},
			{0, 0, 1, 0}
		});

		return getIntrinsicMatrix().mmul(proj).mmul(getExtrinsicMatrix());
	}

	public DoubleMatrix getIntrinsicMatrix() {
		return new DoubleMatrix(new double[][] {
			{focalX, 0, principalPointX},
			{0, focalY, principalPointY},
			{0, 0, 1}
		});
	}

	/***
	 * @return Returns a matrix as RzRyRx (roll pitch yaw) style R3 mat.
	 */
	public DoubleMatrix getRotationMatrix() {
		double ct, st;

		ct = Math.cos(rotX);
		st = Math.sin(rotX);
		DoubleMatrix rx = new DoubleMatrix(new double[][]{
			{1, 0, 0},
			{0, ct, -st},
			{0, st, ct}
		});

		ct = Math.cos(rotY);
		st = Math.sin(rotY);
		DoubleMatrix ry = new DoubleMatrix(new double[][] {
			{ct, 0, st},
			{0, 1, 0},
			{-st, 0, ct}
		});

		ct = Math.cos(rotZ);
		st = Math.sin(rotZ);
		DoubleMatrix rz = new DoubleMatrix(new double[][] {
			{ct, -st, 0},
			{st, ct, 0},
			{0, 0, 1}
		});

		return rz.mmul(ry).mmul(rx);
	}

	public DoubleMatrix getExtrinsicMatrix() {
		DoubleMatrix rot = this.getRotationMatrix();
		DoubleMatrix extrinsic = new DoubleMatrix(4, 4);

		// Copy rot mat into extrinsic.
		for(int i=0; i < 3; i++) {
			for(int j=0; j < 3; j++) {
				extrinsic.put(i, j, rot.get(i, j));
			}
		}

		// Insert translation.
		extrinsic.put(0, 3, translationX);
		extrinsic.put(1, 3, translationY);
		extrinsic.put(2, 3, translationZ);
		extrinsic.put(3, 3, 1);

		return extrinsic;
	}

}
