import com.josephcatrambone.sharpcloud.CameraMatrix;
import com.josephcatrambone.sharpcloud.PointTools;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by josephcatrambone on 9/28/15.
 */
public class CameraTest {
	final double ERROR_THRESHOLD = 0.1;
	final int NUM_POINTS = 100;
	java.util.Random random; // Avoid name conflict with rand mat.
	DoubleMatrix pts; // Randomly generated points nx3

	@Before
	public void setup() {
		random = new java.util.Random();

		// Random points -- perhaps bad for a deterministic test.
		pts = DoubleMatrix.rand(NUM_POINTS, 3);
	}

	@Test
	public void testIdentity() {
		CameraMatrix camera = new CameraMatrix();
		DoubleMatrix pts4d = PointTools.augment(pts);
		DoubleMatrix projected = camera.projectPoints(pts4d);
		org.junit.Assert.assertTrue(pts.distance2(projected) < ERROR_THRESHOLD);
	}

	@Test
	public void testTranslate() {
		double dx = random.nextDouble()*10;
		CameraMatrix camera = new CameraMatrix();
		camera.translationX = dx;
		final DoubleMatrix pts4d = PointTools.augment(pts);
		final DoubleMatrix projected = camera.projectPoints(pts4d);
		final DoubleMatrix delta = projected.sub(pts);
		final DoubleMatrix columnSums = delta.columnSums();
		org.junit.Assert.assertEquals(columnSums.get(0), dx * pts.getRows(), ERROR_THRESHOLD);
		org.junit.Assert.assertEquals(columnSums.get(1), 0, ERROR_THRESHOLD);
		org.junit.Assert.assertEquals(columnSums.get(2), 0, ERROR_THRESHOLD);
	}
}
