package geom.tmap;

import geom.Point2d;

public class XNode<T> extends DecisionNode<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public XNode(Point2d p) {
		this.p = p;
	}
	
	@Override
	public T locate(Point2d q) {
		Node<T> n = right;
		
		if((q.x < p.x) || ((q.x == p.x) && (q.y < p.y))){
			n = left;
		}
		
		return n.locate(q);
	}
	
	@Override
	public Node<T> step(Point2d q) {
		Node<T> n = right;
		
		if((q.x < p.x) || ((q.x == p.x) && (q.y < p.y))){
			n = left;
		}
		
		return n;
	}
	
	@Override
	public String toString() {
		return p.toString();
	}

	Point2d p;
}
