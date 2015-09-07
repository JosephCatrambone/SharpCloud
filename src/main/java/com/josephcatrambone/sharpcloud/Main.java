package com.josephcatrambone.sharpcloud;

import javafx.application.Application;

import org.jblas.DoubleMatrix;

public class Main {
	public static void main(String[] args) {
		final int WINDOW_SIZE = 10;
		//javafx.application.Application.launch(MainWindow.class);

		DoubleMatrix m = ImageTools.ImageFileToMatrix("test.png", -1, -1);
		//DoubleMatrix edges = ImageTools.edgeDetector(m);
		DoubleMatrix features = ImageTools.findFeaturePoints(m, WINDOW_SIZE, 5, 10);
		for(int i=0; i < features.getRows(); i++) {
			DoubleMatrix image = features.getColumnRange(i, 2, features.getColumns());
			image.reshape(WINDOW_SIZE, WINDOW_SIZE).muli(255);
			ImageTools.MatrixToDiskAsImage(image, "output_" + i + "_" + features.get(i, 0) + "_" + features.get(i,1) +  ".png");
		}
	}
}