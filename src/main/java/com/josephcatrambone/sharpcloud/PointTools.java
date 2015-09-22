package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;

/**
 * Created by Jo on 9/20/2015.
 */
public class PointTools {
	public static DoubleMatrix augment(DoubleMatrix mat) {
		return DoubleMatrix.concatHorizontally(mat, DoubleMatrix.ones(mat.getRows(), 1));
	}

	public static void deaugment3D(DoubleMatrix mat) {
		for(int i=0; i < mat.getRows(); i++) {
			double w = mat.get(i, 2);
			mat.put(i, 0, mat.get(i, 0)/w);
			mat.put(i, 1, mat.get(i, 1)/w);
			mat.put(i, 2, mat.get(i, 2)/w);
		}
	}

	public static void deaugment4D(DoubleMatrix mat) {
		for(int i=0; i < mat.getRows(); i++) {
			double w = mat.get(i, 3);
			mat.put(i, 0, mat.get(i, 0)/w);
			mat.put(i, 1, mat.get(i, 1)/w);
			mat.put(i, 2, mat.get(i, 2)/w);
			mat.put(i, 3, mat.get(i, 3)/w);
		}
	}

	/*** skew
	 * Return the skew matrix S(T).
	 * If T is a 3x1 point, T cross A = skew(T)*A.
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static DoubleMatrix skew(double x, double y, double z) {
		return new DoubleMatrix(new double[][]{
			{0, -z, y},
			{z, 0, -x},
			{-y, x, 0}
		});
	}
}
