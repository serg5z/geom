package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

public class TNode implements Node {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TNode(long id) {
		this.id = id;
	}
	
	@Override
	public long locate(Point2d q) {
		return id;
	}
	
	@Override
	public long locate(Segment q) {
		return id;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof TNode) {
			TNode t = (TNode)obj;

			result = t.id == id;
		}
		
		return result;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	long id;
}
