package com.josephcatrambone.sharpcloud;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by josephcatrambone on 8/17/15.
 */
public class ImageTools {
	public static DoubleMatrix imageFileToMatrix(String filename, int width, int height) {
		try {
			BufferedImage img = ImageIO.read(new File(filename));
			return AWTImageToMatrix(img, width, height);
		} catch(IOException ioe) {
			return null;
		}
	}

	public static boolean matrixToDiskAsImage(DoubleMatrix matrix, String filename) {
		BufferedImage img = matrixToAWTImage(matrix);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch(IOException ioe) {
			return false;
		}
		return true;
	}

	public static DoubleMatrix AWTImageToMatrix(BufferedImage img, int width, int height) {
		if(width != -1 || height != -1) {
			if(width == -1) { width = img.getWidth(); }
			if(height == -1) { height = img.getHeight(); }
			img = (BufferedImage)img.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
		}

		DoubleMatrix matrix = new DoubleMatrix(img.getHeight(), img.getWidth());

		for(int y=0; y < img.getHeight(); y++) {
			for(int x=0; x < img.getWidth(); x++) {
				int rgb = img.getRGB(x, y);
				double a = (rgb >> 32 & 0xff)/255.0;
				double r = (rgb >> 16 & 0xff)/255.0;
				double g = (rgb >> 8 & 0xff)/255.0;
				double b = (rgb & 0xff)/255.0;
				double luminance = Math.sqrt(r*r + g*g + b*b);
				matrix.put(y, x, luminance);
			}
		}

		return matrix;
	}

	public static BufferedImage matrixToAWTImage(DoubleMatrix matrix) {
		BufferedImage img = new BufferedImage(matrix.getColumns(), matrix.getRows(), BufferedImage.TYPE_BYTE_GRAY);
		for(int y=0; y < matrix.getRows(); y++) {
			for(int x=0; x < matrix.getColumns(); x++) {
				img.setRGB(x, y, (int)(255*matrix.get(y, x)));
			}
		}
		return img;
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

	public static Image matrixToFXImage(DoubleMatrix matrix) {
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
		DoubleMatrix dx = image.getColumns(new IntervalRange(0, image.getColumns()-1)).sub(image.getColumns(new IntervalRange(1, image.getColumns())));
		DoubleMatrix dy = image.getRows(new IntervalRange(0, image.getRows() - 1)).sub(image.getRows(new IntervalRange(1, image.getRows())));

		// The gradient calculation leaves the matrices slightly smaller.  Pad them back to normal size.
		dx = DoubleMatrix.concatHorizontally(dx, DoubleMatrix.zeros(dx.getRows(), 1));
		dy = DoubleMatrix.concatVertically(dy, DoubleMatrix.zeros(1, dy.getColumns()));

		// Calculate the gradient products.
		DoubleMatrix xResponse = dx.mul(dx);
		DoubleMatrix yResponse = dy.mul(dy);
		//DoubleMatrix xyResponse = dx.mul(dy);

		//DoubleMatrix det = xResponse.mul(yResponse).sub(xyResponse.mul(xyResponse)); // subi?
		//DoubleMatrix trace = xResponse.add(yResponse);

		return xResponse.addi(yResponse); //det.div(trace);
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
	public static DoubleMatrix findFeaturePoints(DoubleMatrix image, int windowSize, int minFeatures, int maxFeatures) {
		final double FAILURE_THRESHOLD = 0.01;
		final double THRESHOLD_STEP = 0.1;
		double threshold = 1.0;

		// First, find the edges.
		DoubleMatrix edges = edgeDetector(image);

		// Adaptive threshold.
		int featureCount = 0;
		DoubleMatrix candidates = null;
		while(featureCount < minFeatures && threshold > FAILURE_THRESHOLD) {
			threshold -= THRESHOLD_STEP;
			double min = edges.min();
			double max = edges.max()+0.000001;
			candidates = (edges.sub(min)).divi(max-min);
			featureCount = (int)candidates.gti(threshold).sum();
		}

		if(featureCount < minFeatures) {
			System.err.println("Unable to acquire at least " + minFeatures);
			System.err.println("Only " + featureCount + " features acquired.");
			return null;
		}

		// We can expect
		DoubleMatrix results = new DoubleMatrix(maxFeatures, 2+(windowSize*windowSize));
		int currentPoint = 0;

		for(int y=windowSize; y < image.getRows()-windowSize && currentPoint < results.getRows(); y++) {
			for(int x=windowSize; x < image.getColumns()-windowSize && currentPoint < results.getRows(); x++) {
				// Is the point at y x a candidate?
				if(candidates.get(y, x) > 0) {
					// Yes?  So fill in the first two columns with the x and y coordinates.
					results.put(currentPoint, 0, x);
					results.put(currentPoint, 1, y);

					// And the remaining with the window details.
					int cursor = 2;
					for(int i=-windowSize/2; i < windowSize/2; i++) {
						for(int j=-windowSize/2; j < windowSize/2; j++) {
							results.put(currentPoint, cursor, image.get(y+i, x+j));
							cursor += 1;
						}
					}

					// And advance ourselves to the next slot.
					currentPoint += 1;
				}
			}
		}

		// Cut out the empty pieces.
		return results.getRows(new IntervalRange(0, currentPoint));
	}
}
