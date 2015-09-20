package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Jo on 9/7/2015.
 */
public class HomographyTools {
	/***
	 * Given two matrices of examples (where each row is one example), computes the normalized-cross-correlation value.
	 * Returns a matrix of distances size |A| * |B|.
	 * @param candidateSetA
	 * @param candidateSetB
	 * @return
	 */
	public static DoubleMatrix buildDistanceMatrix(DoubleMatrix candidateSetA, DoubleMatrix candidateSetB) {
		DoubleMatrix result = DoubleMatrix.zeros(candidateSetA.getRows(), candidateSetB.getRows());
		double scalar = 1.0/candidateSetA.getColumns();
		double BADFILL = -1; // Used when STDDEV is zero.

		for(int a=0; a < candidateSetA.getRows(); a++) {
			DoubleMatrix exampleA = candidateSetA.getRow(a);
			double meanA = exampleA.mean();
			double stddevA = Math.sqrt(exampleA.sub(meanA).mul(exampleA.sub(meanA)).sum()*scalar);
			for(int b=0; b < candidateSetB.getRows(); b++) {
				DoubleMatrix exampleB = candidateSetB.getRow(b);
				double meanB = exampleB.mean();
				double stddevB = Math.sqrt(exampleB.sub(meanB).mul(exampleB.sub(meanB)).sum()*scalar);
				if(stddevA == 0 || stddevB == 0) { result.put(a, b, BADFILL); continue; }

				// No need to reshape our examples, since NCC doesn't care.
				// NCC(a,b) = 1/size * sum{((pixel in a - mean pixel in a)/(standard deviation in a)) * ((p in b - mean b)/stddev(b))
				double accumulator = 0;
				for(int i=0; i < candidateSetA.getColumns(); i++) {
					accumulator += ((exampleA.get(i)-meanA)/stddevA)*((exampleB.get(i)-meanB)/stddevB);
				}
				result.put(a, b, accumulator*scalar);
			}
		}

		return result;
	}

	public static Map<Integer, Integer> getBestPairs(DoubleMatrix distanceMatrix, double threshold) {
		Map <Integer,Integer> pairs = new HashMap<>();
		for(int a=0; a < distanceMatrix.getRows(); a++) {
			double bestValue = -1.0;
			int bestIndex = -1;
			for(int b=0; b < distanceMatrix.getColumns(); b++) {
				if(a == b) { continue; } // Avoid self-match.

				// Is this our best match?
				double matchCoefficient = distanceMatrix.get(a,b);
				if(matchCoefficient < threshold) { continue; }
				if(matchCoefficient < bestValue) { continue; }

				bestValue = matchCoefficient;
				bestIndex = b;
			}
			if(bestIndex > 0) {
				pairs.put(a, bestIndex);
			}
		}
		return pairs;
	}

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
