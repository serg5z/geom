package geom.tmap_long;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import geom.Point2d;
import geom.Tuple2;
import geom.tmap.DynamicNode;
import geom.tmap.Segment;
import geom.tmap.Trapezoid;

public class IndexLong implements Index, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static class Context {
		Trapezoid<Long> above;
		Trapezoid<Long> below;		
	}

	public static class Singular implements Index, Serializable {
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
		edges = new HashMap<Tuple2<Point2d, Point2d>, Edge>();
		points = new HashMap<Point2d, Point2d>();
	}
	
	public long locate(Point2d q) {
		return root.locate(q);
	}
	
	public void add(Point2d p1, Point2d p2, long right_id) {
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
			if(e.left == -1) {
				e.left = right_id;
			} else {
				e.right = right_id;
			}
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

	private void split(Trapezoid<Long> t, Edge e, Context c) {
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
			B = new Trapezoid<Long>(l, r, e, t.bottom);
			nB = new DynamicNode<Long>(B);
			B.id = e.right;
			B.update(c.below, t.lower_left, null, t.lower_right);
			c.below = B;
		}
		
		t.node.nn = new geom.tmap.YNode<Trapezoid<Long>>(e, nA, nB);
	}

	private void split4(Trapezoid<Long> t, Edge e) {
		Trapezoid<Long> A = new Trapezoid<Long>(t.left, e.p, t.top, t.bottom);
		Trapezoid<Long> B = new Trapezoid<Long>(e.q, t.right, t.top, t.bottom);
		Trapezoid<Long> C = new Trapezoid<Long>(e.p, e.q, t.top, e);
		Trapezoid<Long> D = new Trapezoid<Long>(e.p, e.q, e, t.bottom);
		
		A.id = t.id;
		B.id = t.id;
		C.id = e.left;
		D.id = e.right;
		
		A.updateLeft(t.upper_left, t.lower_left);
		C.update(A, null, B, null);
		D.update(null, A, null, B);
		B.updateRight(t.upper_right, t.lower_right);
				
		t.node.nn = new geom.tmap.XNode<Trapezoid<Long>>(
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

	private void splitP(Trapezoid<Long> t, Edge e, Context c) {
		Point2d r = min(e.q, t.right);
		
		Trapezoid<Long> A = new Trapezoid<Long>(t.left, e.p, t.top, t.bottom);
		Trapezoid<Long> B = new Trapezoid<Long>(e.p, r, t.top, e);
		Trapezoid<Long> C = new Trapezoid<Long>(e.p, r, e, t.bottom);
		
		A.id = t.id;
		B.id = e.left;
		C.id = e.right;
		
		A.updateLeft(t.upper_left, t.lower_left);
		B.update(A, null, t.upper_right, null);
		C.update(null, A, null, t.lower_right);
		
		t.node.nn = new geom.tmap.XNode<Trapezoid<Long>>(
				e.p, 
				new DynamicNode<Long>(A), 
				new DynamicNode<Long>(
						e, 
						new DynamicNode<Long>(B), 
						new DynamicNode<Long>(C)));
		
		c.above = B;
		c.below = C;
	}

	private void splitQ(Trapezoid<Long> t, Edge e, Context c) {
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
			B.id = e.left;
			B.updateLeft(t.upper_left, c.above);
		}
		
		if((c.below != null) && t.bottom.equals(c.below.bottom)) {
			C = c.below;
			nC = C.node;
			C.right = e.q;
		} else {
			C = new Trapezoid<Long>(l, e.q, e, t.bottom);
			nC = new DynamicNode<Long>(C);
			C.id = e.right;
			C.updateLeft(c.below, t.lower_left);
		}
		
		t.node.nn = new geom.tmap.XNode<Trapezoid<Long>>(
				e.q,
				new DynamicNode<Long>(e, nB, nC),
				new DynamicNode<Long>(A));
		
		A.update(B, C, t.upper_right, t.lower_right);
	}

	private void add(DynamicNode<Long> dynamic_root, Edge e) {
		Context c = new Context();
		
		Trapezoid<Long> d0 = dynamic_root.locate(e);
		
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
		
		Trapezoid<Long> D0 = new Trapezoid<Long>(-1L, new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new Edge(new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new Edge(new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
		DynamicNode<Long> dynamic_root = new DynamicNode<Long>(D0);
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
	
	private Node simplify(DynamicNode<Long> dn) {
		HashMap<DynamicNode<Long>, Node> node_map = new HashMap<DynamicNode<Long>, Node>();
		simplify(dn, node_map);
		
		return node_map.get(dn);
	}
	
	private Node simplify(DynamicNode<Long> dn, HashMap<DynamicNode<Long>, Node> node_map) {
		Node n = node_map.get(dn);
		
		if(n == null) {
			if(dn.nn instanceof geom.tmap.TNode<?>) {
				geom.tmap.TNode<Trapezoid<Long>> t = (geom.tmap.TNode<Trapezoid<Long>>)dn.nn;
				Long id = t.id.id;
				
				if(id == null) {
					id = -1L;
				}
				TNode tnl = new TNode(id);
				n = leaves.get(tnl);
				if(n == null) {
					n = tnl;
					leaves.put(tnl,  tnl);
				}
			} else if(dn.nn instanceof geom.tmap.XNode<?>) {
				geom.tmap.XNode<Trapezoid<Long>> x = (geom.tmap.XNode<Trapezoid<Long>>)dn.nn;
				
				Node left = simplify((DynamicNode<Long>)x.left, node_map); 
				Node right = simplify((DynamicNode<Long>)x.right, node_map); 
				n = new XNode(x.p, left, right);
			} else if(dn.nn instanceof geom.tmap.YNode<?>) {
				geom.tmap.YNode<Trapezoid<Long>> y = (geom.tmap.YNode<Trapezoid<Long>>)dn.nn;
				
				Node left = simplify((DynamicNode<Long>)y.left, node_map); 
				Node right = simplify((DynamicNode<Long>)y.right, node_map);
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
