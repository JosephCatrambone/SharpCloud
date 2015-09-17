package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;

/**
 * Created by Jo on 9/16/2015.
 */
public class TriangulationTools {
	/*** getFundamentalMatrix
	 * An implementation of the eight-point method.
	 *
	 * @param matches A 2D matrix where each row is a matching pair of points(x y) (x' y')
	 * @return
	 */
	DoubleMatrix getFundamentalMatrix(DoubleMatrix matches) {
		DoubleMatrix equation = new DoubleMatrix(matches.getRows(), 9);
		for(int i=0; i < matches.getRows(); i++) {
			double x1 = matches.get(i, 0);
			double y1 = matches.get(i, 1);
			double x2 = matches.get(i, 2);
			double y2 = matches.get(i, 3);

			equation.put(i, 0, x1*x2);
			equation.put(i, 1, y1*x2);
			equation.put(i, 2, x2);
			equation.put(i, 3, x1*y2);
			equation.put(i, 4, y1*y2);
			equation.put(i, 5, y2);
			equation.put(i, 6, x1);
			equation.put(i, 7, y1);
			equation.put(i, 8, 1);
		}

		// Compute the fundamental matrix with svd.
		DoubleMatrix[] usv = Singular.fullSVD(equation);
		DoubleMatrix fundamental = usv[2].getColumn(8).reshape(3, 3);

		// Constrain by zeroing last SV.
		usv = Singular.fullSVD(fundamental);
		usv[1].put(2, 0); // TODO: verify S shape is linear.
		fundamental = usv[0].mmul(DoubleMatrix.diag(usv[1]).mmul(usv[2]));

		return fundamental;
	}
}
