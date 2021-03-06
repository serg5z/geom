package geom.tmap;

import geom.Point2d;

public class TNode<T> implements Node<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TNode(T id) {
		this.id = id;
	}
	
	@Override
	public T locate(Point2d q) {
		return id;
	}
	
	@Override
	public T locate(Segment q) {
		return id;
	}
	
	@Override
	public String toString() {
		return id == null ? "null" : id.toString();
	}

	public T id;
}
