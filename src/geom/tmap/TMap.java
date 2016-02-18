package geom.tmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import geom.Edge;
import geom.Point2d;
import geom.Polygon2d;
import geom.Tuple2;
import geom.tmap.compact.IndexLong;
import geom.tmap.compact.NodeLong;

public class TMap {
	static class Context<T> {
		Trapezoid<T> above;
		Trapezoid<T> below;		
	}

	static int no = 1;
	static PrintStream dout;
	
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
	
	static <T> void splitP(Trapezoid<T> t, DynamicSegment<T> s, Context<T> c, int split_no, int subsplit) {
		try {
//			dout.println("Start of segment(p) is in "+DandN(t));
			Point2d r = min(s.q, t.right);
			
			Trapezoid<T> A = new Trapezoid<T>(t.left, s.p, t.top, t.bottom);
			Trapezoid<T> B = new Trapezoid<T>(s.p, r, t.top, s);
			Trapezoid<T> C = new Trapezoid<T>(s.p, r, s, t.bottom);
			
			A.id = t.id;
			B.id = s.left_id;
			C.id = s.right_id;
			
			A.updateLeft(t.upper_left, t.lower_left);
			B.update(A, null, t.upper_right, null);
			C.update(null, A, null, t.lower_right);
			
			t.node.nn = new XNode<Trapezoid<T>>(
					s.p, 
					new DynamicNode<T>(A), 
					new DynamicNode<T>(
							s, 
							new DynamicNode<T>(B), 
							new DynamicNode<T>(C)));
			
			c.above = B;
			c.below = C;
//			dout.println(" Split D into 3 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.q.x);
			throw e;
		}
	}
	
	static <T> void splitQ(Trapezoid<T> t, DynamicSegment<T> s, Context<T> c, int split_no, int subsplit) {
		try {
//			dout.println("End of segment(q) is in "+DandN(t));
			Point2d l = max(s.p, t.left);
			
			Trapezoid<T> A = new Trapezoid<T>(s.q, t.right, t.top, t.bottom);
			A.id = t.id;
			Trapezoid<T> B;
			Trapezoid<T> C;
			DynamicNode<T> nB;
			DynamicNode<T> nC;
			
			if((c.above != null) && t.top.equals(c.above.top)) {
				B = c.above;
				nB = B.node;
				B.right = s.q;
			} else {
				B = new Trapezoid<T>(l, s.q, t.top, s);
				nB = new DynamicNode<T>(B);
				B.id = s.left_id;
				B.updateLeft(t.upper_left, c.above);
			}
			
			if((c.below != null) && t.bottom.equals(c.below.bottom)) {
				C = c.below;
				nC = C.node;
				C.right = s.q;
			} else {
				C = new Trapezoid<T>(l, s.q, s, t.bottom);
				nC = new DynamicNode<T>(C);
				C.id = s.right_id;
				C.updateLeft(c.below, t.lower_left);
			}
			
			t.node.nn = new XNode<Trapezoid<T>>(
					s.q,
					new DynamicNode<T>(s, nB, nC),
					new DynamicNode<T>(A));
			
			A.update(B, C, t.upper_right, t.lower_right);
			
//			dout.println(" Split D into 3 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.p.x);
			throw e;
		}
	}
	
	static <T> void split(Trapezoid<T> t, DynamicSegment<T> s, Context<T> c, int split_no, int subsplit) {
		try {
//			dout.println("Segment end points outside of "+DandN(t));
			Point2d l = t.left;
			Point2d r = t.right;
	
			Trapezoid<T> A;
			Trapezoid<T> B;
			DynamicNode<T> nA;
			DynamicNode<T> nB;
			
			if((c.above != null) && t.top.equals(c.above.top)) {
				A = c.above;
				nA = A.node;
				A.updateRight(t.upper_right, null);
				A.right = t.right;
			} else {
				A = new Trapezoid<T>(l, r, t.top, s);
				nA = new DynamicNode<T>(A);
				A.id = s.left_id;
				A.update(t.upper_left, c.above, t.upper_right, null);
				c.above = A;
			}
			
			if((c.below != null) && t.bottom.equals(c.below.bottom)) {
				B = c.below;
				nB = B.node;
				B.updateRight(null, t.lower_right);
				B.right = t.right;
			} else {
				B = new Trapezoid<T>(l, r, s, t.bottom);
				nB = new DynamicNode<T>(B);
				B.id = s.right_id;
				B.update(c.below, t.lower_left, null, t.lower_right);
				c.below = B;
			}
			
			t.node.nn = new YNode<Trapezoid<T>>(s, nA, nB);
			
//			dout.println(" Split D into 2 trapezoids: "+DandN(A)+", "+DandN(B));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.p.x);
			System.out.println(s.q.x);
			throw e;
		}
	}
	
	static <T> void split4(Trapezoid<T> t, DynamicSegment<T> s, int split_no, int subsplit) {
//		dout.println("Both points in "+DandN(t));

		Trapezoid<T> A = new Trapezoid<T>(t.left, s.p, t.top, t.bottom);
		Trapezoid<T> B = new Trapezoid<T>(s.q, t.right, t.top, t.bottom);
		Trapezoid<T> C = new Trapezoid<T>(s.p, s.q, t.top, s);
		Trapezoid<T> D = new Trapezoid<T>(s.p, s.q, s, t.bottom);
		
		A.id = t.id;
		B.id = t.id;
		C.id = s.left_id;
		D.id = s.right_id;
		
		A.updateLeft(t.upper_left, t.lower_left);
		C.update(A, null, B, null);
		D.update(null, A, null, B);
		B.updateRight(t.upper_right, t.lower_right);
				
		t.node.nn = new XNode<Trapezoid<T>>(
				s.p,
				new DynamicNode<T>(A),
				new DynamicNode<T>(
						s.q, 
						new DynamicNode<T>(
								s, 
								new DynamicNode<T>(C), 
								new DynamicNode<T>(D)), 
						new DynamicNode<T>(B)));
//		dout.println(" Split D into 4 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C)+", "+DandN(D));
	}
	
	static <T> String DandN(Trapezoid<T> t) {
		return t.id
				+" left: "+t.left
				+" right: "+t.right
				+" top: "+t.top
				+" bottom: "+t.bottom
				+" (ul: "+ (t.upper_left == null ? "" : t.upper_left.id)
				+" ,ll: "+ (t.lower_left == null ? "" : t.lower_left.id)
				+" ,ur: "+ (t.upper_right == null ? "" : t.upper_right.id)
				+" ,lr: "+ (t.lower_right == null ? "" : t.lower_right.id)+")";
	}
	
	static <T> DynamicNode<T> build(List<DynamicSegment<T>> S) throws FileNotFoundException {
		Point2d p = S.get(0).p;
		double xmin = p.x, xmax = p.x, ymin = p.y, ymax = p.y;
		int split_no = 0;
		
		for(DynamicSegment<T> s : S) {
			if(xmin > s.p.x) {
				xmin = s.p.x;
			}
			if(xmin > s.q.x) {
				xmin = s.q.x;
			}
			if(ymin > s.p.y) {
				ymin = s.p.y;
			}
			if(ymin > s.q.y) {
				ymin = s.q.y;
			}
			if(xmax < s.p.x) {
				xmax = s.p.x;
			}
			if(xmax < s.q.x) {
				xmax = s.q.x;
			}
			if(ymax < s.p.y) {
				ymax = s.p.y;
			}
			if(ymax < s.q.y) {
				ymax = s.q.y;
			}
		}
		
		Trapezoid<T> D0 = new Trapezoid<T>(new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new DynamicSegment<T>(new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new DynamicSegment<T>(new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
		D0.id = null;
		D0.upper_left = null;
		D0.upper_right = null;
		D0.lower_left = null;
		D0.lower_left = null;
	
		Collections.shuffle(S);
		
		DynamicNode<T> f = new DynamicNode<T>(D0);
		D0.node = f;
		
		DynamicNode<T> result = f;
		
		int n = 0;
		int prev = 0;
		int N = S.size();
		Context<T> c = new Context<T>();

		while(!S.isEmpty()) {
			DynamicSegment<T> s = S.remove(0);
			split_no++;
			c.above = null;
			c.below = null;
			int subsplit = 0;
			PrintStream out;
//			dout.print("Adding "+s+". ");

			Trapezoid<T> d0 = result.locate(s);
			
			if(compare(s.q, d0.right) == 0) {
				if(compare(s.p, d0.left) == 0) {
					split(d0, s, c, split_no, subsplit);
				} else {
					splitP(d0, s, c, split_no, subsplit);
				}
			} else if(compare(s.q, d0.right) < 0) {
				if(compare(d0.left, s.p) == 0) {
					splitQ(d0, s, c, split_no, subsplit);					
				} else {
					split4(d0, s, split_no, subsplit);
				}				
			} else {
				if(compare(d0.left, s.p) == 0) {
					split(d0, s, c, split_no, subsplit);					
				} else {
					splitP(d0, s, c, split_no, subsplit);
				}				
				
				Trapezoid<T> t = d0;

				if(s.below(d0.right)) {
					t = d0.lower_right;
				} else {
					t = d0.upper_right;
				}
				
				while(compare(s.q, t.right) > 0) {
					subsplit++;
					
					split(t, s, c, split_no, subsplit);
					
					if(s.below(t.right)) {
						t = t.lower_right;
					} else {
						t = t.upper_right;
					}
				}
				if(compare(s.q, t.right) == 0) {
					split(t, s, c, split_no, subsplit);
				} else {
					splitQ(t, s, c, split_no, subsplit);
				}
			}
			c.above = null;
			c.below = null;
			
			n++;
			
			if((n*10/N) != prev) {
				prev = n*10/N;
				System.out.println(prev*10+"% completed ("+n+" out of "+N+"). "+new Date());
			}
		}
		
		return result;
	}
	
	static <T> void write(DynamicNode<T> n, PrintStream out) {
//		if(!n.mark) {
//			n.mark = true;
//			int id = no++;
//			out.println("n"+id+";");
//			if(n.p != null) {
//				out.println("n"+id+" [label=\""+n.p+"\"];");
//			} else if(n.s != null) {
//				out.println("n"+id+" [label=\""+n.s.name+"\", style=filled, color=\"gray80\"];");			
//			} else {
//				out.println("n"+id+" [label=\""+n.d.name+"("+n.d.id+")\",shape=\"box\"];");
//			}
//			if(n.left != null && !((DynamicNode<T>)n.left).mark) {
//				if(n.p != null) {
//					out.println("edge [label=\"left\"];");
//				} else if(n.s != null) {
//					out.println("edge [label=\"above\"];");
//				}
//				out.print("n"+id+"");
//				out.print(" -> ");
//				write((DynamicNode<T>)n.left, out);
//			}
//			if(n.right != null && !((DynamicNode<T>)n.right).mark) {
//				if(n.p != null) {
//					out.println("edge [label=\"right\"];");
//				} else if(n.s != null) {
//					out.println("edge [label=\"below\"];");
//				}
//				out.print("n"+id+"");
//				out.print(" -> ");
//				write((DynamicNode<T>)n.right, out);
//			}
//		}
	}
	
	static <T> void write(Node<T> n, PrintStream out, Set<Node<T>> M) {
		if(!M.contains(n)) {
//			M.add(n);
//			out.println("n"+n.hashCode()+";");
			if(n instanceof XNode) {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\"];");
				if(((XNode<T>)n).left != null) {
//					out.println("edge [label=\"left\"];");
					out.println("n"+n.hashCode()+" -> n"+((XNode<T>)n).left.hashCode()+" [label=\"left\"];");
					write(((XNode<T>)n).left, out, M);
				}
				if(((XNode<T>)n).right != null) {
//					out.println("edge [label=\"right\"];");
					out.println("n"+n.hashCode()+" -> n"+((XNode<T>)n).right.hashCode()+" [label=\"right\"];");
					write(((XNode<T>)n).right, out, M);
				}
			} else if(n instanceof YNode) {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\", style=filled, color=\"gray80\"];");			
				if(((YNode<T>)n).left != null) {
//					out.println("edge [label=\"above\"];");
					out.println("n"+n.hashCode()+" -> n"+((YNode<T>)n).left.hashCode()+" [label=\"above\"];");
					write(((YNode<T>)n).left, out, M);
				}
				if(((YNode<T>)n).right != null) {
//					out.println("edge [label=\"below\"];");
					out.println("n"+n.hashCode()+" -> n"+((YNode<T>)n).right.hashCode()+" [label=\"below\"];");
					write(((YNode<T>)n).right, out, M);
				}
			} else {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\",shape=\"box\"];");
			}
		}
	}
	
	static <T> void writeMap(DynamicNode<T> n, PrintStream out) {
		if(n != null && !n.mark) {
			n.mark = true;
			if(n.nn instanceof TNode/* && (n.d.left.x != n.d.right.x)*/) {
				TNode<Trapezoid<T>> tn = (TNode<Trapezoid<T>>)n.nn;
				Trapezoid<T> t = tn.id;
				double at = t.top.slope();
				double ab = t.bottom.slope();
				if(Double.isFinite(ab) && Double.isFinite(at)) {
					out.print(t.id+"|");
					out.print("POLYGON((");
					double bt = t.top.p.y - at*t.top.p.x;
					double y = (at*t.left.x+bt);
					double x = t.left.x;
					out.print(x+" "+y+", ");
					y = (at*t.right.x+bt);
					x = t.right.x;
					out.print(x+" "+y+", ");
					double bb = t.bottom.p.y - ab*t.bottom.p.x;
					out.print(t.right.x+" "+(ab*t.right.x+bb)+", ");
					out.print(t.left.x+" "+(ab*t.left.x+bb)+", ");
					out.print(t.left.x+" "+(at*t.left.x+bt));
					out.print("))");
//					out.print("|"+t.top.left+"|"+t.bottom.right+"|");
					out.println("|"+t.left+
							"|"+t.right+
							"|"+(t.upper_left == null ? "null" : t.upper_left.id)+
							"|"+(t.lower_left == null ? "null" : t.lower_left.id)+
							"|"+(t.upper_right == null ? "null" : t.upper_right.id)+
							"|"+(t.lower_right == null ? "null" : t.lower_right.id));
					out.print(t.id+"-l|");
					out.print("POINT(");
					out.print(t.left.x+" "+t.left.y);
					out.print(")");
					out.println("|||||||||||");
					out.print(t.id+"-r|");
					out.print("POINT(");
					out.print(t.right.x+" "+t.right.y);
					out.print(")");
					out.println("|||||||||||");
				}			
			} else {			
				writeMap((DynamicNode<T>)((DecisionNode<Trapezoid<T>>)n.nn).left, out);
				writeMap((DynamicNode<T>)((DecisionNode<Trapezoid<T>>)n.nn).right, out);
			}
		}		
	}
	
	static <T> void setMark(DynamicNode<T> n, boolean mark) {
		if((n != null) && (n.mark != mark)) {
			n.mark = mark;
//			if(n.d != null) {
//				n.d.mark = mark;
//			}
//			setMark((DynamicNode<T>)n.left, mark);
//			setMark((DynamicNode<T>)n.right, mark);
		}
	}
	
	static <T> void writeLinks(Set<Trapezoid<T>> D, PrintStream out) {
		for(Trapezoid<T> t : D) {
			out.println(t.id+
					" ul: "+(t.upper_left == null ? "null" : t.upper_left.id)+
					" ll: "+(t.lower_left == null ? "null" : t.lower_left.id)+
					" ur: "+(t.upper_right == null ? "null" : t.upper_right.id)+
					" lr: "+(t.lower_right == null ? "null" : t.lower_right.id));
		}
	}
	
	static <T> void write(Set<Trapezoid<T>> map, PrintStream out) {
		for(Trapezoid<T> t : map) {
			double at = t.top.slope();
			double ab = t.bottom.slope();
			if(Double.isFinite(ab) && Double.isFinite(at) && (t.left.x != t.right.x)) {
				out.print(t.id+"|");
				out.print("POLYGON((");
				double bt = t.top.p.y - at*t.top.p.x;
				out.print(t.left.x+" "+(at*t.left.x+bt)+", ");
				out.print(t.right.x+" "+(at*t.right.x+bt)+", ");
				double bb = t.bottom.p.y - ab*t.bottom.p.x;
				out.print(t.right.x+" "+(ab*t.right.x+bb)+", ");
				out.print(t.left.x+" "+(ab*t.left.x+bb)+", ");
				out.print(t.left.x+" "+(at*t.left.x+bt));
				out.print("))");
//				out.print("|"+t.top.left_id+"|"+t.bottom.right_id+"|");
				out.println("|"+t.left+
						"|"+t.right+
						"|"+(t.upper_left == null ? "null" : t.upper_left.id)+
						"|"+(t.lower_left == null ? "null" : t.lower_left.id)+
						"|"+(t.upper_right == null ? "null" : t.upper_right.id)+
						"|"+(t.lower_right == null ? "null" : t.lower_right.id));
			}
		}
	}
		
	static void add(Polygon poly, HashMap<Tuple2<Point2d, Point2d>, Edge<Long>> edges, Long id) {
		int n = poly.numRings();
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			Point p = r.getPoint(0);
			Point2d p1 = new Point2d(p.x, p.y);
			
			for(int k = 1; k < r.numPoints(); k++) {
				p = r.getPoint(k);
				Point2d p2 = new Point2d(p.x, p.y);

				// look for other half of the edge
				Edge<Long> e = edges.get(new Tuple2<Point2d, Point2d>(p2, p1));
				
				if(e == null) {
					// Other half not present. Add this edge					
					e = new Edge<Long>(p1, p2);
					e.right = id;
					edges.put(new Tuple2<Point2d, Point2d>(p1, p2), e);
				} else {
					if(e.left != null) {
						throw new RuntimeException("Left side of the edge "+e+" is already assigned.");
					}
					
					e.left = id;
				}
				
				p1 = p2;
			}
		}
	}

	static void add(Polygon poly, EdgeRegistry<Long> edges, Long id) {
		int n = poly.numRings();
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			Point p = r.getPoint(0);
			Point2d p1 = new Point2d(p.x, p.y);
			
			for(int k = 1; k < r.numPoints(); k++) {
				p = r.getPoint(k);
				
				Point2d p2 = new Point2d(p.x, p.y);

				edges.add(p1, p2, id);
				
				p1 = p2;
			}
		}
	}

	static void add(LineString line, EdgeRegistry<Long> edges, Long id) {
		Point p = line.getPoint(0);
		Point2d p1 = new Point2d(p.x, p.y);

		for(int k = 1; k <  line.numPoints(); k++) {
			p = line.getPoint(k);
			
			Point2d p2 = new Point2d(p.x, p.y);

			edges.add(p1, p2, id);
			
			p1 = p2;
		}
	}

	public static void main(String[] args) throws Exception {
		dout = new PrintStream("history.txt");
		System.out.println(new Date()+" start.");
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM bg15");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
		PreparedStatement stmt = c.prepareStatement("SELECT (st_dump(st_boundary(geom))).geom as geom, geoid FROM tl_2015_25_bg");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, tzid FROM tz_world");
		ResultSet rs = stmt.executeQuery();
		
		System.out.println(new Date()+" query executed.");

//		HashMap<Tuple2<Point2d, Point2d>, Edge<Long>> edges = new HashMap<Tuple2<Point2d, Point2d>, Edge<Long>>(1000000);
		EdgeRegistry<Long> edges = new EdgeRegistry<Long>(100000000);
		while(rs.next()) {
			Geometry geom = ((PGgeometry)rs.getObject(1)).getGeometry();
			Long geoid = Long.parseLong(rs.getString(2));
			
			if(geom.getType() == Geometry.POLYGON) {
//				add((Polygon)geom, edges, geoid);
				add((Polygon)geom, edges, geoid);
			} else if(geom.getType() == Geometry.MULTIPOLYGON) {
				for(Polygon p : ((MultiPolygon)geom).getPolygons()) {
//					add(p, edges, geoid);
					add(p, edges, geoid);
				}
			} else if(geom.getType() == Geometry.LINESTRING) {
				add((LineString)geom, edges, geoid);
			} else {
				throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
			}
		}
		rs.close();
		stmt.close();
		c.close();
		System.out.println(new Date()+" edges created.");

		List<DynamicSegment<Long>> S = new LinkedList<DynamicSegment<Long>>();
		
//		for(Edge<Long> e : edges.values()) {
//			S.add(new DynamicSegment<Long>(e.p, e.q, e.left, e.right));
//		}
//		
//		edges.clear();
		edges.close();
		for(int e : edges.edge_index.values()) {
			S.add(new DynamicSegment<Long>(
					edges.point[e], edges.point[e+1], 
					edges.getLeft(e), edges.getRight(e)));
		}
		edges.left = null;
		edges.right = null;
		
		System.out.println(new Date()+" segments created.");

		DynamicNode<Long> dynamicIndex = build(S);
		DynamicNode.point_index = edges.point_index;
		DynamicNode.edge_index = edges.edge_index;
//		NodeLong root = dynamicIndex.simplify();
		IndexLong index = new IndexLong(/*edges.point.length, dynamicIndex.count()*/);
		for(int i = 0; i < edges.point.length; i++) {
			index.x[i] = edges.point[i].x;
			index.y[i] = edges.point[i].y;
		}
		edges.point = null;
		DynamicNode.index = index;
		dynamicIndex.simplify();
		dynamicIndex = null;
		index.close();
		edges.point_index.clear();
		edges.point_index = null;
		edges.edge_index.clear();
		edges.edge_index = null;
		DynamicNode.point_index = null;
		DynamicNode.edge_index = null;
		DynamicNode.index = null;
		DynamicNode.trapezoid_index.clear();;
		DynamicNode.trapezoid_index = null;
		DynamicNode.ynode_index.clear();
		DynamicNode.ynode_index = null;
		
//		dynamicIndex = null;
		System.out.println(new Date()+" index built.");
		
		System.gc();
		Thread.currentThread().sleep(1000);
		System.gc();
		Thread.currentThread().sleep(1000);
		
//		ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream("ma-compact-Long.idx"));
////		ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream("tz_world.idx"));
//		oout.writeObject(index);
//		oout.close();
		
//		setMark(dynamicIndex, false);
//		mout = new PrintStream("map-index-simple.txt");
//		writeMap(index, mout);
//		mout.close();
		System.out.println(new Date()+" index saved.");

//		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
//		
//		if(s.hasNext()) {
//			s.next();
//		}
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		br.readLine();
		
		int N = 0;
		int n = 0;
		Point2d q = new Point2d(0, 0);
		ArrayList<Point2d> points = new ArrayList<Point2d>(1100000);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("IntervalTree_tmap_MA.out")));
//		while(s.hasNext()) {
		String line;
		while((line = br.readLine()) != null) {
//			String[] p = s.next().split(",");
			String[] p = line.split(",");
//			Scanner ls = new Scanner(s.next());
//			ls.useDelimiter(",");
			
//			q.x = ls.nextDouble();
//			q.y = ls.nextDouble();
//			int id = ls.nextInt();
			q.x = Double.parseDouble(p[0]);
			q.y = Double.parseDouble(p[1]);
			int id = Integer.parseInt(p[2]);
			
			points.add(new Point2d(q.x, q.y));
			
			long bg = index.locate(q);
//			Trapezoid<Long> bgd = dynamicIndex.locate(q);

//			assert(bg == bgd.id);
//			Node<String> currentN = index;
//			DynamicNode<String> currentD = dynamicIndex;
//			Node<String> prevN = index;
//			DynamicNode<String> prevD = dynamicIndex;
//			
//			while((currentN != null) && (currentD != null) && (currentD.n == currentN)) {
//				prevN = currentN;
//				prevD = currentD;
//				currentN = currentN.step(q);
//				currentD = currentD.step(q);
//			}
//			
//			if(currentD != null)  {
//				if(currentD.n != currentN) {
//					System.out.println("D != N: "+currentD+" != "+currentN);
//				}
//			} else if(currentN != null) {
//				System.out.println("D != N: "+currentD+" != "+currentN);
//			}
			
//			if(bg == null) {
//				if(bgd != null) {
//					throw new RuntimeException("bg: "+bg+"; bgd: "+bgd+" @ "+q+" "+N);
//				}
//			} else if(!bg.equals(bgd)) {
//				throw new RuntimeException("bg: "+bg+"; bgd: "+bgd + " @ "+q+" "+N);				
//			}
			
			if(bg == 0) {
				out.println(id+",");
			} else {
				n++;
				out.println(id+","+bg);
			}
			
//			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		out.close();
		
		N = 0;
		n = 0;
		for(Point2d q2 : points) {			
//			String bg = index.locate(q2);
			Long bg = index.locate(q2);
			
			if(bg == null) {
			} else {
				n++;
			}
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}			
		}
		
		System.out.println("Points examined: "+N+". Geometry hits: "+n+".");
//		s.close();
		br.close();
		
		System.out.println(new Date()+" done.");
	}

/* simple test	

	public static void main(String[] args) throws FileNotFoundException {
		dout = new PrintStream("test-history.txt");

		double e = 0.01;
		List<DynamicSegment<String>> S = new LinkedList<DynamicSegment<String>>();

		S.add(new DynamicSegment<String>("-10", new Point2d(-1, -1), new Point2d(3, -1), "1", "2"));
		S.add(new DynamicSegment<String>("10", new Point2d(-1, 2), new Point2d(3, 2), "1", "2"));
		S.add(new DynamicSegment<String>("2", new Point2d(1, 1), new Point2d(2, 1), "1", "2"));
		S.add(new DynamicSegment<String>("1", new Point2d(0, 1), new Point2d(1, 1), "1", "2"));

//		S.add(new DynamicSegment<String>("-10", new Point2d(-1, -1), new Point2d(3, -1), "1", "2"));
//		S.add(new DynamicSegment<String>("10", new Point2d(-1, 2), new Point2d(3, 2), "1", "2"));
//		S.add(new DynamicSegment<String>("1", new Point2d(0, 1), new Point2d(1, 1), "1", "2"));
//		S.add(new DynamicSegment<String>("2", new Point2d(1, 1), new Point2d(2, 1), "1", "2"));
		
//		S.add(new DynamicSegment<String>("24049-0", new Point2d(-72.274742, 42.24109), new Point2d(-72.27471899999999, 42.2410752), "0", "out"));
//		S.add(new DynamicSegment<String>("24049-1", new Point2d(-72.274725, 42.24106), new Point2d(-72.27471899999999, 42.2410752), "out", "0"));
		
//		S.add(new DynamicSegment<String>("7152-2", new Point2d(-71.122036, 42.295221), new Point2d(-71.121602, 42.29505599999999), "out", "0"));
////		S.add(new DynamicSegment<String>("13561-0", new Point2d(-71.121752, 42.295615999999995), new Point2d(-71.121668, 42.295587999999995), "0", "out"));
//		S.add(new DynamicSegment<String>("13561-1", new Point2d(-71.121752, 42.295615999999995), new Point2d(-71.121602, 42.295859), "0", "out"));
//		S.add(new DynamicSegment<String>("6043-12", new Point2d(-71.121696, 42.295395), new Point2d(-71.121408, 42.295233), "out", "0"));
		
//		S.add(new DynamicSegment<String>("7152-2", new Point2d(-71.122036, 42.262921), new Point2d(-71.121602, 42.26275599999999), "out", "0"));
//		S.add(new DynamicSegment<String>("13561-0", new Point2d(-71.121752, 42.407115999999995), new Point2d(-71.121668, 42.407087999999995), "0", "out"));
//		S.add(new DynamicSegment<String>("13561-1", new Point2d(-71.121752, 42.407115999999995), new Point2d(-71.121602, 42.407359), "0", "out"));
//		S.add(new DynamicSegment<String>("6043-12", new Point2d(-71.121696, 42.287395), new Point2d(-71.121408, 42.287233), "out", "0"));
		
//		S.add(new DynamicSegment<String>("S1", new Point2d(0, 0), new Point2d(5, 0), "out", "0"));
//		S.add(new DynamicSegment<String>("S2", new Point2d(0+e*4, 4), new Point2d(5+e*4, 4), "0", "out"));
//		S.add(new DynamicSegment<String>("S3", new Point2d(1+e*1, 1), new Point2d(2+e, 1), "2", "1"));
//		S.add(new DynamicSegment<String>("S4", new Point2d(3+e*1, 1), new Point2d(4+e, 1), "7", "6"));
//		S.add(new DynamicSegment<String>("S5", new Point2d(1+e*3, 3), new Point2d(2+e*3, 3), "3", "2"));
//		S.add(new DynamicSegment<String>("S6", new Point2d(3+e*3, 3), new Point2d(4+e*3, 3), "8", "3"));
//		S.add(new DynamicSegment<String>("S7", new Point2d(2+e*2, 2), new Point2d(3+e*2, 2), "5", "4"));
		
//		S.add(new DynamicSegment<String>("S1", new Point2d(0, 0), new Point2d(5, 0), "out", "0"));
//		S.add(new DynamicSegment<String>("S2", new Point2d(0, 4), new Point2d(5, 4), "0", "out"));
//		S.add(new DynamicSegment<String>("S3", new Point2d(1, 1), new Point2d(2, 1), "2", "1"));
//		S.add(new DynamicSegment<String>("S4", new Point2d(3, 1), new Point2d(4, 1), "7", "6"));
//		S.add(new DynamicSegment<String>("S5", new Point2d(1, 3), new Point2d(2, 3), "3", "2"));
//		S.add(new DynamicSegment<String>("S6", new Point2d(3, 3), new Point2d(4, 3), "8", "3"));
//		S.add(new DynamicSegment<String>("S7", new Point2d(2, 2), new Point2d(3, 2), "5", "4"));
		
//		S.add(new Segment("S1", new Point2d(0, 1), new Point2d(1 ,2), "2", "1"));
//		S.add(new Segment("S2", new Point2d(2, 1), new Point2d(3 ,1), "6", "5"));
//		S.add(new Segment("S3", new Point2d(3, 2), new Point2d(2 ,2), "6", "7"));
//		S.add(new Segment("S4", new Point2d(1, 2), new Point2d(0 ,2), "2", "3"));
//		S.add(new Segment("S5", new Point2d(0, 3), new Point2d(1 ,3), "4", "3"));
//		S.add(new Segment("S6", new Point2d(2, 4), new Point2d(3 ,3), "8", "7"));
//		S.add(new Segment("S7", new Point2d(3, 4), new Point2d(2 ,4), "8", "10"));
//		S.add(new Segment("S8", new Point2d(1, 4), new Point2d(0 ,4), "4", "9"));
//		S.add(new Segment("S9", new Point2d(1, 1.5), new Point2d(2 ,1.5), "12", "11"));
//		S.add(new Segment("S10", new Point2d(2, 3.5), new Point2d(1 ,3.5), "12", "13"));
//		S.add(new Segment("S11", new Point2d(0, 2.5), new Point2d(3 ,2.5), "q1", "q2"));
		
//		S.add(new Segment("S1",  new Point2d(0+e*1, 1),   new Point2d(1+e*2, 2), "2", "1"));
//		S.add(new Segment("S2",  new Point2d(2+e*1, 1),   new Point2d(3+e*1, 1), "6", "5"));
//		S.add(new Segment("S3",  new Point2d(3+e*2, 2),   new Point2d(2+e*2, 2), "6", "7"));
//		S.add(new Segment("S4",  new Point2d(1+e*2, 2),   new Point2d(0+e*2, 2), "2", "3"));
//		S.add(new Segment("S5",  new Point2d(0+e*3, 3),   new Point2d(1+e*3, 3), "4", "3"));
//		S.add(new Segment("S6",  new Point2d(2+e*4, 4),   new Point2d(3+e*3, 3), "8", "7"));
//		S.add(new Segment("S7",  new Point2d(3+e*4, 4),   new Point2d(2+e*4, 4), "8", "10"));
//		S.add(new Segment("S8",  new Point2d(1+e*4, 4),   new Point2d(0+e*4, 4), "4", "9"));
//		S.add(new Segment("S9",  new Point2d(1+e*1.5, 1.5), new Point2d(2+e*1.5, 1.5), "12", "11"));
//		S.add(new Segment("S10", new Point2d(2+e*3.5, 3.5), new Point2d(1+e*3.5, 3.5), "12", "13"));
//		S.add(new Segment("S11", new Point2d(0+e*2.5, 2.5), new Point2d(3+e*2.5, 2.5), "q1", "q2"));
		
//		S.add(new Segment("S1", new Point2d(2, 2), new Point2d(0 ,1), "1", "4"));
//		S.add(new Segment("S2", new Point2d(3, 3), new Point2d(2 ,2), "1", "4"));
//		S.add(new Segment("S3", new Point2d(0, 1), new Point2d(1 ,0), "1", "5"));
//		S.add(new Segment("S4", new Point2d(1, 0), new Point2d(4 ,1), "1", "2"));
//		S.add(new Segment("S5", new Point2d(4, 1), new Point2d(3 ,3), "1", "3"));
		
//		S.add(new Segment("S5", new Point2d(3, 3), new Point2d(2 ,2), 1, 0));
//		S.add(new Segment("S3", new Point2d(1, 0), new Point2d(4 ,1), 1, 0));
//		S.add(new Segment("S4", new Point2d(4, 1), new Point2d(3 ,3), 1, 0));
//		S.add(new Segment("S2", new Point2d(0, 1), new Point2d(1 ,0), 1, 0));
//		S.add(new Segment("S1", new Point2d(2, 2), new Point2d(0 ,1), 1, 0));
		
		DynamicNode<String> root = build(S);
		
		PrintStream out = new PrintStream("tree.dot");
		setMark((DynamicNode<String>) root, false);
		out.println("digraph G {");
		write(root, out);
		out.println("}");
		out.close();
		
		Node<String> simple = root.simplify();
		out = new PrintStream("tree-simple.dot");
		out.println("digraph G {");
		write(simple, out, new HashSet<Node<String>>());
		out.println("}");
		out.close();
		
		out = new PrintStream("map-index.txt");
		setMark(root, false);
		writeMap(root, out);
		out.close();
		
		out = new PrintStream("map.txt");
		setMark(root, false);
		write(T, out);
		out.close();
		
		Random r = new Random();
		for(int i = 0; i < 100000; i++) {
			Point2d q = new Point2d(r.nextDouble(), r.nextDouble());
			
			String l1 = root.locate(q);
			String l2 = simple.locate(q);
			
			if(l1 == null) {
				if(l2 != null) {
					throw new RuntimeException(l1 + " != "+l2+" @ "+q);
				}
			} else if(!l1.equals(l2)) {
				throw new RuntimeException(l1 + " != "+l2+" @ "+q);				
			}
		}
		
		System.out.println("Done.");
	}
*/
}
