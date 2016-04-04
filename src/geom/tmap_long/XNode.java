package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

public class XNode extends DecisionNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public XNode(Point2d p) {
		this(p, null, null);
	}
	
	public XNode(Point2d p, Node left, Node right) {
		super(left, right);
		this.p = p;
	}
	
	@Override
	public long locate(Point2d q) {
		Node n = right;
		
		if((q.x < p.x) || ((q.x == p.x) && (q.y < p.y))){
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public long locate(Segment q) {
		Node n = right;
		
		if((q.p.x < p.x) || ((q.p.x == p.x) && (q.p.y < p.y))){
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public int hashCode() {
		return p.hashCode()+left.hashCode()+right.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof XNode) {
			XNode x = (XNode)obj;

			result = p.equals(x.p) && left.equals(x.left) && right.equals(x.right);
		}
		
		return result;
	}

	@Override
	public String toString() {
		return p.toString();
	}

	public Point2d p;
}
