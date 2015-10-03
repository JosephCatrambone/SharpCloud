package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by Jo on 9/7/2015.
 */
public class HomographyTools {
	/*** solveDLT
	 * Given an array with four columns (x1, y1, x2, y2), solve for the homography, returning the error.
	 * @param matches
	 * @return
	 */
	public static double solveDLT(DoubleMatrix matches, DoubleMatrix homography) {
		DoubleMatrix m = new DoubleMatrix(matches.getRows()*2, 9);
		// -x1 -y1 -1 0 0 0 x1x2 y1x2 x2
		// 0 0 0 -x1 -y1 -1 x1y2 y1y2 y2
		for(int i=0; i < matches.getRows(); i++) {
			double x1 = matches.get(i, 0);
			double y1 = matches.get(i, 1);
			double x2 = matches.get(i, 2);
			double y2 = matches.get(i, 3);
			//
			// -x1 -y1 -1
			m.put(i*2, 0, -x1);	m.put(i*2, 1, -y1); m.put(i*2, 2, -1);
			// 0 0 0
			m.put(i*2, 3, 0); m.put(i*2, 4, 0);m.put(i*2, 5, 0);
			// x2x2 y1x2 x2
			m.put(i*2, 6, x1*x2); m.put(i*2, 7, y1*x2); m.put(i*2, 8, x2);
			//
			// 0 0 0
			m.put(1+i*2, 0, 0); m.put(1+i*2, 1, 0);m.put(1+i*2, 2, 0);
			// -x1 -y1 -1
			m.put(1+i*2, 3, -x1); m.put(1+i*2, 4, -y1);m.put(1+i*2, 5, -1);
			// 0 0 0
			m.put(1+i*2, 6, x1*y2); m.put(1+i*2, 7, y1*y2);m.put(1+i*2, 8, y2);
		}
		// Now that the matrix is built, solve the homography.
		DoubleMatrix[] usv = Singular.fullSVD(m);
		int minRowIndex = usv[1].argmin();
		homography.addi(usv[2].getColumn(minRowIndex).reshape(3, 3).transpose());
		homography.divi(homography.get(2, 2));
		return usv[1].get(minRowIndex);
	}

	public static DoubleMatrix RANSACHomography(DoubleMatrix matches, int subgroupSize, double threshold, int maxIterations) {
		// Try getting a homography with a bunch of randomly chosen subgroups.
		// Init our return matrix, and split the points into two [x, y, 1] row-based groups.
		// We need this so when we multiply out p*h = p', we can quickly compare the two.
		DoubleMatrix homography = null;
		DoubleMatrix points1 = DoubleMatrix.concatHorizontally(matches.getColumns(new int[]{0, 1}), DoubleMatrix.ones(matches.getRows(), 1));
		DoubleMatrix points2 = DoubleMatrix.concatHorizontally(matches.getColumns(new int[]{2, 3}), DoubleMatrix.ones(matches.getRows(), 1));
		Random random = new Random();

		double error = Double.POSITIVE_INFINITY;
		while(error > threshold && maxIterations-- > 0) {
			int[] selection = new int[subgroupSize];
			Arrays.parallelSetAll(selection, i -> random.nextInt(matches.getRows()));
			DoubleMatrix candidate = new DoubleMatrix(3, 3);
			double selectionError = solveDLT(matches.getRows(selection), candidate);
			double tempError = candidate.mmul(points1.transpose()).transpose().distance2(points2);
			if(tempError < error) {
				error = tempError;
				homography = candidate;
			}
		}
		return homography;
	}
}
