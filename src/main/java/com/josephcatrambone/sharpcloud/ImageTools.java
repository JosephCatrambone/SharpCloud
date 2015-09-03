package com.josephcatrambone.sharpcloud;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.IndicesRange;
import org.jblas.ranges.IntervalRange;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Created by josephcatrambone on 8/17/15.
 */
public class ImageTools {
	public static DoubleMatrix ImageFileToMatrix(String filename, int width, int height) {
		Image img = new Image("file:" + filename, width, height, true, true, false);
		return FXImageToMatrix(img, width, height);
	}

	public static DoubleMatrix FXImageToMatrix(Image image, int width, int height) {
		if(width == -1) { width = (int)image.getWidth(); }
		if(height == -1) { height = (int)image.getHeight(); }

		// Return a matrix with the given dimensions, image scaled to the appropriate size.
		// If the aspect ratio of the image is different, fill with black around the edges.
		ImageView imageView = new ImageView();
		imageView.setImage(image);
		imageView.setFitWidth(width);
		imageView.setFitHeight(height);
		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);

		WritableImage scaledImage = new WritableImage(width, height);

		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT);
		imageView.snapshot(parameters, scaledImage);

		PixelReader img = scaledImage.getPixelReader();
		DoubleMatrix output = new DoubleMatrix(height, width);
		for(int y=0; y < height; y++) {
			for(int x=0; x < width; x++) {
				output.put(y, x, img.getColor(x, y).getBrightness());
			}
		}
		return output;
	}


	public static Image MatrixToFXImage(DoubleMatrix matrix) {
		WritableImage img = new WritableImage(matrix.getColumns(), matrix.getRows());
		PixelWriter pw = img.getPixelWriter();

		double min = matrix.min();
		double max = matrix.max()+0.00001;

		for(int y=0; y < matrix.getRows(); y++) {
			for(int x=0; x < matrix.getColumns(); x++) {
				double color = matrix.get(y, x);
				color = (color-min)/(max-min);
				pw.setColor(x, y, Color.gray(color));
			}
		}

		return img;
	}

	public static boolean FXImageToDisk(Image img, String filename) {
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(filename));
		} catch(IOException ioe) {
			return false;
		}
		return true;
	}

	public static DoubleMatrix edgeDetector(DoubleMatrix image) {
		// Calculate X and Y gradients
		DoubleMatrix dx = image.getColumns(new IntervalRange(0, image.getColumns()-1)).subi(image.getColumns(new IntervalRange(1, image.getColumns())));
		DoubleMatrix dy = image.getRows(new IntervalRange(0, image.getRows() - 1)).subi(image.getRows(new IntervalRange(1, image.getRows())));

		// The gradient calculation leaves the matrices slightly smaller.  Pad them back to normal size.
		dx = DoubleMatrix.concatHorizontally(dx, DoubleMatrix.zeros(dx.getRows(), 1));
		dy = DoubleMatrix.concatVertically(dy, DoubleMatrix.zeros(1, dy.getColumns()));

		// Calculate the gradient products.
		DoubleMatrix xResponse = dx.mul(dx);
		DoubleMatrix yResponse = dy.mul(dy);
		DoubleMatrix xyResponse = dx.mul(dy);

		DoubleMatrix det = xResponse.mul(yResponse).sub(xyResponse.mul(xyResponse)); // subi?
		DoubleMatrix trace = xResponse.add(yResponse);

		return det; //det.div(trace);
	}

	/*** Given an image matrix, and a feature size, calculate feature points.
	 * The result will be a matrix with one row = one feature.
	 * Column zero will be the x coordinate of the feature.
	 * Column one will be the y coordinate of the feature.
	 * The remaining columns are the flattened intensities of the pixels in the window.
	 * @param windowSize The region that should be sampled.  5x5 is often enough.
	 * @param image A single-channel greyscale image.
	 * @return
	 */
	public static DoubleMatrix findFeaturePoints(DoubleMatrix image, int windowSize) {
		return null;
	}
}
