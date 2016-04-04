package geom.tmap_long;

import java.io.Serializable;

import geom.Point2d;
import geom.tmap.Segment;

public interface Node extends Serializable {
	long locate(Point2d q);
	long locate(Segment q);
}