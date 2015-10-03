package com.josephcatrambone.sharpcloud;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;

import java.util.Map;

public class Main {
	public static void main(String[] args) {
		final int WINDOW_SIZE = 10;
		final double INLIER_THRESHOLD = 10.0;
		//javafx.application.Application.launch(MainWindow.class);

		// Load images and convert to matrix form.
		DoubleMatrix m1 = ImageTools.imageFileToMatrix(args[0], -1, -1);
		DoubleMatrix m2 = ImageTools.imageFileToMatrix(args[1], -1, -1);

		DoubleMatrix correspondences = ImageTools.getCorrespondences(m1, m2);
		DoubleMatrix pts1 = correspondences.getColumns(new int[]{0, 1});
		DoubleMatrix pts2 = correspondences.getColumns(new int[]{2, 3});

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

		DoubleMatrix fundamental = TriangulationTools.RANSACFundamentalMatrix(correspondences, 8, 0.01, 100000);
		DoubleMatrix origin = new CameraMatrix().getCombinedMatrix();
		DoubleMatrix camera2 = TriangulationTools.cameraMatrixFromFundamentalMatrix(fundamental);
		//boolean[] inliers = new boolean[pts1.getRows()];
		//TriangulationTools.getFundamentalError(fundamental, pts1, pts2, INLIER_THRESHOLD, inliers);

		for(int i=0; i < correspondences.getRows(); i++) {
			if(true) { //inliers[i]) {
				DoubleMatrix p3d = TriangulationTools.triangulatePoint(
					origin,
					camera2,
					PointTools.augment(pts1.getRow(i)),
					PointTools.augment(pts2.getRow(i))
				);
				System.out.println(p3d.get(0) + "," + p3d.get(1) + "," + p3d.get(2));
			}
		}
	}
}