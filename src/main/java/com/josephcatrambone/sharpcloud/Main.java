package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;

import java.util.Map;

public class Main {
	public static void main(String[] args) {
		final int WINDOW_SIZE = 10;
		//javafx.application.Application.launch(MainWindow.class);

		// Load images and convert to matrix form.
		DoubleMatrix m1 = ImageTools.imageFileToMatrix(args[0], -1, -1);
		DoubleMatrix m2 = ImageTools.imageFileToMatrix(args[1], -1, -1);

		// Find features.
		DoubleMatrix features1 = ImageTools.findFeaturePoints(m1, WINDOW_SIZE, 100, 1000);
		DoubleMatrix features2 = ImageTools.findFeaturePoints(m2, WINDOW_SIZE, 100, 1000);

		// Decompose into windows and point values.
		DoubleMatrix points1 = features1.getColumns(new IntervalRange(0, 2));
		DoubleMatrix points2 = features2.getColumns(new IntervalRange(0, 2));
		DoubleMatrix windows1 = features1.getColumns(new IntervalRange(2, features1.getColumns()));
		DoubleMatrix windows2 = features2.getColumns(new IntervalRange(2, features2.getColumns()));

		// Use the windows (basically feature descriptors) to find matches.
		// Then convert the match indices into a matrix with four columns (two xy pairs for each match).
		DoubleMatrix dismat = HomographyTools.buildDistanceMatrix(windows1, windows2);
		Map<Integer,Integer> matches = HomographyTools.getBestPairs(dismat, -1);
		System.out.println("Points columns: " + points1.getColumns());
		DoubleMatrix correspondences = new DoubleMatrix(points1.getRows(), 4);

		for(Integer a : matches.keySet()) {
			Integer b = matches.get(a);
			correspondences.put(a, 0, points1.get(a, 0));
			correspondences.put(a, 1, points1.get(a, 1));
			correspondences.put(a, 2, points2.get(b, 0));
			correspondences.put(a, 3, points2.get(b, 1));
		}

		// Visualize matches and find homography.
		ImageTools.visualizeCorrespondence(args[0], args[1], correspondences);
		DoubleMatrix homography = new DoubleMatrix(3, 3);
		double homographyError = HomographyTools.solveDLT(correspondences, homography);
		System.out.println("Homography error: " + homographyError);
		System.out.println("Homography: " + homography);
		/*
		for(int i=0; i < features.getRows(); i++) {
			DoubleMatrix image = features.getColumnRange(i, 2, features.getColumns());
			image.reshape(WINDOW_SIZE, WINDOW_SIZE).muli(255);
			ImageTools.matrixToDiskAsImage(image, "output_" + i + "_" + features.get(i, 0) + "_" + features.get(i, 1) + ".png");
		}
		*/
	}
}