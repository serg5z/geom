package geom.tmap;

import java.io.Serializable;

import geom.Point2d;

public interface Node<T> extends Serializable {
	T locate(Point2d q);
	T locate(Segment q);
}