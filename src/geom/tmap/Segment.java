package geom.tmap;

import java.io.Serializable;

import geom.Point2d;

class Segment implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Segment(Point2d p, Point2d q) {
		if (p.x < q.x) {
			this.p = p;
			this.q = q;
		} else if (p.x > q.x) {
			this.p = q;
			this.q = p;
		} else {
			if (p.y < q.y) {
				this.p = p;
				this.q = q;
			} else {
				this.p = q;
				this.q = p;
			}
		}
	}

	Segment() {
	}

	// y = ax+b
	// p.y = a*p.x+b
	// q.y = a*q.x+b
	// a = (q.y-p.y)/(q.x-p.x)
	// b = p.y - p.x*(q.y-p.y)/(q.x-p.x) = (q.x*p.y - p.x*p.y - p.x*q.y +
	// p.x*p.y)/(q.x-p.x) = (q.x*p.y+p.x*q.y)/(q.x-p.x)
	// y = a*(x-p.x) + (p.y - p.x*a)

	public boolean above(Point2d p0) {
		return ccw(p, q, p0) < 0;
	}

	public boolean below(Point2d p0) {
		return ccw(p, q, p0) > 0;
	}

	public boolean above(Segment s) {
		boolean result;
		if (p.equals(s.p)) {
			result = above(s.q);
		} else {
			result = above(s.p);
		}

		return result;
	}

	public boolean below(Segment s) {
		boolean result;
		if (p.equals(s.p)) {
			result = below(s.q);
		} else {
			result = below(s.p);
		}

		return result;
	}

	public static int ccw(final Point2d p1, final Point2d p2, final Point2d p3) {
		int n = 0;
		final double dx21 = p2.x - p1.x;
		final double dy21 = p2.y - p1.y;
		final double dx31 = p3.x - p1.x;
		final double dy31 = p3.y - p1.y;
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
		return p.hashCode() + q.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof Segment) {
			Segment s = (Segment) obj;

			result = p.equals(s.p) && q.equals(s.q);
		}
		return result;
	}

	@Override
	public String toString() {
		return "{" + p + " -> " + q + "}";
	}

	Point2d p;
	Point2d q;
}