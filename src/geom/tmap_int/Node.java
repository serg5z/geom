package geom.tmap_int;

import java.io.Serializable;

import geom.Point2d;
import geom.tmap.Segment;

public interface Node extends Serializable {
	int locate(Point2d q);
}