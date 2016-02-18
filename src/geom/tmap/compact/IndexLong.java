package geom.tmap.compact;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

import geom.Point2d;
import geom.tmap.DynamicNode;
import geom.tmap.Trapezoid;
import geom.tmap.XNode;
import geom.tmap.YNode;

public class IndexLong implements Index, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static class Context {
		Trapezoid<Long> above;
		Trapezoid<Long> below;		
	}

	private static class Singular implements Index, Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Singular(long id) { this.id = id;}
		
		@Override
		public long locate(Point2d q) { return id; }
		
		private long id;
	}
	
	public static Index EMPTY = new Singular(0);
	
	public IndexLong() {
		nodes = new LinkedList<NodeLong>();
		nodes.add(new TNodeLong(-1)); // the root element
	}
	
	public long locate(Point2d q) {
		return node[0].locate(q, this);
	}
	
	public int addXNode(int p) {
//		int result = next_node;
//		node[next_node++] = new XNodeLong(p);
//		return result;
		nodes.add(new XNodeLong(p));
		return nodes.size()-1;
	}
	
	public int addYNode(int p) {
//		int result = next_node;
//		node[next_node++] = new YNodeLong(p);
//		return result;
		nodes.add(new YNodeLong(p));
		return nodes.size()-1;
	}
	
	public int addTNode(long id) {
//		int result = next_node;
//		node[next_node++] = new TNodeLong(id);
//		return result;
		nodes.add(new TNodeLong(id));
		return nodes.size()-1;
	}
	
	public void add(Point2d p1, Point2d p2, long right_id) {
		if(edges == null) {
			edges = new EdgeRegistryLong(10000);
		}
		edges.add(p1, p2, right_id);
	}
	
	static int compare(Point2d p1, Point2d p2) {
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
	
	static Point2d min(Point2d p, Point2d q) {
		Point2d result = p;
		if(compare(p, q) > 0) {
			result = q;
		}
		return result;
	}
	
	static Point2d max(Point2d p, Point2d q) {
		Point2d result = p;
		if(compare(p, q) < 0) {
			result = q;
		}
		return result;
	}

	private void split(Trapezoid<Long> t, EdgeLong e, Context c) {
		Point2d l = t.left;
		Point2d r = t.right;

		Trapezoid<Long> A;
		Trapezoid<Long> B;
		DynamicNode<Long> nA;
		DynamicNode<Long> nB;
		
		if((c.above != null) && t.top.equals(c.above.top)) {
			A = c.above;
			nA = A.node;
			A.updateRight(t.upper_right, null);
			A.right = t.right;
		} else {
			A = new Trapezoid<Long>(l, r, t.top, e);
			nA = new DynamicNode<Long>(A);
			A.id = e.left_id;
			A.update(t.upper_left, c.above, t.upper_right, null);
			c.above = A;
		}
		
		if((c.below != null) && t.bottom.equals(c.below.bottom)) {
			B = c.below;
			nB = B.node;
			B.updateRight(null, t.lower_right);
			B.right = t.right;
		} else {
			B = new Trapezoid<Long>(l, r, e, t.bottom);
			nB = new DynamicNode<Long>(B);
			B.id = e.right_id;
			B.update(c.below, t.lower_left, null, t.lower_right);
			c.below = B;
		}
		
		t.node.nn = new YNode<Trapezoid<Long>>(e, nA, nB);
	}

	private void split4(Trapezoid<Long> t, EdgeLong e) {
		Trapezoid<Long> A = new Trapezoid<Long>(t.left, e.p, t.top, t.bottom);
		Trapezoid<Long> B = new Trapezoid<Long>(e.q, t.right, t.top, t.bottom);
		Trapezoid<Long> C = new Trapezoid<Long>(e.p, e.q, t.top, e);
		Trapezoid<Long> D = new Trapezoid<Long>(e.p, e.q, e, t.bottom);
		
		A.id = t.id;
		B.id = t.id;
		C.id = e.left_id;
		D.id = e.right_id;
		
		A.updateLeft(t.upper_left, t.lower_left);
		C.update(A, null, B, null);
		D.update(null, A, null, B);
		B.updateRight(t.upper_right, t.lower_right);
				
		t.node.nn = new XNode<Trapezoid<Long>>(
				e.p,
				new DynamicNode<Long>(A),
				new DynamicNode<Long>(
						e.q, 
						new DynamicNode<Long>(
								e, 
								new DynamicNode<Long>(C), 
								new DynamicNode<Long>(D)), 
						new DynamicNode<Long>(B)));
	}

	private void splitP(Trapezoid<Long> t, EdgeLong e, Context c) {
		Point2d r = min(e.q, t.right);
		
		Trapezoid<Long> A = new Trapezoid<Long>(t.left, e.p, t.top, t.bottom);
		Trapezoid<Long> B = new Trapezoid<Long>(e.p, r, t.top, e);
		Trapezoid<Long> C = new Trapezoid<Long>(e.p, r, e, t.bottom);
		
		A.id = t.id;
		B.id = e.left_id;
		C.id = e.right_id;
		
		A.updateLeft(t.upper_left, t.lower_left);
		B.update(A, null, t.upper_right, null);
		C.update(null, A, null, t.lower_right);
		
		t.node.nn = new XNode<Trapezoid<Long>>(
				e.p, 
				new DynamicNode<Long>(A), 
				new DynamicNode<Long>(
						e, 
						new DynamicNode<Long>(B), 
						new DynamicNode<Long>(C)));
		
		c.above = B;
		c.below = C;
	}

	private void splitQ(Trapezoid<Long> t, EdgeLong e, Context c) {
		Point2d l = max(e.p, t.left);
		
		Trapezoid<Long> A = new Trapezoid<Long>(e.q, t.right, t.top, t.bottom);
		A.id = t.id;
		Trapezoid<Long> B;
		Trapezoid<Long> C;
		DynamicNode<Long> nB;
		DynamicNode<Long> nC;
		
		if((c.above != null) && t.top.equals(c.above.top)) {
			B = c.above;
			nB = B.node;
			B.right = e.q;
		} else {
			B = new Trapezoid<Long>(l, e.q, t.top, e);
			nB = new DynamicNode<Long>(B);
			B.id = e.left_id;
			B.updateLeft(t.upper_left, c.above);
		}
		
		if((c.below != null) && t.bottom.equals(c.below.bottom)) {
			C = c.below;
			nC = C.node;
			C.right = e.q;
		} else {
			C = new Trapezoid<Long>(l, e.q, e, t.bottom);
			nC = new DynamicNode<Long>(C);
			C.id = e.right_id;
			C.updateLeft(c.below, t.lower_left);
		}
		
		t.node.nn = new XNode<Trapezoid<Long>>(
				e.q,
				new DynamicNode<Long>(e, nB, nC),
				new DynamicNode<Long>(A));
		
		A.update(B, C, t.upper_right, t.lower_right);
	}

	private void add(EdgeLong e) {
		Context c = new Context();
		
		Trapezoid<Long> d0 = root.locate(e);
		
		if(compare(e.q, d0.right) == 0) {
			if(compare(e.p, d0.left) == 0) {
				split(d0, e, c);
			} else {
				splitP(d0, e, c);
			}
		} else if(compare(e.q, d0.right) < 0) {
			if(compare(d0.left, e.p) == 0) {
				splitQ(d0, e, c);					
			} else {
				split4(d0, e);
			}				
		} else {
			if(compare(d0.left, e.p) == 0) {
				split(d0, e, c);					
			} else {
				splitP(d0, e, c);
			}				
			
			Trapezoid<Long> t = d0;

			if(e.below(d0.right)) {
				t = d0.lower_right;
			} else {
				t = d0.upper_right;
			}
			
			while(compare(e.q, t.right) > 0) {
				split(t, e, c);
				
				if(e.below(t.right)) {
					t = t.lower_right;
				} else {
					t = t.upper_right;
				}
			}
			if(compare(e.q, t.right) == 0) {
				split(t, e, c);
			} else {
				splitQ(t, e, c);
			}
		}
	}
	
	private static void shuffle(int[] ar) {
		Random rnd = new Random();
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	public void close() throws FileNotFoundException {
		edges.close();
		for(Point2d p : edges.point) {
			if(xmin > p.x) {
				xmin = p.x;
			}
			if(ymin > p.y) {
				ymin = p.y;
			}
			if(xmax < p.x) {
				xmax = p.x;
			}
			if(ymax < p.y) {
				ymax = p.y;
			}
		}
		
		Trapezoid<Long> D0 = new Trapezoid<Long>(0L, new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new EdgeLong(new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new EdgeLong(new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
		root = new DynamicNode<>(D0);
		D0.node = root;
		
		int N = edges.edge_index.values().size();
		int[] edge_list = new int[N];
		int i = 0;
		for(int e : edges.edge_index.values()) {
			edge_list[i++] = e;
		}

		shuffle(edge_list);
		
		PrintStream out = new PrintStream("edges.txt");
		for(int k : edge_list) {
			Point2d p1 = edges.point.get(k);
			Point2d p2 = edges.point.get(k+1);
			long l = edges.getLeft(k);
			long r = edges.getRight(k);
			out.println("LINESTRING ("+p1.x+" "+p1.y+", "+p2.x+" "+p2.y+")"
					+ "|"+k
					+ "|"+l
					+ "|"+r);
		}
		out.close();
		
		for(int k : edge_list) {
			Point2d p1 = edges.point.get(k);
			Point2d p2 = edges.point.get(k+1);
			long l = edges.getLeft(k);
			long r = edges.getRight(k);
			add(new EdgeLong(p1, p2, l, r));
		}

		DynamicNode.point_index = edges.point_index;
		DynamicNode.edge_index = edges.edge_index;
		DynamicNode.index = this;
		root.simplify();
		root = null;
		DynamicNode.point_index = null;
		DynamicNode.edge_index = null;
		edges.edge_index.clear();
		edges.edge_index = null;
		edges.point_index.clear();
		edges.point_index = null;
		
		x = new double[edges.point.size()];
		y = new double[edges.point.size()];
		for(int k = 0; k < edges.point.size(); k++) {
			Point2d p = edges.point.get(k);
			x[k] = p.x;
			y[k] = p.y;
		}
		edges.point = null;
		edges = null;
//		node = Arrays.copyOf(node, next_node);
		node = nodes.toArray(new NodeLong[] {});
		node[0] = node[node.length-1];
		nodes.clear();
		nodes = null;
	}
	
	public double x[];
	public double y[];
	public NodeLong node[];
	transient int next_node;
	transient public LinkedList<NodeLong> nodes;
	transient EdgeRegistryLong edges;
	transient DynamicNode<Long> root;
	transient double xmin = Double.POSITIVE_INFINITY;
	transient double xmax = Double.NEGATIVE_INFINITY;
	transient double ymin = Double.POSITIVE_INFINITY;
	transient double ymax = Double.NEGATIVE_INFINITY;
}
