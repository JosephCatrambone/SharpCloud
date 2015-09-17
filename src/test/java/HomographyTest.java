import com.josephcatrambone.sharpcloud.HomographyTools;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;

import org.junit.Test;


/**
 * Created by Jo on 9/16/2015.
 */
public class HomographyTest {
	@Test
	public void test() {
		//assertArrayEquals("Failure tanh.", m.getRowArray(0), m2.getRowArray(0), 1e-5);
		DoubleMatrix affine = new DoubleMatrix(new double[][]{
			{1, 0, 3},
			{0, 1, 4},
			{0, 0, 1}
		}).reshape(3, 3);

		// Make a bunch of correspondences.
		DoubleMatrix pts = new DoubleMatrix(new double[][] {
			{0, 0},
			{10, 30},
			{-1, -10},
			{30, 2},
			{18, 21},
			{-10, 10},
			{10, -10}
		});

		// Transform the points with the affeine to get the new poitns.
		DoubleMatrix pts2 = DoubleMatrix.concatHorizontally(pts, DoubleMatrix.ones(pts.getRows(), 1)).mmul(affine);
		for(int i=0; i < pts.getRows(); i++) {
			pts2.put(i, 0, pts2.get(i, 0)/pts2.get(i, 2));
			pts2.put(i, 1, pts2.get(i, 1)/pts2.get(i, 2));
		}

		// Combine results, stripping the normalization factor on the right.
		DoubleMatrix pairs = DoubleMatrix.concatHorizontally(pts, pts2.getColumns(new IntervalRange(0, 2)));

		// Verify homography recovered
		DoubleMatrix homography = new DoubleMatrix(3, 3);
		HomographyTools.solveDLT(pairs, homography);

		// Rescale and compare.
		homography.divi(homography.get(2, 2));
		org.junit.Assert.assertTrue(affine.squaredDistance(homography) < 0.1);

		// Verify ransac is sane.
		homography = HomographyTools.RANSACHomography(pairs, 4, 0.001, 10000);
		homography.divi(homography.get(2, 2));
		System.err.println(homography);
		org.junit.Assert.assertTrue(affine.squaredDistance(homography) < 0.1);
	}
}
