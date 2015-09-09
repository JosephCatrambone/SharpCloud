package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;

import java.util.Map;

public class Main {
	public static void main(String[] args) {
		final int WINDOW_SIZE = 10;
		//javafx.application.Application.launch(MainWindow.class);

		DoubleMatrix m1 = ImageTools.imageFileToMatrix(args[0], -1, -1);
		DoubleMatrix m2 = ImageTools.imageFileToMatrix(args[1], -1, -1);

		DoubleMatrix features1 = ImageTools.findFeaturePoints(m1, WINDOW_SIZE, 100, 1000);
		DoubleMatrix features2 = ImageTools.findFeaturePoints(m2, WINDOW_SIZE, 100, 1000);

		DoubleMatrix points1 = features1.getColumns(new IntervalRange(0, 2));
		DoubleMatrix points2 = features2.getColumns(new IntervalRange(0, 2));
		DoubleMatrix windows1 = features1.getColumns(new IntervalRange(2, features1.getColumns()));
		DoubleMatrix windows2 = features2.getColumns(new IntervalRange(2, features2.getColumns()));

		DoubleMatrix dismat = HomographyTools.buildDistanceMatrix(windows1, windows2);
		Map<Integer,Integer> matches = HomographyTools.getBestPairs(dismat, -1);
		System.out.println("Points columns: " + points1.getColumns());
		for(Integer a : matches.keySet()) {
			Integer b = matches.get(a);
			System.out.println(points1.getRow(a) + " -> " + points2.getRow(b));
		}
		/*
		for(int i=0; i < features.getRows(); i++) {
			DoubleMatrix image = features.getColumnRange(i, 2, features.getColumns());
			image.reshape(WINDOW_SIZE, WINDOW_SIZE).muli(255);
			ImageTools.matrixToDiskAsImage(image, "output_" + i + "_" + features.get(i, 0) + "_" + features.get(i, 1) + ".png");
		}
		*/
	}
}