package geom.tmap.compact;

import geom.Point2d;
import geom.tmap.Node;

public interface NodeLong extends Node<Long> {
	long locate(Point2d q, IndexLong c);
}