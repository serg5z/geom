package geom.tmap.compact;

import java.io.Serializable;

import geom.Point2d;

/*
 * Segemnt encoded by index of its smaller point in IndexLong
 */
public class Segment_ implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Segment_(int p) {
		this.p = p;
	}

	Segment_() {
	}

	// y = ax+b
	// p.y = a*p.x+b
	// q.y = a*q.x+b
	// a = (q.y-p.y)/(q.x-p.x)
	// b = p.y - p.x*(q.y-p.y)/(q.x-p.x) = (q.x*p.y - p.x*p.y - p.x*q.y +
	// p.x*p.y)/(q.x-p.x) = (q.x*p.y+p.x*q.y)/(q.x-p.x)
	// y = a*(x-p.x) + (p.y - p.x*a)

	public boolean above(Point2d p0, IndexLong c) {
		return ccw(p0, c) < 0;
	}

	public boolean below(Point2d p0, IndexLong c) {
		return ccw(p0, c) > 0;
	}

	public boolean above(Segment_ s, IndexLong c) {
		boolean result;
		if (p == s.p) {
			result = ccw(s.p+1, c) < 0;
		} else {
			result = ccw(s.p, c) < 0;
		}

		return result;
	}

	public boolean below(Segment_ s, IndexLong c) {
		boolean result;
		if (p == s.p) {
			result = ccw(s.p+1, c) > 0;
		} else {
			result = ccw(s.p, c) > 0;
		}

		return result;
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
		return n;
	}

	public int ccw(final int p3, IndexLong c) {
		int n = 0;
		final double dx21 = c.x[p+1] - c.x[p];
		final double dy21 = c.y[p+1] - c.y[p];
		final double dx31 = c.x[p3] - c.x[p];
		final double dy31 = c.y[p3] - c.y[p];
		if (dx21 * dy31 > dy21 * dx31) {
			n = 1;
		} else if (dx21 * dy31 < dy21 * dx31) {
			n = -1;
		} else {
			if (dx21 * dx31 < 0 || dy21 * dy31 < 0) {
				n = -1;
			} else if (dx21 * dx21 + dy21 * dy21 >= dx31 * dx31 + dy31 * dy31) {
				n = 0;
			} else {
				n = 1;
			}
		}
		return n;
	}

	@Override
	public int hashCode() {
		return p;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof Segment_) {
			Segment_ s = (Segment_) obj;

			result = p == s.p;
		}
		return result;
	}

	@Override
	public String toString() {
		return "{" + p + " -> " + (p+1) + "}";
	}

	public int p;
}