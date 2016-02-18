package geom.tmap.compact;

import geom.Point2d;
import geom.tmap.Segment;

public class EdgeLong extends Segment {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public EdgeLong(Point2d p, Point2d q) {
		this(p, q, 0, 0);
	}
	
	public EdgeLong(Point2d p, Point2d q, long left_id, long right_id) {
		super(p, q);
		if(p.x < q.x) {
			this.left_id = left_id;
			this.right_id = right_id;
		} else if(p.x > q.x) {
			this.left_id = right_id;
			this.right_id = left_id;
		} else {
			if(p.y < q.y) {
				this.left_id = left_id;
				this.right_id = right_id;
			} else {
				this.left_id = right_id;
				this.right_id = left_id;
			}
		}
	}
	
	@Override
	public int hashCode() {
		return p.hashCode() + q.hashCode() + Long.hashCode(left_id) + Long.hashCode(right_id);
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		
		if(obj instanceof EdgeLong) {
			EdgeLong s = (EdgeLong)obj;
			
			result = left_id == s.left_id && right_id == s.right_id && p.equals(s.p) && q.equals(s.q);
		}
		return result;
	}

	public double slope() {
		return (q.y-p.y)/(q.x-p.x);
	}
	
	@Override
	public String toString() {
		return "{"+p+" -> "+q+" "+left_id+", "+right_id+"}";
	}

	public long left_id;
	public long right_id;
}