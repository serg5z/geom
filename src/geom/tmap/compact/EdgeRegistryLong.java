package geom.tmap.compact;

import java.util.ArrayList;
import java.util.HashMap;

import geom.Point2d;
import geom.Tuple2;

public class EdgeRegistryLong {
	public EdgeRegistryLong(int N) {
		point_index = new HashMap<Point2d, Integer>(N);
		edge_index = new HashMap<Tuple2<Integer, Integer>, Integer>(N);
		point = new ArrayList<Point2d>(N);
		right = new ArrayList<Long>(N);
		left = new ArrayList<Long>(N);
	}
	
	public void close() {
//		point = Arrays.copyOf(point, next_point);
//		right = Arrays.copyOf(right, next_point);
//		left = Arrays.copyOf(left, next_point);
		point.trimToSize();
		right.trimToSize();
		left.trimToSize();
	}
	
	public long getRight(int i) {
		return right.get(i);
	}
	
	public long getLeft(int i) {
		return left.get(i);
	}
	
	public void add(Point2d p1, Point2d p2, long right_id) {
		/*
		 * find existing or create new half edge from p1->p2 with rightid = right
		 * 1. look for existing half edge in edge_index by p2
		 * 	  -- if found set it's leftid to right
		 * 2. (if existing edge is not found) check if previous point equals to p1
		 *    -- if prev_point == p1 create new edge pointing to prev_point and rightid = right
		 *    -- otherwise add p1 and p2 as new points create edge pointing to next and rightid = right 
		 */
		if(point.size() == 0) {
			point_index.put(p1, 0);
			point_index.put(p2, 1);
			point.add(p1);
			point.add(p2);
			edge_index.put(new Tuple2<Integer, Integer>(0,  1), right.size());
			right.add(right_id);
			left.add(-1L);
		} else {
			Integer i1 = point_index.get(p1);
			Integer i2 = point_index.get(p2);
			
			Integer e = edge_index.get(new Tuple2<Integer, Integer>(i2,  i1));
		
			if((e != null) && (p1.equals(point.get(e+1)))) {
				left.set(e, right_id);
			} else {
				if(p1.equals(point.get(point.size()-1))) {
					if(i2 == null) {
						i2 = point.size();
						point_index.put(p2,  i2);
					}
					point.add(p2);
				} else {
					if(i1 == null) {
						i1 = point.size();
						point_index.put(p1,  i1);
					}
					point.add(p1);
					if(i2 == null) {
						i2 = point.size();
						point_index.put(p2,  i2);
					}
					point.add(p2);
					right.add(-1L);
					left.add(-1L);
				}
				edge_index.put(new Tuple2<Integer, Integer>(i1,  i2), right.size());
				right.add(right_id);
				left.add(-1L);
			}
		}
	}
	
	HashMap<Point2d, Integer> point_index;
	HashMap<Tuple2<Integer, Integer>, Integer> edge_index;
	ArrayList<Point2d> point;
	ArrayList<Long> right;
	ArrayList<Long> left;
}