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
		double angle = Math.PI/3;
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		DoubleMatrix affine = new DoubleMatrix(new double[][]{
			{c, -s, 3},
			{s, c, 4},
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
		DoubleMatrix ptsAug = DoubleMatrix.concatHorizontally(pts, DoubleMatrix.ones(pts.getRows(), 1));

		// Transform the points with the affeine to get the new poitns.
		//DoubleMatrix pts2Aug = ptsAug.mmul(affine);
		// aff.dot(a.T).T <- This works.  ^ This doesn't.
		DoubleMatrix pts2Aug = affine.mmul(ptsAug.transpose()).transpose();

		for(int i=0; i < pts.getRows(); i++) {
			pts2Aug.put(i, 0, pts2Aug.get(i, 0)/pts2Aug.get(i, 2));
			pts2Aug.put(i, 1, pts2Aug.get(i, 1)/pts2Aug.get(i, 2));
		}
		DoubleMatrix pts2 = pts2Aug.getColumns(new IntervalRange(0, 2));

		// Combine results, stripping the normalization factor on the right.
		DoubleMatrix pairs = DoubleMatrix.concatHorizontally(pts, pts2);

		// Verify homography recovered
		DoubleMatrix homography = new DoubleMatrix(3, 3);
		HomographyTools.solveDLT(pairs, homography);

		// Rescale and compare.
		homography.divi(homography.get(2, 2));
		homography.print();
		org.junit.Assert.assertTrue(affine.squaredDistance(homography) < 0.1);

		// Verify ransac is sane.
		homography = HomographyTools.RANSACHomography(pairs, 4, 0.001, 10000);
		System.err.println(homography);
		org.junit.Assert.assertTrue(affine.squaredDistance(homography) < 0.1);
	}
}
