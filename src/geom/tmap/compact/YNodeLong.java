package geom.tmap.compact;

import geom.Point2d;
import geom.tmap.Segment;

public class YNodeLong extends DecisionNodeLong {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public YNodeLong(int p) {
		this(p, -1, -1);
	}
	
	public YNodeLong(int p, int left, int right) {
		super(left, right);
		this.p = p;
	}
		
	@Override
	public long locate(Point2d q, IndexLong c) {
		NodeLong n = c.node[right];
		
		if(below(q, c)) {
			n = c.node[left];
		}
		
		return n.locate(q, c);
	}
	
	@Override
	public String toString() {
		return String.valueOf(p)+" -> "+String.valueOf(p+1);
	}

	public boolean above(Point2d p0, IndexLong c) {
		return ccw(p0, c) < 0;
	}

	public boolean below(Point2d p0, IndexLong c) {
		return ccw(p0, c) > 0;
	}

	public int ccw(final Point2d p3, IndexLong c) {
		int n = -1;
		final double dx21 = c.x[p+1] - c.x[p];
		final double dy21 = c.y[p+1] - c.y[p];
		final double dx31 = p3.x - c.x[p];
		final double dy31 = p3.y - c.y[p];
		
		if (dx21 * dy31 > dy21 * dx31) {
			n = 1;
		}
		
		return n;
	}
//	public static int ccw(double x1, double y1, double x2, double y2, double x3, double y3) {
//		int n = 0;
//		final double dx21 = x2 - x1;
//		final double dy21 = y2 - y1;
//		final double dx31 = x3 - x1;
//		final double dy31 = y3 - y1;
//		if (dx21 * dy31 > dy21 * dx31) {
//			n = 1;
//		} else if (dx21 * dy31 < dy21 * dx31) {
//			n = -1;
//		} else {
//			if (dx21 * dx31 < 0 || dy21 * dy31 < 0) {
//				n = -1;
//			} else if (dx21 * dx21 + dy21 * dy21 >= dx31 * dx31 + dy31 * dy31) {
//				n = 0;
//			} else {
//				n = 1;
//			}
//		}
//		return n;
//	}

	
	@Override
	public int hashCode() {
		return Integer.hashCode(p)+Integer.hashCode(left)+Integer.hashCode(right);
	}

	@Override
	public boolean equals(Object o) {
		boolean result = super.equals(o);

		if (o instanceof YNodeLong) {
 			YNodeLong yn = (YNodeLong)o;

			result = (p == yn.p) && (left == yn.left) && (right == yn.right);
		}
		
		return result;
	}

	int p;

	@Override
	public Long locate(Point2d q) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long locate(Segment q) {
		// TODO Auto-generated method stub
		return null;
	}
}
