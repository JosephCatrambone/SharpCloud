package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;
import org.jblas.ranges.IntervalRange;

import java.awt.*;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;

/**
 * Created by Joseph Catrambone on 9/16/2015.
 * To use this for multiple view reconstruction:
 * - Detect features points and do matching.
 * - Compute the fundamental matrices.
 * - Compute the camera matrices from the fundamental matrices.
 * - Triangulate the points from the fundamental matrices.
 */
public class TriangulationTools {
	/*** getFundamentalMatrix
	 * An implementation of the eight-point method.  Get the fundamental matrix of camera 2.
	 *
	 * @param matches A 2D matrix where each row is a matching pair of points(x y) (x' y')
	 * @return
	 */
	public static DoubleMatrix getFundamentalMatrix(DoubleMatrix matches) {
		// TODO: Normalize the points into the -1 1 range and recenter them.
		//  Normalization isn't working great.  Find out why.

		// Run eight-point algo.
		DoubleMatrix equation = new DoubleMatrix(matches.getRows(), 9);
		for(int i=0; i < matches.getRows(); i++) {
			double x1 = matches.get(i, 0);
			double y1 = matches.get(i, 1);
			double x2 = matches.get(i, 2);
			double y2 = matches.get(i, 3);

			equation.put(i, 0, x1*x2);
			equation.put(i, 1, x1*y2);
			equation.put(i, 2, x1);
			equation.put(i, 3, y1*x2);
			equation.put(i, 4, y1*y2);
			equation.put(i, 5, y1);
			equation.put(i, 6, x2);
			equation.put(i, 7, y2);
			equation.put(i, 8, 1);
		}

		// Compute the fundamental matrix with svd.
		DoubleMatrix[] usv = Singular.fullSVD(equation);
		// We want the last row of VT, so we can just get the last column of V
		DoubleMatrix fundamental = usv[2].getColumn(usv[1].argmin()).reshape(3, 3);

		// Constrain by zeroing last SV.
		usv = Singular.fullSVD(fundamental);
		usv[1].put(2, 0);
		return usv[0].mmul(DoubleMatrix.diag(usv[1])).mmul(usv[2].transpose()).transpose();
	}

	public static DoubleMatrix getEpipole(DoubleMatrix fundamental) {
		// pass fundamental.transpose() for left-epipole.
		// Compute the null subspace of F -> Fe = 0
		DoubleMatrix[] usv = Singular.fullSVD(fundamental);
		DoubleMatrix pole = usv[2].getColumn(usv[2].getColumns() - 1).transpose();
		pole.divi(pole.get(2));
		return pole;
	}

	/*** triangulatePoint
	 * Given the two camera matrices and two matching points in the respective cameras, return a 3D point.
	 * @param camera1 a 3x4 matrix with camera params
	 * @param camera2 a 3x4 matrix with camera params
	 * @param p1 a point in homogeneous coordinates -- assumes a flat array, so .get(0) will get the x.
	 * @param p2 the same point, viewed by the other camera, in homogeneous coordinates.
	 * @return
	 */
	public static DoubleMatrix triangulatePoint(DoubleMatrix camera1, DoubleMatrix camera2, DoubleMatrix p1, DoubleMatrix p2) {
		DoubleMatrix setup = DoubleMatrix.zeros(6, 6);
		// Copy the first camera matrix into set and the first point.
		for(int y=0; y < 3; y++) {
			setup.put(y, 0, camera1.get(y, 0));
			setup.put(y, 1, camera1.get(y, 1));
			setup.put(y, 2, camera1.get(y, 2));
			setup.put(y, 3, camera1.get(y, 3));
			setup.put(y, 4, p1.get(y));
		}
		// Copy last camera and last point.
		for(int y=0; y < 3; y++) {
			setup.put(y+3, 0, camera2.get(y, 0));
			setup.put(y+3, 1, camera2.get(y, 1));
			setup.put(y+3, 2, camera2.get(y, 2));
			setup.put(y+3, 3, camera2.get(y, 3));
			setup.put(y+3, 5, p2.get(y));
		}
		// Calculate point.
		DoubleMatrix[] usv = Singular.fullSVD(setup);
		DoubleMatrix point = usv[2].getColumn(usv[2].getColumns() - 1);
		DoubleMatrix point4d = point.getRows(new IntervalRange(0, 5));
		point4d.divi(point4d.get(3));
		return point4d.getRows(new IntervalRange(0, 3)).transpose();
	}

	public static DoubleMatrix cameraMatrixFromFundamentalMatrix(DoubleMatrix fundamental) {
		final DoubleMatrix leftEpipole = getEpipole(fundamental.transpose());
		final DoubleMatrix skewEpipole = PointTools.skew(leftEpipole.get(0), leftEpipole.get(1), leftEpipole.get(2));
		// I have no idea which of these will produce the better results.
		// Number two is mathematically correct.  Number one has performed better in all tests.
		return DoubleMatrix.concatVertically(skewEpipole.mmul(fundamental.transpose()).transpose(), leftEpipole).transpose();
		//return DoubleMatrix.concatHorizontally(skewEpipole.mmul(fundamental), leftEpipole.transpose());
	}

	/*** getFundamentalError
	 * Given the funamental matrix and two nx3 (augmented) point sets, compute per-point error.
	 * WARNING: points sets will be augmented in-function.
	 * @param fundamental The 3x3 rank-two matrix.
	 * @param p1 An nx2 or nx3 set of augmented points.  One row = x y 1.
	 * @param p2 An nx3 or nx3 set of augmented points.
	 * @param matchThreshold The threshold for a point to be 'matching'.  Ignored if inliers is null.
	 * @param inliers null OR an array of length n which will be filled with true if a point's error is less than matchThreshold or false otherwise.
	 * @return Returns the total error.
	 */
	public static double getFundamentalError(DoubleMatrix fundamental, DoubleMatrix p1, DoubleMatrix p2, double matchThreshold, boolean[] inliers) {
		// Algebraic distance(a,b) = (aFbT)^2
		// Sampson Distance(a,b) = algebraic distance(a,b) * ( 1/((FbT)_x^2 + (FbT)_y^2) + 1/((aF)_x^2+ (aF)_y^2))

		if(p1.getColumns() == 2) {
			p1 = PointTools.augment(p1);
		}
		if(p2.getColumns() == 2) {
			p2 = PointTools.augment(p2);
		}

		// p1 = nx3
		// f = 3x3
		// p2 = nx3
		// p2T = 3xn
		// p1 f p2T = nxn
		final DoubleMatrix pairDistance = p1.mmul(fundamental.mmul(p2.transpose())); // Should be nxn
		final DoubleMatrix pairDistanceSquared = pairDistance.mul(pairDistance); // ^2.

		// Calculate the denominator for the sampson distance.
		DoubleMatrix fb = fundamental.mmul(p2.transpose()).transpose(); // nx3
		DoubleMatrix af = p1.mmul(fundamental); // nx3
		fb.muli(fb); // nx3
		af.muli(af); // nx3
		DoubleMatrix fbInv = DoubleMatrix.ones(fb.getRows(), 1).divi(fb.getColumn(0).add(fb.getColumn(1)));
		DoubleMatrix afInv = DoubleMatrix.ones(af.getRows(), 1).divi(af.getColumn(0).add(af.getColumn(1)));
		DoubleMatrix sampsonScalar = afInv.add(fbInv);

		// Since a row matches with the corresponding column, we only care about the diagonal.
		DoubleMatrix sampsonDistance = pairDistanceSquared.diag().mul(sampsonScalar);

		// Parallel set inliers.
		if(inliers != null) {
			assert(inliers.length == p1.getRows());
			//Arrays.parallelSetAll(inliers, (i) -> sampsonDistance.get(i) < matchThreshold);
			// Bullshit that we can't use lambda here.  No boolean[] operator?
			for(int i=0; i < inliers.length; i++) {
				inliers[i] = sampsonDistance.get(i) < matchThreshold;
			}
		}

		return sampsonDistance.sum();
	}

	public static DoubleMatrix RANSACFundamentalMatrix(DoubleMatrix matches, int subgroupSize, double errorThreshold, int maxIterations) {
		java.util.Random random = new java.util.Random(); // Avoid namespace conflict with jblas.
		DoubleMatrix bestFundamental = null;
		double bestError = Double.POSITIVE_INFINITY;

		int[] subgroup = new int[subgroupSize];
		int[] p1Columns = new int[]{0, 1};
		int[] p2Columns = new int[]{2, 3};
		for(int i=0; i < maxIterations && bestError > errorThreshold; i++) {
			Arrays.parallelSetAll(subgroup, operand -> random.nextInt(matches.getRows()));
			DoubleMatrix matchSubgroup = matches.getRows(subgroup);
			DoubleMatrix candidate = getFundamentalMatrix(matchSubgroup);
			DoubleMatrix pts1 = matchSubgroup.getColumns(p1Columns);
			DoubleMatrix pts2 = matchSubgroup.getColumns(p2Columns);
			double candidateError = getFundamentalError(candidate, pts1, pts2, 0, null);
			if(candidateError < bestError) {
				bestError = candidateError;
				bestFundamental = candidate;
			}
		}

		return bestFundamental;
	}
}
