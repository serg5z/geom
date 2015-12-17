package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

class DynamicSegmentLong extends Segment {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public DynamicSegmentLong(Point2d p, Point2d q) {
			this(p, q, -1, -1);
		}
		
		public DynamicSegmentLong(Point2d p, Point2d q, long left_id, long right_id) {
			super(p, q);
			reverse = false;
			if(p.x < q.x) {
				this.left_id = left_id;
				this.right_id = right_id;
			} else if(p.x > q.x) {
				this.left_id = right_id;
				this.right_id = left_id;
				reverse = true;
			} else {
				if(p.y < q.y) {
					this.left_id = left_id;
					this.right_id = right_id;
				} else {
					this.left_id = right_id;
					this.right_id = left_id;
					reverse = true;
				}
			}
			s = new Segment(p, q);
		}
		
		@Override
		public int hashCode() {
			return p.hashCode()+q.hashCode()+Long.hashCode(left_id)+Long.hashCode(right_id);
		}

		@Override
		public boolean equals(Object obj) {
			boolean result = false;
			
			if(obj instanceof DynamicSegmentLong) {
				DynamicSegmentLong s = (DynamicSegmentLong)obj;
				
				result = (left_id == s.left_id) && (right_id == s.right_id) && p.equals(s.p) && q.equals(s.q);
			}
			return result;
		}

		public double slope() {
			return (q.y-p.y)/(q.x-p.x);
		}

		@Override
		public String toString() {
			String prefix = reverse ? "-" : "";
			return "{"+p+" -> "+q+" "+prefix+left_id+", "+prefix+right_id+"}";
		}

		boolean reverse;
		long left_id;
		long right_id;
		Trapezoid above;
		Trapezoid below;
		Segment s;
	}