package geom.tmap_int;

import geom.Point2d;
import geom.tmap.Segment;

public class YNode extends DecisionNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public YNode (Segment s) {
		this(s, null, null);
	}
	
	public YNode (Segment s, Node left, Node right) {
		super(left, right);
		this.s = s;
	}
		
	@Override
	public int locate(Point2d q) {
		Node n = right;
		
		if(s.below(q)) {
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public int hashCode() {
		return s.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof YNode) {
			YNode y = (YNode)obj;

			result = s.equals(y.s) && left.equals(y.left) && right.equals(y.right);
		}
		
		return result;
	}

	@Override
	public String toString() {
		return s.toString();
	}

	Segment s;
}
