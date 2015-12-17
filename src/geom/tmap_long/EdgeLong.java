package geom.tmap_long;

import geom.Point2d;

public class EdgeLong {
	public EdgeLong(Point2d p, Point2d q) {
		this(p, q, -1, -1);
	}
	
	public EdgeLong(Point2d p, Point2d q, long left, long right) {
		this.p = p;
		this.q = q;
		this.left = left;
		this.right = right;
	}

	@Override
	public int hashCode() {
		return p.hashCode() + q.hashCode() + Long.hashCode(left) + Long.hashCode(right);
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof EdgeLong) {
			@SuppressWarnings("rawtypes")
			EdgeLong s = (EdgeLong)obj;

			result = p.equals(s.p) && q.equals(s.q) && (left == s.left) && (right == s.right);
		}
		
		return result;
	}

	@Override
	public String toString() {
		return "{" + p + " -> " + q + " left: " + left + "; right: " + right +"}";
	}

	public Point2d p;
	public Point2d q;
	public long left;
	public long right;
}