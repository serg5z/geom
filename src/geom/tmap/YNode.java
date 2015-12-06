package geom.tmap;

import geom.Point2d;

public class YNode<T> extends DecisionNode<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public YNode (Segment s) {
		this.s = s;
	}
	
	YNode () {
	}
	
	@Override
	public T locate(Point2d q) {
		Node<T> n = right;
		
		if(s.below(q)) {
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public Node<T> step(Point2d q) {
		Node<T> n = right;
		
		if(s.below(q)) {
			n = left;
		}
		
		return n;		
	}
	
	@Override
	public String toString() {
		return s.toString();
	}

	Segment s;
}
