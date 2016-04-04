package geom.tmap_int;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import geom.Point2d;
import geom.Tuple2;
import geom.tmap.DynamicNode;
import geom.tmap.Segment;
import geom.tmap.Trapezoid;

public class IndexInt implements Index, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static class Context {
		Trapezoid<Integer> above;
		Trapezoid<Integer> below;		
	}

	public static class Singular implements Index, Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Singular(int id) { this.id = id;}
		
		@Override
		public int locate(Point2d q) { return id; }
		
		private int id;
	}
	
	public static Index EMPTY = new Singular(0);
	
	public IndexInt() {
		edges = new HashMap<Tuple2<Point2d, Point2d>, Edge>();
		points = new HashMap<Point2d, Point2d>();
	}
	
	public int locate(Point2d q) {
		return root.locate(q);
	}
	
	public void add(Point2d p1, Point2d p2, int right_id) {
		Point2d p = points.get(p1);
		if(p == null) {
			points.put(p1, p1);
		} else {
			p1 = p;
		}
		p = points.get(p2);
		if(p == null) {
			points.put(p2, p2);
		} else {
			p2 = p;
		}
		
		Edge e = edges.get(new Tuple2<Point2d, Point2d>(p2, p1));
		if(e == null) {
			e = new Edge(p1, p2, -1, right_id);
			edges.put(new Tuple2<Point2d, Point2d>(p1, p2), e);
		} else {
			e.left = right_id;
		}
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

	private void split(Trapezoid<Integer> t, Edge e, Context c) {
		Point2d l = t.left;
		Point2d r = t.right;

		Trapezoid<Integer> A;
		Trapezoid<Integer> B;
		DynamicNode<Integer> nA;
		DynamicNode<Integer> nB;
		
		if((c.above != null) && t.top.equals(c.above.top)) {
			A = c.above;
			nA = A.node;
			A.updateRight(t.upper_right, null);
			A.right = t.right;
		} else {
			A = new Trapezoid<Integer>(l, r, t.top, e);
			nA = new DynamicNode<Integer>(A);
			A.id = e.left;
			A.update(t.upper_left, c.above, t.upper_right, null);
			c.above = A;
		}
		
		if((c.below != null) && t.bottom.equals(c.below.bottom)) {
			B = c.below;
			nB = B.node;
			B.updateRight(null, t.lower_right);
			B.right = t.right;
		} else {
			B = new Trapezoid<Integer>(l, r, e, t.bottom);
			nB = new DynamicNode<Integer>(B);
			B.id = e.right;
			B.update(c.below, t.lower_left, null, t.lower_right);
			c.below = B;
		}
		
		t.node.nn = new geom.tmap.YNode<Trapezoid<Integer>>(e, nA, nB);
	}

	private void split4(Trapezoid<Integer> t, Edge e) {
		Trapezoid<Integer> A = new Trapezoid<Integer>(t.left, e.p, t.top, t.bottom);
		Trapezoid<Integer> B = new Trapezoid<Integer>(e.q, t.right, t.top, t.bottom);
		Trapezoid<Integer> C = new Trapezoid<Integer>(e.p, e.q, t.top, e);
		Trapezoid<Integer> D = new Trapezoid<Integer>(e.p, e.q, e, t.bottom);
		
		A.id = t.id;
		B.id = t.id;
		C.id = e.left;
		D.id = e.right;
		
		A.updateLeft(t.upper_left, t.lower_left);
		C.update(A, null, B, null);
		D.update(null, A, null, B);
		B.updateRight(t.upper_right, t.lower_right);
				
		t.node.nn = new geom.tmap.XNode<Trapezoid<Integer>>(
				e.p,
				new DynamicNode<Integer>(A),
				new DynamicNode<Integer>(
						e.q, 
						new DynamicNode<Integer>(
								e, 
								new DynamicNode<Integer>(C), 
								new DynamicNode<Integer>(D)), 
						new DynamicNode<Integer>(B)));
	}

	private void splitP(Trapezoid<Integer> t, Edge e, Context c) {
		Point2d r = min(e.q, t.right);
		
		Trapezoid<Integer> A = new Trapezoid<Integer>(t.left, e.p, t.top, t.bottom);
		Trapezoid<Integer> B = new Trapezoid<Integer>(e.p, r, t.top, e);
		Trapezoid<Integer> C = new Trapezoid<Integer>(e.p, r, e, t.bottom);
		
		A.id = t.id;
		B.id = e.left;
		C.id = e.right;
		
		A.updateLeft(t.upper_left, t.lower_left);
		B.update(A, null, t.upper_right, null);
		C.update(null, A, null, t.lower_right);
		
		t.node.nn = new geom.tmap.XNode<Trapezoid<Integer>>(
				e.p, 
				new DynamicNode<Integer>(A), 
				new DynamicNode<Integer>(
						e, 
						new DynamicNode<Integer>(B), 
						new DynamicNode<Integer>(C)));
		
		c.above = B;
		c.below = C;
	}

	private void splitQ(Trapezoid<Integer> t, Edge e, Context c) {
		Point2d l = max(e.p, t.left);
		
		Trapezoid<Integer> A = new Trapezoid<Integer>(e.q, t.right, t.top, t.bottom);
		A.id = t.id;
		Trapezoid<Integer> B;
		Trapezoid<Integer> C;
		DynamicNode<Integer> nB;
		DynamicNode<Integer> nC;
		
		if((c.above != null) && t.top.equals(c.above.top)) {
			B = c.above;
			nB = B.node;
			B.right = e.q;
		} else {
			B = new Trapezoid<Integer>(l, e.q, t.top, e);
			nB = new DynamicNode<Integer>(B);
			B.id = e.left;
			B.updateLeft(t.upper_left, c.above);
		}
		
		if((c.below != null) && t.bottom.equals(c.below.bottom)) {
			C = c.below;
			nC = C.node;
			C.right = e.q;
		} else {
			C = new Trapezoid<Integer>(l, e.q, e, t.bottom);
			nC = new DynamicNode<Integer>(C);
			C.id = e.right;
			C.updateLeft(c.below, t.lower_left);
		}
		
		t.node.nn = new geom.tmap.XNode<Trapezoid<Integer>>(
				e.q,
				new DynamicNode<Integer>(e, nB, nC),
				new DynamicNode<Integer>(A));
		
		A.update(B, C, t.upper_right, t.lower_right);
	}

	private void add(DynamicNode<Integer> dynamic_root, Edge e) {
		Context c = new Context();
		
		Trapezoid<Integer> d0 = dynamic_root.locate(e);
		
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
			
			Trapezoid<Integer> t = d0;

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
	
	private static void shuffle(Edge[] ar) {
		Random rnd = new Random();
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			Edge a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	public void close() throws FileNotFoundException {
		double xmin = Double.POSITIVE_INFINITY;
		double xmax = Double.NEGATIVE_INFINITY;
		double ymin = Double.POSITIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		
		for(Point2d p : points.values()) {
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
		points.clear();
		points = null;
		
		Trapezoid<Integer> D0 = new Trapezoid<Integer>(-1, new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new Edge(new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new Edge(new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
		DynamicNode<Integer> dynamic_root = new DynamicNode<Integer>(D0);
		D0.node = dynamic_root;
		
		Edge[] edge_list = edges.values().toArray(new Edge[] {});
//		shuffle(edge_list);
		
//		PrintStream out = new PrintStream("edges.txt");
//		for(EdgeLong e : edges.values()) {
//			out.println("LINESTRING ("+e.p.x+" "+e.p.y+", "+e.q.x+" "+e.q.y+")"
//					+ "|"+e.left
//					+ "|"+e.right);
//		}
//		out.close();
		
		for(Edge e : edges.values()) {
			add(dynamic_root, e);
		}
		edges.clear();
		edges = null;

		leaves = new HashMap<TNode, TNode>();
		segments = new HashMap<Segment, Segment>();
		ynodes = new HashMap<YNode, YNode>();
		root = simplify(dynamic_root);
		leaves.clear();
		leaves = null;
		segments.clear();
		segments = null;
		ynodes.clear();
		ynodes = null;
	}
	
	private Node simplify(DynamicNode<Integer> dn) {
		HashMap<DynamicNode<Integer>, Node> node_map = new HashMap<DynamicNode<Integer>, Node>();
		simplify(dn, node_map);
		
		return node_map.get(dn);
	}
	
	private Node simplify(DynamicNode<Integer> dn, HashMap<DynamicNode<Integer>, Node> node_map) {
		Node n = node_map.get(dn);
		
		if(n == null) {
			if(dn.nn instanceof geom.tmap.TNode<?>) {
				geom.tmap.TNode<Trapezoid<Integer>> t = (geom.tmap.TNode<Trapezoid<Integer>>)dn.nn;
				Integer id = t.id.id;
				
				if(id == null) {
					id = -1;
				}
				TNode tnl = new TNode(id);
				n = leaves.get(tnl);
				if(n == null) {
					n = tnl;
					leaves.put(tnl,  tnl);
				}
			} else if(dn.nn instanceof geom.tmap.XNode<?>) {
				geom.tmap.XNode<Trapezoid<Integer>> x = (geom.tmap.XNode<Trapezoid<Integer>>)dn.nn;
				
				Node left = simplify((DynamicNode<Integer>)x.left, node_map); 
				Node right = simplify((DynamicNode<Integer>)x.right, node_map); 
				n = new XNode(x.p, left, right);
			} else if(dn.nn instanceof geom.tmap.YNode<?>) {
				geom.tmap.YNode<Trapezoid<Integer>> y = (geom.tmap.YNode<Trapezoid<Integer>>)dn.nn;
				
				Node left = simplify((DynamicNode<Integer>)y.left, node_map); 
				Node right = simplify((DynamicNode<Integer>)y.right, node_map);
				Segment s = new Segment(y.s.p, y.s.q);
				YNode ynl = new YNode(s, left, right);
				n = ynodes.get(ynl);
				if(n == null) {
					n = ynl;
					ynodes.put(ynl, ynl);
				}
			} else {
				throw new RuntimeException("Bad node: "+dn.nn.getClass());
			}
			node_map.put(dn, n);
		}
		
		return n;
	}
	
	public Node root;
	transient HashMap<Tuple2<Point2d, Point2d>, Edge> edges;
	transient HashMap<Point2d, Point2d> points;
	transient HashMap<TNode, TNode> leaves;
	transient HashMap<Segment, Segment> segments;
	transient HashMap<YNode, YNode> ynodes;
}
