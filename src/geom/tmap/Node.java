package geom.tmap;

import java.io.Serializable;

import geom.Point2d;

interface Node<T> extends Serializable {
	T locate(Point2d q);

	Node<T> step(Point2d q);
}