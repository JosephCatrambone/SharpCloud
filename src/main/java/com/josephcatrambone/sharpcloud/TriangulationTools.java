package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;
import org.jblas.ranges.IntervalRange;

/**
 * Created by Jo on 9/16/2015.
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
		return usv[0].mmul(DoubleMatrix.diag(usv[1])).mmul(usv[2].transpose());
	}

	public static DoubleMatrix getEpipole(DoubleMatrix fundamental) {
		// pass fundamental.transpose() for left-epipole.
		// Compute the null subspace of F -> Fe = 0
		DoubleMatrix[] usv = Singular.fullSVD(fundamental);
		DoubleMatrix pole = usv[2].getRow(usv[2].getRows() - 1);
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
		DoubleMatrix leftEpipole = getEpipole(fundamental.transpose());
		DoubleMatrix skewEpipole = PointTools.skew(leftEpipole.get(0), leftEpipole.get(1), leftEpipole.get(2));
		return DoubleMatrix.concatVertically(skewEpipole.mmul(fundamental.transpose()).transpose(), leftEpipole).transpose();
	}
}
