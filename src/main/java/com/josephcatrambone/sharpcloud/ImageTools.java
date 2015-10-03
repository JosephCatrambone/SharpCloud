package com.josephcatrambone.sharpcloud;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by josephcatrambone on 8/17/15.
 */
public class ImageTools {
	public static void visualizeCorrespondence(String image1, String image2, DoubleMatrix points) {
		try {
			Random random = new Random();
			BufferedImage img1 = ImageIO.read(new File(image1));
			BufferedImage img2 = ImageIO.read(new File(image2));
			BufferedImage result = new BufferedImage(img1.getWidth()+img2.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D)result.getGraphics();
			g.drawImage(img1, 0, 0, null);
			g.drawImage(img2, img1.getWidth(), 0, null);
			for(int i=0; i < points.getRows(); i++) {
				g.setColor(new java.awt.Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
				g.drawLine((int)points.get(i, 0), (int)points.get(i, 1), (int)points.get(i, 2)+img1.getWidth(), (int)points.get(i, 3));
			}
			ImageIO.write(result, "png", new File("result.png"));
		} catch(IOException ioe) {
		}
	}

	public static DoubleMatrix imageFileToMatrix(String filename, int width, int height) {
		try {
			BufferedImage img = ImageIO.read(new File(filename));
			return AWTImageToMatrix(img, width, height);
		} catch(IOException ioe) {
			return null;
		}
	}

	public static boolean matrixToDiskAsImage(DoubleMatrix matrix, String filename) {
		return matrixToDiskAsImage(matrix, filename, true);
	}

	public static boolean matrixToDiskAsImage(DoubleMatrix matrix, String filename, boolean normalize) {

		// Normalize contrast to 0-1.
		if(normalize) {
			double min = matrix.min();
			double max = matrix.max();
			DoubleMatrix preimg = matrix.sub(min).div(max - min);
			matrix = preimg;
		}

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
				img.setRGB(x, y, (int) (255 * matrix.get(y, x)));
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
		parameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
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
				pw.setColor(x, y, javafx.scene.paint.Color.gray(color));
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

	public static double getGaussian(double x, double y, double sigma) {
		return Math.exp(-(x*x + y*y)/(2.0*sigma*sigma))/(2.0*sigma*sigma*Math.PI);
	}

	public static DoubleMatrix getGaussianMatrix(int width, int height, double sigma) {
		int halfWidth = width/2;
		int halfHeight = height/2;
		double twoSigmaSq = 2.0*sigma*sigma;
		double denom = Math.PI*twoSigmaSq;
		DoubleMatrix gaussian = new DoubleMatrix(width, height);
		for(int y=0; y < height; y++) {
			for(int x=0; x < width; x++) {
				double dx = x-halfWidth;
				double dy = y-halfHeight;
				double g = Math.exp(-(dx*dx + dy*dy)/twoSigmaSq)/denom;
				gaussian.put(y, x, g);
			}
		}
		return gaussian;
	}

	public static DoubleMatrix convolve2D(DoubleMatrix matrix, DoubleMatrix kernel) {
		DoubleMatrix result = new DoubleMatrix(matrix.getRows(), matrix.getColumns());
		int halfWidth = kernel.getColumns()/2;
		int halfHeight = kernel.getRows()/2;
		for(int r=halfHeight; r < matrix.getRows()-halfHeight; r++) {
			for(int c=halfWidth; c < matrix.getColumns()-halfWidth; c++) {
				double accumulator = 0.0;
				for(int y=0; y < kernel.getRows(); y++) {
					for(int x=0; x < kernel.getColumns(); x++) {
						double m = matrix.get(r-halfHeight+y, c-halfWidth+x);
						double k = kernel.get(y, x);
						accumulator += m*k;
					}
				}
				result.put(r, c, accumulator);
			}
		}
		return result;
	}

	public static DoubleMatrix getHarrisResponse(DoubleMatrix image, int windowSize) {
		final double sigma1 = 0.2;
		final double sigma2 = 0.01;
		final double epsilon = 0.1; // To prevent divide by zero.

		final DoubleMatrix gaussian1 = getGaussianMatrix(windowSize, windowSize, sigma1);
		final DoubleMatrix gaussian2 = getGaussianMatrix(windowSize, windowSize, sigma2);
		final DoubleMatrix horizontalEdge = new DoubleMatrix(new double[][] {
			{-1, 0, 1},
			{-1, 0, 1},
			{-1, 0, 1}
		});
		final DoubleMatrix verticalEdge = horizontalEdge.transpose();

		final DoubleMatrix img = convolve2D(image, gaussian1);

		final DoubleMatrix Ix = convolve2D(img, horizontalEdge);
		final DoubleMatrix Iy = convolve2D(img, verticalEdge);

		final DoubleMatrix Ix2 = Ix.mul(Ix);
		final DoubleMatrix Iy2 = Iy.mul(Iy);
		final DoubleMatrix Ixy = Ix.mul(Iy);

		// You have to do gaussian filtering BEFORE GRADIENT CALCULATION OR YOU GET ZERO.
		final double gaussianSum = gaussian2.sum();
		final DoubleMatrix gIx2 = convolve2D(Ix2, gaussian2).divi(gaussianSum);
		final DoubleMatrix gIy2 = convolve2D(Iy2, gaussian2).divi(gaussianSum);
		final DoubleMatrix gIxy = convolve2D(Ixy, gaussian2).divi(gaussianSum);

		// (ix2*iy2 - ixy*ixy) / (ix2+iy2)
		return gIx2.mul(gIy2).sub(gIxy.mul(gIxy)).div(gIx2.add(gIy2).add(epsilon));
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
		// First, find the corners.
		DoubleMatrix harrisResponse = getHarrisResponse(image, windowSize);
		double meanResponse = harrisResponse.mean();
		double maxResponse = harrisResponse.max();
		double threshold = (5*meanResponse+maxResponse)/6.0; // Somewhere above average and max, slightly favoring mean.

		int featureCount = (int)harrisResponse.gt(meanResponse).sum();

		// We can expect
		DoubleMatrix results = new DoubleMatrix(featureCount, 2+(windowSize*windowSize));
		int currentPoint = 0;
		for(int y=windowSize; y < image.getRows()-windowSize && currentPoint < results.getRows(); y++) {
			for(int x=windowSize; x < image.getColumns()-windowSize && currentPoint < results.getRows(); x++) {
				// Is the point at y x a candidate?
				if(harrisResponse.get(y, x) > threshold) {
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

	/***
	 * Given two matrices of examples (where each row is one example), computes the normalized-cross-correlation value.
	 * Returns a matrix of distances size |A| * |B|.
	 * @param candidateSetA
	 * @param candidateSetB
	 * @return
	 */
	public static DoubleMatrix buildDistanceMatrix(DoubleMatrix candidateSetA, DoubleMatrix candidateSetB) {
		DoubleMatrix result = DoubleMatrix.zeros(candidateSetA.getRows(), candidateSetB.getRows());

		for(int a=0; a < candidateSetA.getRows(); a++) {
			DoubleMatrix exampleA = candidateSetA.getRow(a);
			for(int b=0; b < candidateSetB.getRows(); b++) {
				DoubleMatrix exampleB = candidateSetB.getRow(b);
				result.put(a, b, exampleA.squaredDistance(exampleB));
			}
		}

		return result;
	}

	public static int[] getBestPairs(DoubleMatrix distanceMatrix) {
		int[] pairs = new int[distanceMatrix.getRows()];
		for(int a=0; a < distanceMatrix.getRows(); a++) {
			double bestValue = Double.MAX_VALUE;
			int bestIndex = -1;
			for(int b=0; b < distanceMatrix.getColumns(); b++) {
				// Is this our best match?
				double matchCoefficient = distanceMatrix.get(a,b);
				if(matchCoefficient < bestValue) {
					bestValue = matchCoefficient;
					bestIndex = b;
				}
			}
			pairs[a] = bestIndex;
		}
		return pairs;
	}

	public static DoubleMatrix getCorrespondences(DoubleMatrix mat1, DoubleMatrix mat2) {
		int WINDOW_SIZE = 10;
		// Find features.
		DoubleMatrix features1 = ImageTools.findFeaturePoints(mat1, WINDOW_SIZE);
		DoubleMatrix features2 = ImageTools.findFeaturePoints(mat2, WINDOW_SIZE);

		// Decompose into windows and point values.
		DoubleMatrix points1 = features1.getColumns(new IntervalRange(0, 2));
		DoubleMatrix points2 = features2.getColumns(new IntervalRange(0, 2));
		DoubleMatrix windows1 = features1.getColumns(new IntervalRange(2, features1.getColumns()));
		DoubleMatrix windows2 = features2.getColumns(new IntervalRange(2, features2.getColumns()));

		// Use the windows (basically feature descriptors) to find matches.
		// Then convert the match indices into a matrix with four columns (two xy pairs for each match).
		DoubleMatrix dismat = ImageTools.buildDistanceMatrix(windows1, windows2);
		int[] matches = ImageTools.getBestPairs(dismat);
		DoubleMatrix correspondences = new DoubleMatrix(points1.getRows(), 4);

		for(int a = 0; a < matches.length; a++) {
			int b = matches[a];
			if(b == -1) { continue; }
			correspondences.put(a, 0, points1.get(a, 0));
			correspondences.put(a, 1, points1.get(a, 1));
			correspondences.put(a, 2, points2.get(b, 0));
			correspondences.put(a, 3, points2.get(b, 1));
		}

		return correspondences;
	}
}
