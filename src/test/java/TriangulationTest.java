import com.josephcatrambone.sharpcloud.PointTools;
import com.josephcatrambone.sharpcloud.TriangulationTools;
import org.jblas.DoubleMatrix;
import org.jblas.Singular;
import org.jblas.ranges.IntervalRange;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Jo on 9/16/2015.
 */
public class TriangulationTest {
	final double ERROR_THRESHOLD = 0.1;
	final int NUM_POINTS = 100;
	DoubleMatrix pts; // Randomly generated points nx3
	DoubleMatrix ptsAug; // Augmented nx4 points.
	DoubleMatrix camera1; // Randomly rotated camera.
	DoubleMatrix camera2; // Offset from camrea 1.
	DoubleMatrix pts1; // Points projected by camera 1.  nx3
	DoubleMatrix pts2;

	private DoubleMatrix makeCamera(double focalDistance, double x, double y, double z, double rot) {
		double ct = Math.cos(rot);
		double st = Math.sin(rot);
		return new DoubleMatrix(new double[][]{
			{ct/focalDistance, -st, 0, x},
			{st, ct/focalDistance, 0, y},
			{0, 0, 1, z}
		});
	}

	@Before
	public void setup() {
		java.util.Random random = new java.util.Random(); // Avoid name conflict with rand mat.

		// Random points -- perhaps bad for a deterministic test.
		pts = DoubleMatrix.rand(NUM_POINTS, 3);

		// Drift for camera 2.
		double f = random.nextDouble()+0.1; // Focal distance.
		double theta = random.nextDouble()*0.1*Math.PI/2; // Rotation.
		double dx = random.nextDouble()*0.1; // Small offset.
		double dy = random.nextDouble()*0.1; // Small offset.
		double dz = random.nextDouble()*0.1; // Small offset.

		// 3D rotation matrix about z
		// ct -st 0
		// st ct 0
		// 0 0 1

		// Known projection matrices.
		// Intrinsic camera params...
		// 2D translation * 2D scaling * 2D shear * ...
		// Extrinsic params
		// 3D translation * 3D rotation
		camera1 = makeCamera(f, 0, 0, 0, 0);
		camera2 = makeCamera(f, dx, dy, dz, theta);

		// Augment the points
		ptsAug = PointTools.augment(pts);

		// Transform the points with the proj to get the new poitns.
		//DoubleMatrix pts2Aug = ptsAug.mmul(affine); does not work
		// pts -> num_pts x 4
		// camera -> 3x4
		// (c*pT)T -> (3x4(nx4)T)T -> 3xnT -> nx3
		pts1 = camera1.mmul(ptsAug.transpose()).transpose();
		pts2 = camera2.mmul(ptsAug.transpose()).transpose();

		PointTools.deaugment3D(pts1);
		PointTools.deaugment3D(pts2);
	}

	@Test
	public void testKnownCameraTriangulation() {
		// Build a projection matrix (a simple one)
		// And convert points from [x, y, z, w] to [x, y, w]
		double error = 0;
		for(int i=0; i < pts.getRows(); i++) {
			DoubleMatrix pOriginal = pts.getRow(i);
			DoubleMatrix p1 = pts1.getRow(i);
			DoubleMatrix p2 = pts2.getRow(i);
			DoubleMatrix pRecovered = TriangulationTools.triangulatePoint(camera1, camera2, p1, p2);
			error += pOriginal.sub(pRecovered).sum();
		}

		// Verify sanity
		org.junit.Assert.assertTrue(error < ERROR_THRESHOLD);
	}

	@Test
	public void testFundamentalMatrixRecovery() {
		// Make matches from points 1 and points 2.
		DoubleMatrix trueFundamental = DoubleMatrix.rand(3, 3);
		DoubleMatrix[] svd = Singular.fullSVD(trueFundamental);
		svd[1].put(2, 0); // Force rank 2.
		trueFundamental = svd[0].mmul(DoubleMatrix.diag(svd[1])).mmul(svd[2].transpose());
		trueFundamental.divi(trueFundamental.get(0, 0)); // Arbitrarily chose f00 to be 1.

		// Multiply out points to get 'correspondence'.
		pts2 = trueFundamental.mmul(pts1.transpose()).transpose(); // Still some camera distortion on pts1.
		PointTools.deaugment3D(pts2);
		DoubleMatrix matches = DoubleMatrix.concatHorizontally(pts1.getColumns(new IntervalRange(0, 2)), pts2.getColumns(new IntervalRange(0, 2)));
		DoubleMatrix recoveredFundamental = TriangulationTools.getFundamentalMatrix(matches.getRows(new IntervalRange(0, 9)));
		recoveredFundamental.divi(recoveredFundamental.get(0,0));

		DoubleMatrix pointError = TriangulationTools.getFundamentalError(recoveredFundamental, pts1, pts2);
		//double error = trueFundamental.distance2(recoveredFundamental);
		double error = pointError.sum();

		System.out.println("Error: " + error);
		trueFundamental.print();
		recoveredFundamental.print();

		// Verify sanity
		//org.junit.Assert.assertTrue(error < ERROR_THRESHOLD);
	}
}
