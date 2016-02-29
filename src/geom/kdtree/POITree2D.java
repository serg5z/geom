package kdtree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import geom.Point2d;
import geom.Tuple2;

public class POITree2D {
	public POITree2D(Collection<POI> pois) {
		HashMap<Point2d, List<POI>> locations = new HashMap<Point2d, List<POI>>(pois.size());
		
		for(POI p : pois) {
			List<POI> l = locations.get(p.location());
			if(l == null) {
				l = new LinkedList<POI>();
				locations.put(p.location(), l);
			}
			l.add(p);
		}
		
		build(locations);
	}
	
	public POITree2D(Map<Point2d, List<POI>> locations) {
		build(locations);
	}
	
	private void build(Map<Point2d, List<POI>> locations) {
		int n = locations.size();
		@SuppressWarnings("unchecked")
		List<POI>[] pois_set = locations.values().toArray(new List[]{});
		Integer[] x_index = new Integer[n];
		Integer[] y_index = new Integer[n];
		
		for(int i = 0; i < n; i++) {
			x_index[i] = i;
			y_index[i] = i;
		}
		
		Arrays.sort(x_index, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return compareByX(pois_set[o1].get(0).location(), pois_set[o2].get(0).location());
			}
		});
		
		Arrays.sort(y_index, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return compareByY(pois_set[o1].get(0).location(), pois_set[o2].get(0).location());
			}
		});
		
		//TODO: determine Axis based on spread of location coordinates
		axis = Axis.X;
		root = add(pois_set, axis, x_index, y_index, 0, x_index.length-1);
		augment(root);
	}
	
	public POI nearest(Point2d q) {
//		return nearest(root, q, axis);
		PriorityQueue<Tuple2<POI, Double>> Q = new PriorityQueue<Tuple2<POI, Double>>(2, distance);
		nearest(root, q, axis, Q, 1);
		
		return Q.size() > 0 ? Q.remove().e1 : null;
	}
	
	public Collection<POI> nearestN(Point2d q, int N) {
		PriorityQueue<Tuple2<POI, Double>> Q = new PriorityQueue<Tuple2<POI, Double>>(N+1, distance);
		nearest(root, q, axis, Q, N);
		LinkedList<POI> result = new LinkedList<POI>();
		
		for(Tuple2<POI, Double> t : Q) {
			result.addFirst(t.e1);
		}
		
		return result;
	}
	
	protected void nearest(Node n, Point2d q, Axis axis, PriorityQueue<Tuple2<POI, Double>> items, int N) {		
		if((n != null) && (items.size() < N)) {
			Node next = n.left;
			Node other = n.right;
			Point2d p = n.pois[0].location();
			if(axis == Axis.X) {
//				System.out.print("compare "+q+" with "+n.pois.get(0).location()+" using x coordinate... ");
				if(compareByX(q, p) > 0) {
//					System.out.println(">");
					next = n.right;
					other = n.left;
//				} else {
//					System.out.println("<");
				}
			} else {
//				System.out.print("compare "+q+" with "+n.pois.get(0).location()+" using y coordinate... ");
				if(compareByY(q, p) > 0) {
//					System.out.println(">");
					next = n.right;
					other = n.left;
//				} else {
//					System.out.println("<");
				}
			}
			
			double d2 = distance2(p, q);			
			//Add current POIs if distance to current less than max distance to added pois  
			if((items.size() < N) || (d2 < items.peek().e2)) {
				for(POI poi : n.pois) {
					if(d2 <= poi.R*poi.R) {
						if(items.size() < N) {
							items.add(new Tuple2<POI, Double>(poi, d2));
						} else {
							break;
						}
					}
				}
//				System.out.println("\tafter "+n.pois.get(0).location()+"@"+n.maxR2+": "+items);
			}

			nearest(next, q, axis.other(), items, N);
//			System.out.println("\tafter recursive: "+items);

			if((other != null) && (items.size() < N)) {
				double d = 0.0;
				if(axis == Axis.X) {
					d = Math.abs(p.x - q.x); // + other.maxR;
				} else {
					d = Math.abs(p.y - q.y); // + other.maxR;
				}
				if((items.size() < N) && (d < other.maxR)) {
//					System.out.println("\t"+n.pois.get(0).location()+" other side is plausible");
					nearest(other, q, axis.other(), items, N);					
				}
			}
		}
	}
	
	private enum Axis {
		X, Y;
		
		public Axis other() {
			if(this == X) {
				return Y;
			} else {
				return X;
			}
		}
	}
	
	private static class Node {
		public Node(POI[] pois, Node left, Node right) {			
			this.pois = pois;
			this.R = 0;
			for(POI p : pois) {
				if(p.R > R) {
					R = p.R;
				}
			}
			this.left = left;
			this.right = right;
		}
		
		@Override
		public String toString() {
			return "(" + pois.toString() + ", " + R + "/" + maxR +
					", " + (left==null ? "None" : left.toString()) + 
					", " + (right==null ? "None" : right.toString()) + ")";
		}

		POI[] pois;
		double R; // max R2 of POIs in this location
		double maxR; // max R2 of this node and it's children
		Node left;
		Node right;
	}
	
	private double augment(Node n) {
		double result = 0.0;
		
		if(n != null) {
			n.maxR = Math.max(n.R, Math.max(augment(n.left), augment(n.right)));
			result = n.maxR;
		}
		
		return result;
	}
	
	private double distance2(Point2d p1, Point2d p2) {
		return ((p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y));
	}
	
	private static int compareByX(Point2d p1, Point2d p2) {
		int result = 0;
		if(p1.x < p2.x) {
			result = -1;
		} else if(p1.x > p2.x) {
			result = 1;
		} else {
			if(p1.y < p2.y) {
				result = -1;
			} else if (p1.y > p2.y) {
				result = 1;
			}
		}
		
		return result;
	}
	
	private static int compareByY(Point2d p1, Point2d p2) {
		int result = 0;
		if(p1.y < p2.y) {
			result = -1;
		} else if(p1.y > p2.y) {
			result = 1;
		} else {
			if(p1.x < p2.x) {
				result = -1;
			} else if (p1.x > p2.x) {
				result = 1;
			}
		}
		
		return result;
	}
	
	private Node add(List<POI>[] pois, Axis axis, Integer[] x_index, Integer[] y_index, int start, int end) {
		Node result = null;
		
//		System.out.print("start: "+start+"; end: "+end+"; points: ");
//		for(int i = start; i <= end; i++) {
//			System.out.print(pois[x_index[i]]);
//		}
//		System.out.println();
//		System.out.print("start: "+start+"; end: "+end+"; points: ");
//		for(int i = start; i <= end; i++) {
//			System.out.print(pois[y_index[i]]);
//		}
//		System.out.println();
		if(start == end) {
			if(axis == Axis.X) {
				result = new Node(pois[y_index[start]].toArray(new POI[]{}), null, null);
			} else {
				result = new Node(pois[x_index[start]].toArray(new POI[]{}), null, null);				
			}
		} else if (start < end) {
			int m = (start+end+1)/2;
			List<POI> split = null;
			int[] left = new int[end-start+1];
			int[] tmp = new int[end-start+1];
			Integer[] index;
			
			if(axis == Axis.X) {
				index = y_index;
				
				split = pois[x_index[m]];
				
				for(int i = start, j = 0; i <= end; i++, j++) {
					left[j] = compareByX(pois[y_index[i]].get(0).location(), split.get(0).location());//(points[y_index[i+start]].location().x <= split.location().x);
					tmp[j] = y_index[i];
				}
			} else {
				index = x_index;
				
				split = pois[y_index[m]]; 
				
				for(int i = start, j = 0; i <= end; i++, j++) {
					left[j] = compareByY(pois[x_index[i]].get(0).location(), split.get(0).location());//(points[y_index[i+start]].location().x <= split.location().x);
					tmp[j] = x_index[i];
				}
			}
			
			int j = start;
			for(int i = start, k = 0; i <= end; i++, k++) {
				if(left[k] < 0) {
					index[j] = tmp[k];
					j++;
				}
			}
			j++;
			for(int i = start, k = 0; i <= end; i++, k++) {
				if(left[k] > 0) {
					index[j] = tmp[k];
					j++;
				}
			}
			
			result = new Node(split.toArray(new POI[]{}), 
					add(pois, axis.other(), x_index, y_index, start, m-1), 
					add(pois, axis.other(), x_index, y_index, m+1, end));
//			if(m == end) {
//				result = new Node(split, 
//						add(pois, axis.other(), x_index, y_index, start, start), 
//						null);				
//			} else {
//				result = new Node(split, 
//						add(pois, axis.other(), x_index, y_index, start, m-1), 
//						add(pois, axis.other(), x_index, y_index, m+1, end));
//			}
		}
		
		return result;
	}
	
	public Node root;
	Axis axis;
	
	private static Comparator<? super Tuple2<?, Double>> distance = new Comparator<Tuple2<?, Double>>() {
		@Override
		public int compare(Tuple2<?, Double> o1, Tuple2<?, Double> o2) {
			return Double.compare(o2.e2, o1.e2); //reverse order
		}
	};
}
