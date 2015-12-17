package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

public class YNodeLong extends DecisionNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public YNodeLong (Segment s) {
		this(s, null, null);
	}
	
	public YNodeLong (Segment s, NodeLong left, NodeLong right) {
		super(left, right);
		this.s = s;
	}
		
	@Override
	public long locate(Point2d q) {
		NodeLong n = right;
		
		if(s.below(q)) {
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public long locate(Segment q) {
		NodeLong n = right;
		
		if(s.below(q)) {
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public String toString() {
		return s.toString();
	}

	Segment s;
}
