package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Segment;

public class TNodeLong implements NodeLong {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TNodeLong(long id) {
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
	public String toString() {
		return String.valueOf(id);
	}

	long id;
}
