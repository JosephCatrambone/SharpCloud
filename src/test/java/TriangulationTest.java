import com.josephcatrambone.sharpcloud.HomographyTools;
import com.josephcatrambone.sharpcloud.TriangulationTools;
import org.jblas.DoubleMatrix;
import org.jblas.util.Random;
import org.jblas.ranges.IntervalRange;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Jo on 9/16/2015.
 */
public class TriangulationTest {
	final double ERROR_THRESHOLD = 0.1;
	final int NUM_POINTS = 100;

	private DoubleMatrix makeCamera(double focalDistance, double x, double y, double z, double rot) {
		double ct = Math.cos(rot);
		double st = Math.sin(rot);
		return new DoubleMatrix(new double[]{
				ct/focalDistance, -st, 0, x,
				st, ct/focalDistance, 0, y,
				0, 0, 1, z
		}).reshape(3, 4);
	}

	public DoubleMatrix augment(DoubleMatrix mat) {
		return DoubleMatrix.concatHorizontally(mat, DoubleMatrix.ones(mat.getRows(), 1));
	}

	public void normalize3D(DoubleMatrix mat) {
		for(int i=0; i < mat.getRows(); i++) {
			double w = mat.get(i, 2);
			mat.put(i, 0, mat.get(i, 0)/w);
			mat.put(i, 1, mat.get(i, 1)/w);
			mat.put(i, 2, mat.get(i, 2)/w);
		}
	}

	public void normalize4D(DoubleMatrix mat) {
		for(int i=0; i < mat.getRows(); i++) {
			double w = mat.get(i, 3);
			mat.put(i, 0, mat.get(i, 0)/w);
			mat.put(i, 1, mat.get(i, 1)/w);
			mat.put(i, 2, mat.get(i, 2)/w);
			mat.put(i, 3, mat.get(i, 3)/w);
		}
	}

	@Before
	public void setup() {

	}

	@Test
	public void testPointRecovery() {
		java.util.Random random = new java.util.Random(); // Avoid name conflict with rand mat.

		// Random points -- perhaps bad for a deterministic test.
		DoubleMatrix pts = DoubleMatrix.rand(NUM_POINTS, 3);

		// Drift for camera 2.
		double f = random.nextDouble()+0.01; // Focal distance.
		double theta = random.nextDouble()*Math.PI/2; // Rotation.
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
		DoubleMatrix camera1 = makeCamera(f, 0, 0, 0, 0);
		DoubleMatrix camera2 = makeCamera(f, dx, dy, dz, theta);

		// This camera is offset 1 unit to the right, has the same focal distance, and is rotated slightl CCW on Z-up.

		// Augment the poitns
		DoubleMatrix ptsAug = augment(pts);

		// Transform the points with the proj to get the new poitns.
		//DoubleMatrix pts2Aug = ptsAug.mmul(affine); does not work
		// pts -> num_pts x 4
		// camera -> 3x4
		// (c*pT)T -> (3x4(nx4)T)T -> 3xnT -> nx3
		DoubleMatrix pts1 = camera1.mmul(ptsAug.transpose()).transpose();
		DoubleMatrix pts2 = camera2.mmul(ptsAug.transpose()).transpose();

		normalize3D(pts1);
		normalize3D(pts2);

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
}
