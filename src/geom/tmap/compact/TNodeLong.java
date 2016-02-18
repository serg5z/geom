package geom.tmap.compact;

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
	public long locate(Point2d q, IndexLong c) {
		return id;
	}
	
	@Override
	public String toString() {
		return String.valueOf(id);
	}

	long id;

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
