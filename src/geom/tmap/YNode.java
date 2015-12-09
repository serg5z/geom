package geom.tmap;

import geom.Point2d;

public class YNode<T> extends DecisionNode<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public YNode (Segment s) {
		this(s, null, null);
	}
	
	public YNode (Segment s, Node<T> left, Node<T> right) {
		super(left, right);
		this.s = s;
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
	public T locate(Segment q) {
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
