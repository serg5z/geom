package geom.tmap;

import java.io.ObjectStreamException;

import geom.Point2d;

public class DynamicSegment<T> extends Segment {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public DynamicSegment(Point2d p, Point2d q) {
		this(p, q, null, null);
	}
	
	public DynamicSegment(Point2d p, Point2d q, T left_id, T right_id) {
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
		int lh = (left_id == null) ? 0 : left_id.hashCode();
		int rh = (right_id == null) ? 0 : right_id.hashCode();
		return p.hashCode()+q.hashCode()+lh+rh;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		
		if(obj instanceof DynamicSegment) {
			@SuppressWarnings("unchecked")
			DynamicSegment<T> s = (DynamicSegment<T>)obj;
			boolean leq = (left_id == null) ? (s.left_id == null) : left_id.equals(s.left_id);
			boolean req = (right_id == null) ? (s.right_id == null) : right_id.equals(s.right_id);
			
			result = leq && req && p.equals(s.p) && q.equals(s.q);
		}
		return result;
	}

	public double slope() {
		return (q.y-p.y)/(q.x-p.x);
	}
	
	Object writeReplace() throws ObjectStreamException {
		return new Segment(p, q);
	}

	@Override
	public String toString() {
		return "{"+p+" -> "+q+" "+left_id+", "+right_id+"}";
	}

	public T left_id;
	public T right_id;
}