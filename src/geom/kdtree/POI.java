package kdtree;

import geom.Point2d;

public class POI implements Locatable {
	public POI(long id, Point2d location, double r) {
		this.id = id;
		this.location = location;
		R = r;
	}
	
	@Override
	public Point2d location() {
		return location;
	}
	
	@Override
	public String toString() {
		return id+"@"+location.toString();
	}

	long id;
	private Point2d location;
	double R;
}
