package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;

import java.util.HashMap;
import java.util.Map;

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
}
