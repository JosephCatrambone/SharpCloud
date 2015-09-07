package com.josephcatrambone.sharpcloud;

import javafx.application.Application;

import org.jblas.DoubleMatrix;

public class Main {
	public static void main(String[] args) {
		//javafx.application.Application.launch(MainWindow.class);

		DoubleMatrix m = ImageTools.ImageFileToMatrix("test.png", -1, -1);
		DoubleMatrix edges = ImageTools.edgeDetector(m);
		ImageTools.MatrixToDiskAsImage(edges, "output.png");
	}
}