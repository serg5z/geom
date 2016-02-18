package geom.tmap;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

import geom.Point2d;
import geom.Tuple2;

public class EdgeRegistry<T> {
	@SuppressWarnings("unchecked")
	public EdgeRegistry(int N) {
		point_index = new HashMap<Point2d, Integer>(200000);
		edge_index = new HashMap<Tuple2<Integer, Integer>, Integer>(200000);
		point = new Point2d[N];
		right = (T[])new Object[N];
		left = (T[])new Object[N];
		next_point = 0;
		next_edge = 0;
	}
	
	public void close() {
		point = Arrays.copyOf(point, next_point);
		right = Arrays.copyOf(right, next_point);
		left = Arrays.copyOf(left, next_point);
	}
	
	public T getRight(int i) {
		return right[i];
	}
	
	public T getLeft(int i) {
		return left[i];
	}
	
	public void add(Point2d p1, Point2d p2, T right_id) {
		/*
		 * find existing or create new half edge from p1->p2 with rightid = right
		 * 1. look for existing half edge in edge_index by p2
		 * 	  -- if found set it's leftid to right
		 * 2. (if existing edge is not found) check if previous point equals to p1
		 *    -- if prev_point == p1 create new edge pointing to prev_point and rightid = right
		 *    -- otherwise add p1 and p2 as new points create edge pointing to next and rightid = right 
		 */
		if(next_point == 0) {
			point_index.put(p1, 0);
			point_index.put(p2, 1);
			point[0] = p1;
			point[1] = p2;
			next_point = 2;
			right[next_edge] = right_id;
			edge_index.put(new Tuple2<Integer, Integer>(0,  1), next_edge);
			next_edge++;
		} else {
			Integer i1 = point_index.get(p1);
			Integer i2 = point_index.get(p2);
			
			Integer e = edge_index.get(new Tuple2<Integer, Integer>(i2,  i1));
		
			if((e != null) && (p1.equals(point[e+1]))) {
				left[e] = right_id;
			} else {
				int p = next_point - 1;
				
				if(p1.equals(point[p])) {
					if(i2 == null) {
						point_index.put(p2,  next_point);
						i2 = next_point;
					}
					point[next_point] = p2;
					next_point++;
				} else {
					p++;
					if(i1 == null) {
						point_index.put(p1,  next_point);
						i1 = next_point;
					}
					point[next_point] = p1;
					next_point++;
					if(i2 == null) {
						point_index.put(p2,  next_point);
						i2 = next_point;
					}
					point[next_point] = p2;
					next_point++;					
				}
				right[p] = right_id;
				edge_index.put(new Tuple2<Integer, Integer>(i1,  i2), p);
				next_edge++;
			}
		}
	}
	
	HashMap<Point2d, Integer> point_index;
	HashMap<Tuple2<Integer, Integer>, Integer> edge_index;
	Point2d[] point;
	T[] right;
	T[] left;
	int next_point;
	int next_edge;
}