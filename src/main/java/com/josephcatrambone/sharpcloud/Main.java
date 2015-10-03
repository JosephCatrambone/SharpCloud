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

		DoubleMatrix correspondences = ImageTools.getCorrespondences(m1, m2);

		// Visualize matches and find homography.
		ImageTools.visualizeCorrespondence(args[0], args[1], correspondences);
		//DoubleMatrix homography = new DoubleMatrix(3, 3);
		//double homographyError = HomographyTools.solveDLT(correspondences, homography);
		//DoubleMatrix homography = HomographyTools.RANSACHomography(correspondences, (int)(correspondences.getRows()*0.1), 0.01, 1000);

		/*
		for(int i=0; i < features.getRows(); i++) {
			DoubleMatrix image = features.getColumnRange(i, 2, features.getColumns());
			image.reshape(WINDOW_SIZE, WINDOW_SIZE).muli(255);
			ImageTools.matrixToDiskAsImage(image, "output_" + i + "_" + features.get(i, 0) + "_" + features.get(i, 1) + ".png");
		}
		*/

		DoubleMatrix fundamental = TriangulationTools.RANSACFundamentalMatrix(correspondences, 8, 1.0, 1000);
		DoubleMatrix origin = new CameraMatrix().getCombinedMatrix();
		DoubleMatrix camera2 = TriangulationTools.cameraMatrixFromFundamentalMatrix(fundamental);
		fundamental.print();

		for(int i=0; i < correspondences.getRows(); i++) {
			DoubleMatrix pair = correspondences.getRow(i);
			DoubleMatrix p3d = TriangulationTools.triangulatePoint(
				origin,
				camera2,
				PointTools.augment(pair.getColumns(new int[]{0, 1})),
				PointTools.augment(pair.getColumns(new int[]{2, 3}))
			);
			System.out.println(p3d.get(0) + "," + p3d.get(1) + "," + p3d.get(2));
		}
	}
}