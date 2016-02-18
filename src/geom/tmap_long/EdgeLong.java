package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

public class EdgeLong extends Segment {
	private static final long serialVersionUID = 1L;
	
	public EdgeLong(Point2d p, Point2d q) {
		this(p, q, -1, -1);
	}
	
	public EdgeLong(Point2d p, Point2d q, long left, long right) {
		super(p, q);
		if(p.x < q.x) {
			this.left = left;
			this.right = right;
		} else if(p.x > q.x) {
			this.left = right;
			this.right = left;
		} else {
			if(p.y < q.y) {
				this.left = left;
				this.right = right;
			} else {
				this.left = right;
				this.right = left;
			}
		}
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

	public long left;
	public long right;
}