import com.josephcatrambone.sharpcloud.CameraMatrix;
import com.josephcatrambone.sharpcloud.PointTools;
import com.josephcatrambone.sharpcloud.TriangulationTools;
import org.jblas.DoubleMatrix;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Jo on 9/16/2015.
 */
public class TriangulationTest {
	final double RANGE = 10.0;
	final double ERROR_THRESHOLD = 0.1;
	final int NUM_POINTS = 100;
	final int[] xyCols = new int[]{0, 1};
	DoubleMatrix pts; // Randomly generated points nx3
	DoubleMatrix ptsAug; // Augmented nx4 points.
	CameraMatrix camera1; // Randomly rotated camera.
	CameraMatrix camera2; // Offset from camrea 1.
	DoubleMatrix pts1; // Points projected by camera 1.  nx3
	DoubleMatrix pts2;
	DoubleMatrix matches;

	@Before
	public void setup() {
		java.util.Random random = new java.util.Random(); // Avoid name conflict with rand mat.

		// Random points -- perhaps bad for a deterministic test.
		pts = DoubleMatrix.rand(NUM_POINTS, 3);

		// Drift for camera 2.
		double f = 1.0; //random.nextDouble()+0.5; // Focal distance.
		double theta = random.nextDouble()*RANGE*Math.PI/2; // Rotation.
		double dx = random.nextDouble()*RANGE;
		double dy = random.nextDouble()*RANGE;
		double dz = random.nextDouble()*RANGE;

		// 3D rotation matrix about z
		// ct -st 0
		// st ct 0
		// 0 0 1

		// Known projection matrices.
		// Intrinsic camera params...
		// 2D translation * 2D scaling * 2D shear * ...
		// Extrinsic params
		// 3D translation * 3D rotation
		camera1 = new CameraMatrix();
		camera2 = new CameraMatrix(f, dx, dy, dz, 0, theta, 0);

		// Augment the points
		ptsAug = PointTools.augment(pts);

		// Transform the points with the proj to get the new poitns.
		//DoubleMatrix pts2Aug = ptsAug.mmul(affine); does not work
		// pts -> num_pts x 4
		// camera -> 3x4
		// (c*pT)T -> (3x4(nx4)T)T -> 3xnT -> nx3
		pts1 = camera1.projectPoints(ptsAug);
		pts2 = camera2.projectPoints(ptsAug);

		PointTools.deaugment3D(pts1);
		PointTools.deaugment3D(pts2);

		matches = DoubleMatrix.concatHorizontally(pts1.getColumns(xyCols), pts2.getColumns(xyCols));
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
			DoubleMatrix pRecovered = TriangulationTools.triangulatePoint(camera1.getCombinedMatrix(), camera2.getCombinedMatrix(), p1, p2);
			error += pOriginal.sub(pRecovered).sum();
		}

		// Verify sanity
		org.junit.Assert.assertTrue(error < ERROR_THRESHOLD);
	}

	@Test
	public void testFundamentalMatrixRecovery() {
		double error;
		DoubleMatrix fundamental;

		// Try calculating with pts1 -> pts2
		fundamental = TriangulationTools.getFundamentalMatrix(matches);
		error = TriangulationTools.getFundamentalError(fundamental, pts1, pts2, 0, null);

		org.junit.Assert.assertTrue(error < ERROR_THRESHOLD);
	}

	@Test
	public void testEpipoleRecovery() {
		final DoubleMatrix fundamental = TriangulationTools.getFundamentalMatrix(matches);
		DoubleMatrix leftEpipole = TriangulationTools.getEpipole(fundamental.transpose());
		DoubleMatrix rightEpipole = TriangulationTools.getEpipole(fundamental);
		DoubleMatrix leftEpipoleNullSpaceProduct = leftEpipole.mmul(fundamental); // Should be zero.
		DoubleMatrix rightEpipoleNullSpaceProduct = rightEpipole.mmul(fundamental.transpose());

		org.junit.Assert.assertTrue(leftEpipoleNullSpaceProduct.sum() < ERROR_THRESHOLD);
		org.junit.Assert.assertTrue(rightEpipoleNullSpaceProduct.sum() < ERROR_THRESHOLD);
	}

	@Test
	public void testCameraMatrixRecoveryFromFundamental() {
		double error;
		final DoubleMatrix cameraMatrix = camera2.getCombinedMatrix();
		final DoubleMatrix fundamental = TriangulationTools.getFundamentalMatrix(matches);
		final DoubleMatrix recoveredCamera = TriangulationTools.cameraMatrixFromFundamentalMatrix(fundamental);
		error = recoveredCamera.distance1(cameraMatrix);

		// leftEpipole.mmul(fundamental) = 0
		// If e is the left epipole, eTF = 0
		// Left epipole -> eTF=0
		//DoubleMatrix.concatHorizontally(PointTools.skew(leftEpipole.get(0), leftEpipole.get(1), leftEpipole.get(2)).mmul(fundamental), leftEpipole.transpose()).distance2(camera2.getCombinedMatrix())

		//DoubleMatrix reprojected = recoveredCamera.mmul(ptsAug.transpose()).transpose();
		//PointTools.deaugment3D(reprojected);
		//System.out.println(pts2.distance2(reprojected));

		org.junit.Assert.assertTrue(error < ERROR_THRESHOLD*RANGE*100);
	}

	@Test
	public void testCameraMatrixRecoveryFromRansac() {
		double error;
		final DoubleMatrix cameraMatrix = camera2.getCombinedMatrix();
		final DoubleMatrix fundamental = TriangulationTools.RANSACFundamentalMatrix(matches, 8, ERROR_THRESHOLD, 100000);
		final DoubleMatrix recoveredCamera = TriangulationTools.cameraMatrixFromFundamentalMatrix(fundamental);
		error = recoveredCamera.distance1(cameraMatrix);

		org.junit.Assert.assertTrue(error < ERROR_THRESHOLD*RANGE*10);
	}
}
