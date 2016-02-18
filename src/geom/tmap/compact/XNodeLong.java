package geom.tmap.compact;

import geom.Point2d;
import geom.tmap.Segment;

public class XNodeLong extends DecisionNodeLong {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public XNodeLong(int p) {
		this(p, -1, -1);
	}
	
	public XNodeLong(int p, int left, int right) {
		super(left, right);
		this.p = p;
	}
	
	@Override
	public long locate(Point2d q, IndexLong c) {
		NodeLong n = c.node[right];
		double x = c.x[p];
		double y = c.x[p];

		if((q.x < x) || ((q.x == x) && (q.y < y))) {
			n = c.node[left];
		}
		
		return n.locate(q, c);
	}
	
	@Override
	public String toString() {
		return String.valueOf(p);
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
