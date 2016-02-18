package geom.tmap_long;

import java.io.Serializable;

import geom.Point2d;
import geom.tmap.Segment;

public interface NodeLong extends Serializable {
	long locate(Point2d q);
	long locate(Segment q);
}