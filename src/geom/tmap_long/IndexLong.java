package geom.tmap_long;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import geom.Point2d;
import geom.tmap.Segment;

public class IndexLong {
	public IndexLong(List<Segment> S) {
		int N = S.size();
		Map<Point2d, Integer> point_index = new HashMap<Point2d, Integer>((int)(1.3*N));
		x = new double[2*N];
		y = new double[2*N];
		p = new int[2*N];
		q = new int[2*N];
		id = new long[6*N+6];
	}
	
	public long locate(Point2d p) {
		return root.locate(p);
	}
	
	double x[];
	double y[];
	int p[];
	int q[];
	long id[];
	NodeLong root;
}
