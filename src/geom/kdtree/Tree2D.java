package kdtree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import geom.Point2d;
import geom.Tuple2;

public class Tree2D<T extends Locatable> {
	public Tree2D(final T[] points) {
		Integer[] x_index = new Integer[points.length];
		Integer[] y_index = new Integer[points.length];
		
		for(int i = 0; i < points.length; i++) {
			x_index[i] = i;
			y_index[i] = i;
		}
		
		Arrays.sort(x_index, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return compareByX(points[o1].location(), points[o2].location());
			}
		});
		
		Arrays.sort(y_index, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return compareByY(points[o1].location(), points[o2].location());
			}
		});
		
		//TODO: determine Axis based on spread of location coordinates
		axis = Axis.X;
		root = add(points, axis, x_index, y_index, 0, x_index.length-1);
	}
	
	public T nearest(Point2d q) {
//		return nearest(root, q, axis);
		PriorityQueue<Tuple2<T, Double>> Q = new PriorityQueue<Tuple2<T, Double>>(2, distance);
		nearest(root, q, axis, Q, 1);
		
		return Q.remove().e1;
	}
	
	public Collection<T> nearestN(Point2d q, int N) {
		PriorityQueue<Tuple2<T, Double>> Q = new PriorityQueue<Tuple2<T, Double>>(N+1, distance);
		nearest(root, q, axis, Q, N);
		LinkedList<T> result = new LinkedList<T>();
		
		for(Tuple2<T, Double> t : Q) {
			result.addFirst(t.e1);
		}
		
		return result;
	}
	
	protected void nearest(Node<T> n, Point2d q, Axis axis, PriorityQueue<Tuple2<T, Double>> items, int N) {		
		if(n != null) {
			Node<T> next = n.left;
			Node<T> other = n.right;
			if(axis == Axis.X) {
				System.out.print("compare "+q+" with "+n.data.location()+" using x coordinate... ");
				if(compareByX(q, n.data.location()) > 0) {
					System.out.println(">");
					next = n.right;
					other = n.left;
				} else {
					System.out.println("<");
				}
			} else {
				System.out.print("compare "+q+" with "+n.data.location()+" using y coordinate... ");
				if(compareByY(q, n.data.location()) > 0) {
					System.out.println(">");
					next = n.right;
					other = n.left;
				} else {
					System.out.println("<");
				}
			}

			double d2 = distance2(n.data.location(), q);
			if(d2 < items.peek().e2) {
				items.add(new Tuple2<T, Double>(n.data, d2));
				if(items.size() > N) {
					items.remove();
				}
			}
			
			nearest(next, q, axis.other(), items, N);
			System.out.println("\t"+q);

			if(axis == Axis.X) {
				if((items.size() < N) || (items.peek().e2 > ((n.data.location().x - q.x)*(n.data.location().x - q.x)))) {
					System.out.println("\t"+n.data.location()+" other side is plausible");
					nearest(other, q, axis.other(), items, N);					
				}
			} else {
				if((items.size() < N) || (items.peek().e2 > ((n.data.location().y - q.y)*(n.data.location().y - q.y)))) {
					System.out.println("\t"+n.data.location()+" other side is plausible");
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
	
	private static class Node<T> {
		public Node(T data, Node<T> left, Node<T> right) {
			this.data = data;
			this.left = left;
			this.right = right;
		}
		
		@Override
		public String toString() {
			return "(" + data.toString() + 
					", " + (left==null ? "None" : left.toString()) + 
					", " + (right==null ? "None" : right.toString()) + ")";
		}

		T data;
		Node<T> left;
		Node<T> right;
	}
	
	private double distance2(Point2d p1, Point2d p2) {
		return ((p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y));
	}
	
	private int compareByX(Point2d p1, Point2d p2) {
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
	
	private int compareByY(Point2d p1, Point2d p2) {
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
	
	private Node<T> add(T[] points, Axis axis, Integer[] x_index, Integer[] y_index, int start, int end) {
		Node<T> result = null;
		
		System.out.print("start: "+start+"; end: "+end+"; points: ");
		for(int i = start; i <= end; i++) {
			System.out.print(points[x_index[i]]);
		}
		System.out.println();
		System.out.print("start: "+start+"; end: "+end+"; points: ");
		for(int i = start; i <= end; i++) {
			System.out.print(points[y_index[i]]);
		}
		System.out.println();
		if(start == end) {
			if(axis == Axis.X) {
				result = new Node<T>(points[y_index[start]], null, null);
			} else {
				result = new Node<T>(points[x_index[start]], null, null);				
			}
		} else if (start < end) {
			int m = (start+end+1)/2;
			T split = null;
			boolean[] left = new boolean[end-start+1];
			int[] tmp = new int[end-start+1];
			Integer[] index;
			
			if(axis == Axis.X) {
				index = y_index;
				
				split = points[x_index[m]];
				
				for(int i = 0; i < left.length; i++) {
					left[i] = (compareByX(points[y_index[i+start]].location(), split.location()) <= 0);//(points[y_index[i+start]].location().x <= split.location().x);
					tmp[i] = y_index[i+start];
				}
			} else {
				index = x_index;
				
				split = points[y_index[m]]; 
				
				for(int i = 0; i < left.length; i++) {
					left[i] = (compareByY(points[y_index[i+start]].location(), split.location()) <= 0);//(points[y_index[i+start]].location().y <= split.location().y);
					tmp[i] = x_index[i+start];
				}
			}
			
			for(int i = 0, j = m+1, k = start; i < left.length; i++) {
				if((i+start) != index[m]) {
					if(left[i]) {
						index[k++] = tmp[i];
					} else {
						index[j++] = tmp[i];
					}
				}
			}
			
			if(m == end) {
				result = new Node<T>(split, 
						add(points, axis.other(), x_index, y_index, start, start), 
						null);				
			} else {
				result = new Node<T>(split, 
						add(points, axis.other(), x_index, y_index, start, m-1), 
						add(points, axis.other(), x_index, y_index, m+1, end));
			}
		}
		
		return result;
	}
	
	Node<T> root;
	Axis axis;
	
	private static Comparator<? super Tuple2<?, Double>> distance = new Comparator<Tuple2<?, Double>>() {
		@Override
		public int compare(Tuple2<?, Double> o1, Tuple2<?, Double> o2) {
			return Double.compare(o2.e2, o1.e2); //reverse order
		}
	};
}
