package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

public class XNodeLong extends DecisionNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public XNodeLong(Point2d p) {
		this(p, null, null);
	}
	
	public XNodeLong(Point2d p, NodeLong left, NodeLong right) {
		super(left, right);
		this.p = p;
	}
	
	@Override
	public long locate(Point2d q) {
		NodeLong n = right;
		
		if((q.x < p.x) || ((q.x == p.x) && (q.y < p.y))){
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public long locate(Segment q) {
		NodeLong n = right;
		
		if((q.p.x < p.x) || ((q.p.x == p.x) && (q.p.y < p.y))){
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public String toString() {
		return p.toString();
	}

	Point2d p;
}
