package geom.tmap_long;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
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
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import geom.Point2d;
import geom.Tuple2;
import geom.tmap.Segment;
import geom.tmap.XNode;
import geom.tmap.YNode;

public class TMap {
	static int no = 1;
	static PrintStream dout;
	static HashMap<Point2d, Point2d> points = new HashMap<Point2d, Point2d>(40000000);
	
	static class Context {
		Trapezoid above;
		Trapezoid below;		
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
	
	static void splitP(Trapezoid t, EdgeLong s, Context c, int split_no, int subsplit) {
		try {
//			dout.println("Start of segment(p) is in "+DandN(t));
			Point2d r = min(s.q, t.right);
			
			Trapezoid A = new Trapezoid(t.left, s.p, t.top, t.bottom);
			Trapezoid B = new Trapezoid(s.p, r, t.top, s);
			Trapezoid C = new Trapezoid(s.p, r, s, t.bottom);
			
			A.id = t.id;
			B.id = s.left;
			C.id = s.right;
			
			A.updateLeft(t.upper_left, t.lower_left);
			B.update(A, null, t.upper_right, null);
			C.update(null, A, null, t.lower_right);
			
			t.node.nn = new XNode<Trapezoid>(
					s.p, 
					new DynamicNode(A), 
					new DynamicNode(
							s, 
							new DynamicNode(B), 
							new DynamicNode(C)));
			
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
	
	static <T> void splitQ(Trapezoid t, EdgeLong s, Context c, int split_no, int subsplit) {
		try {
//			dout.println("End of segment(q) is in "+DandN(t));
			Point2d l = max(s.p, t.left);
			
			Trapezoid A = new Trapezoid(s.q, t.right, t.top, t.bottom);
			A.id = t.id;
			Trapezoid B;
			Trapezoid C;
			DynamicNode nB;
			DynamicNode nC;
			
			if((c.above != null) && t.top.equals(c.above.top)) {
				B = c.above;
				nB = B.node;
				B.right = s.q;
			} else {
				B = new Trapezoid(l, s.q, t.top, s);
				nB = new DynamicNode(B);
				B.id = s.left;
				B.updateLeft(t.upper_left, c.above);
			}
			
			if((c.below != null) && t.bottom.equals(c.below.bottom)) {
				C = c.below;
				nC = C.node;
				C.right = s.q;
			} else {
				C = new Trapezoid(l, s.q, s, t.bottom);
				nC = new DynamicNode(C);
				C.id = s.right;
				C.updateLeft(c.below, t.lower_left);
			}
			
			t.node.nn = new XNode<Trapezoid>(
					s.q,
					new DynamicNode(s, nB, nC),
					new DynamicNode(A));
			
			A.update(B, C, t.upper_right, t.lower_right);
			
//			dout.println(" Split D into 3 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.p.x);
			throw e;
		}
	}
	
	static <T> void split(Trapezoid t, EdgeLong s, Context c, int split_no, int subsplit) {
		try {
//			dout.println("Segment end points outside of "+DandN(t));
			Point2d l = t.left;
			Point2d r = t.right;
	
			Trapezoid A;
			Trapezoid B;
			DynamicNode nA;
			DynamicNode nB;
			
			if((c.above != null) && t.top.equals(c.above.top)) {
				A = c.above;
				nA = A.node;
				A.updateRight(t.upper_right, null);
				A.right = t.right;
			} else {
				A = new Trapezoid(l, r, t.top, s);
				nA = new DynamicNode(A);
				A.id = s.left;
				A.update(t.upper_left, c.above, t.upper_right, null);
				c.above = A;
			}
			
			if((c.below != null) && t.bottom.equals(c.below.bottom)) {
				B = c.below;
				nB = B.node;
				B.updateRight(null, t.lower_right);
				B.right = t.right;
			} else {
				B = new Trapezoid(l, r, s, t.bottom);
				nB = new DynamicNode(B);
				B.id = s.right;
				B.update(c.below, t.lower_left, null, t.lower_right);
				c.below = B;
			}
			
			t.node.nn = new YNode<Trapezoid>(s, nA, nB);
			
//			dout.println(" Split D into 2 trapezoids: "+DandN(A)+", "+DandN(B));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.p.x);
			System.out.println(s.q.x);
			throw e;
		}
	}
	
	static <T> void split4(Trapezoid t, EdgeLong s, int split_no, int subsplit) {
//		dout.println("Both points in "+DandN(t));

		Trapezoid A = new Trapezoid(t.left, s.p, t.top, t.bottom);
		Trapezoid B = new Trapezoid(s.q, t.right, t.top, t.bottom);
		Trapezoid C = new Trapezoid(s.p, s.q, t.top, s);
		Trapezoid D = new Trapezoid(s.p, s.q, s, t.bottom);
		
		A.id = t.id;
		B.id = t.id;
		C.id = s.left;
		D.id = s.right;
		
		A.updateLeft(t.upper_left, t.lower_left);
		C.update(A, null, B, null);
		D.update(null, A, null, B);
		B.updateRight(t.upper_right, t.lower_right);
				
		t.node.nn = new XNode<Trapezoid>(
				s.p,
				new DynamicNode(A),
				new DynamicNode(
						s.q, 
						new DynamicNode(
								s, 
								new DynamicNode(C), 
								new DynamicNode(D)), 
						new DynamicNode(B)));
//		dout.println(" Split D into 4 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C)+", "+DandN(D));
	}
	
	static <T> String DandN(Trapezoid t) {
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
	
	static <T> void add(DynamicNode root, EdgeLong[] S) {
		int split_no = 0;
		Context c = new Context();
		
		for(EdgeLong s : S) {
			split_no++;
			if(s != null) {
				c.above = null;
				c.below = null;
				int subsplit = 0;
				PrintStream out;
	//			dout.print("Adding "+s+". ");
	
				Trapezoid d0 = root.locate(s);
				
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
					
					Trapezoid t = d0;
	
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
			}
		}
	}
	
	static <T> DynamicNode build(List<EdgeLong> S) throws FileNotFoundException {
		Point2d p = S.get(0).p;
		double xmin = p.x, xmax = p.x, ymin = p.y, ymax = p.y;
		int split_no = 0;
		
		for(EdgeLong s : S) {
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
		
		Trapezoid D0 = new Trapezoid(new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new Segment(new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new Segment(new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
		D0.id = -1;
		D0.upper_left = null;
		D0.upper_right = null;
		D0.lower_left = null;
		D0.lower_left = null;
	
		Collections.shuffle(S);
		
		DynamicNode f = new DynamicNode(D0);
		D0.node = f;
		
		DynamicNode result = f;
		
		int n = 0;
		int prev = 0;
		int N = S.size();
		Context c = new Context();

		while(!S.isEmpty()) {
			EdgeLong s = S.remove(0);
			split_no++;
			c.above = null;
			c.below = null;
			int subsplit = 0;
			PrintStream out;
//			dout.print("Adding "+s+". ");

			Trapezoid d0 = result.locate(s);
			
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
				
				Trapezoid t = d0;

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
			
			n++;
			
			if((n*10/N) != prev) {
				prev = n*10/N;
				System.out.println(new Date()+" "+prev*10+"% completed ("+n+" out of "+N+").");
			}
		}
		
		return result;
	}
	
	static void write(DynamicNode n, PrintStream out) {
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
	
	static <T> void write(NodeLong n, PrintStream out, Set<NodeLong> M) {
		if(!M.contains(n)) {
//			M.add(n);
//			out.println("n"+n.hashCode()+";");
			if(n instanceof XNodeLong) {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\"];");
				if(((XNodeLong)n).left != null) {
//					out.println("edge [label=\"left\"];");
					out.println("n"+n.hashCode()+" -> n"+((XNodeLong)n).left.hashCode()+" [label=\"left\"];");
					write(((XNodeLong)n).left, out, M);
				}
				if(((XNodeLong)n).right != null) {
//					out.println("edge [label=\"right\"];");
					out.println("n"+n.hashCode()+" -> n"+((XNodeLong)n).right.hashCode()+" [label=\"right\"];");
					write(((XNodeLong)n).right, out, M);
				}
			} else if(n instanceof YNodeLong) {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\", style=filled, color=\"gray80\"];");			
				if(((YNodeLong)n).left != null) {
//					out.println("edge [label=\"above\"];");
					out.println("n"+n.hashCode()+" -> n"+((YNodeLong)n).left.hashCode()+" [label=\"above\"];");
					write(((YNodeLong)n).left, out, M);
				}
				if(((YNodeLong)n).right != null) {
//					out.println("edge [label=\"below\"];");
					out.println("n"+n.hashCode()+" -> n"+((YNodeLong)n).right.hashCode()+" [label=\"below\"];");
					write(((YNodeLong)n).right, out, M);
				}
			} else {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\",shape=\"box\"];");
			}
		}
	}
	
	static <T> void writeMap(DynamicNode n, PrintStream out) {
//		if(n != null && !n.mark) {
//			n.mark = true;
//			if(n.nn instanceof TNodeLong/* && (n.d.left.x != n.d.right.x)*/) {
//				TNode<Trapezoid> tn = (TNode<Trapezoid>)n.nn;
//				Trapezoid t = tn.id;
//				double at = t.top.slope();
//				double ab = t.bottom.slope();
//				if(Double.isFinite(ab) && Double.isFinite(at)) {
//					out.print(t.id+"|");
//					out.print("POLYGON((");
//					double bt = t.top.p.y - at*t.top.p.x;
//					double y = (at*t.left.x+bt);
//					double x = t.left.x;
//					out.print(x+" "+y+", ");
//					y = (at*t.right.x+bt);
//					x = t.right.x;
//					out.print(x+" "+y+", ");
//					double bb = t.bottom.p.y - ab*t.bottom.p.x;
//					out.print(t.right.x+" "+(ab*t.right.x+bb)+", ");
//					out.print(t.left.x+" "+(ab*t.left.x+bb)+", ");
//					out.print(t.left.x+" "+(at*t.left.x+bt));
//					out.print("))");
//					out.print("|"+t.top.left_id+"|"+t.bottom.right_id+"|");
//					out.println("|"+t.left+
//							"|"+t.right+
//							"|"+(t.upper_left == null ? "null" : t.upper_left.id)+
//							"|"+(t.lower_left == null ? "null" : t.lower_left.id)+
//							"|"+(t.upper_right == null ? "null" : t.upper_right.id)+
//							"|"+(t.lower_right == null ? "null" : t.lower_right.id));
//					out.print(t.id+"-l|");
//					out.print("POINT(");
//					out.print(t.left.x+" "+t.left.y);
//					out.print(")");
//					out.println("|||||||||||");
//					out.print(t.id+"-r|");
//					out.print("POINT(");
//					out.print(t.right.x+" "+t.right.y);
//					out.print(")");
//					out.println("|||||||||||");
//				}			
//			} else {			
//				writeMap((DynamicNode)((DecisionNode)n.nn).left, out);
//				writeMap((DynamicNode)((DecisionNode)n.nn).right, out);
//			}
//		}	
	}
	
	static <T> void writeLinks(Set<Trapezoid> D, PrintStream out) {
		for(Trapezoid t : D) {
			out.println(t.id+
					" ul: "+(t.upper_left == null ? "null" : t.upper_left.id)+
					" ll: "+(t.lower_left == null ? "null" : t.lower_left.id)+
					" ur: "+(t.upper_right == null ? "null" : t.upper_right.id)+
					" lr: "+(t.lower_right == null ? "null" : t.lower_right.id));
		}
	}
	
	static <T> void write(Set<Trapezoid> map, PrintStream out) {
//		for(Trapezoid t : map) {
//			double at = t.top.slope();
//			double ab = t.bottom.slope();
//			if(Double.isFinite(ab) && Double.isFinite(at) && (t.left.x != t.right.x)) {
//				out.print(t.id+"|");
//				out.print("POLYGON((");
//				double bt = t.top.p.y - at*t.top.p.x;
//				out.print(t.left.x+" "+(at*t.left.x+bt)+", ");
//				out.print(t.right.x+" "+(at*t.right.x+bt)+", ");
//				double bb = t.bottom.p.y - ab*t.bottom.p.x;
//				out.print(t.right.x+" "+(ab*t.right.x+bb)+", ");
//				out.print(t.left.x+" "+(ab*t.left.x+bb)+", ");
//				out.print(t.left.x+" "+(at*t.left.x+bt));
//				out.print("))");
//				out.print("|"+t.top.left_id+"|"+t.bottom.right_id+"|");
//				out.println("|"+t.left+
//						"|"+t.right+
//						"|"+(t.upper_left == null ? "null" : t.upper_left.id)+
//						"|"+(t.lower_left == null ? "null" : t.lower_left.id)+
//						"|"+(t.upper_right == null ? "null" : t.upper_right.id)+
//						"|"+(t.lower_right == null ? "null" : t.lower_right.id));
//			}
//		}
	}
		
	static void add(Polygon poly, HashMap<Tuple2<Point2d, Point2d>, EdgeLong> edges, long id) {
		int n = poly.numRings();
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			Point p = r.getPoint(0);
			Point2d p1 = new Point2d(p.x, p.y);
			
			if(points.containsKey(p1)) {
				p1 = points.get(p1);
			} else {
				points.put(p1, p1);
			}
			
			for(int k = 1; k < r.numPoints(); k++) {
				p = r.getPoint(k);
				Point2d p2 = new Point2d(p.x, p.y);

				if(points.containsKey(p2)) {
					p2 = points.get(p2);
				} else {
					points.put(p2, p2);
				}
				
				// look for other half of the edge
				EdgeLong e = edges.get(new Tuple2<Point2d, Point2d>(p2, p1));
				
				if(e == null) {
					// Other half not present. Add this edge					
					e = new EdgeLong(p1, p2);
					e.right = id;
					edges.put(new Tuple2<Point2d, Point2d>(p1, p2), e);
				} else {
					if(e.left != -1) {
						throw new RuntimeException("Left side of the edge "+e+" is already assigned.");
					}
					
					e.left = id;
				}
				
				p1 = p2;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		dout = new PrintStream("history.txt");
		System.out.println(new Date()+" start.");
		double xmin = -180.0, xmax = 180.0, ymin = -90.0, ymax = 90.0;
		EdgeLong[] S;
		ObjectOutputStream oout;
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM bg15 WHERE statefp not in ('02', '15', '60', '64', '66', '68', '69', '70', '72', '74', '78', '81', '84', '86', '67', '89', '71', '76', '95', '79')");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, tzid FROM tz_world");
		ResultSet rs = stmt.executeQuery();
		
		System.out.println(new Date()+" query executed.");

		HashMap<Tuple2<Point2d, Point2d>, EdgeLong> edges = new HashMap<Tuple2<Point2d, Point2d>, EdgeLong>(1000000);
		while(rs.next()) {
			Geometry geom = ((PGgeometry)rs.getObject(1)).getGeometry();
			long geoid = Long.valueOf(rs.getString(2)).longValue();
			
			if(geom.getType() == Geometry.POLYGON) {
				add((Polygon)geom, edges, geoid);
			} else if(geom.getType() == Geometry.MULTIPOLYGON) {
				for(Polygon p : ((MultiPolygon)geom).getPolygons()) {
					add(p, edges, geoid);
				}
			} else {
				throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
			}
		}
		rs.close();
		stmt.close();
		c.close();
		System.out.println(new Date()+" edges created.");

		List<EdgeLong> ed = new LinkedList<EdgeLong>(edges.values());
		Collections.shuffle(ed);

		Point2d p = ed.get(0).p;
		xmin = p.x; xmax = p.x; ymin = p.y; ymax = p.y;
		S = new EdgeLong[100000];
		oout = new ObjectOutputStream(new FileOutputStream("ma-long.bin"));
		int i = 0;

		for(EdgeLong e : ed) {
			EdgeLong s = new EdgeLong(e.p, e.q, e.left, e.right);
			
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
			
			if(i == S.length) {
				i = 0;
				oout.writeObject(S);
				oout.reset();
				System.out.print('.');
			}
			
			S[i] = s;
			i++;
		}
		
		oout.writeObject(S);
		oout.close();
		S = null;
		System.out.println();
		
		edges.clear();
		edges = null;
		System.out.println(new Date()+" segments saved.");
		
		System.gc();
		Thread.currentThread().sleep(1000);
		System.gc();
		Thread.currentThread().sleep(1000);

		Trapezoid D0 = new Trapezoid(new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new EdgeLong(new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new EdgeLong(new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
		D0.id = -1;
		D0.upper_left = null;
		D0.upper_right = null;
		D0.lower_left = null;
		D0.lower_left = null;
			
		DynamicNode root = new DynamicNode(D0);
		D0.node = root;

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream("ma-long.bin"));
		
		S = (EdgeLong[])ois.readObject();
		while(S != null) {
			add(root, S);
			System.out.print('.');
			try {
				S = null;
				S = (EdgeLong[])ois.readObject();
			}
			catch(EOFException e) {
				// no-op
			}
		}
		
		ois.close();
		System.out.println(new Date()+" index built.");

//		PrintStream tout = new PrintStream("tree.dot");
//		tout.println("digraph G {");
//		setMark(dynamicIndex, false);
//		write(dynamicIndex, tout);
//		tout.println("}");
//		tout.close();
//		
		
//		PrintStream mout = new PrintStream("ma-index.txt");
//		PrintStream mout = new PrintStream("tz_world-index.txt");
//		writeMap(dynamicIndex, mout);
//		mout.close();

//		HashMap<DynamicNode<String>, Node<String>> M = new HashMap<DynamicNode<String>, Node<String>>(1000000);
//		setMark(dynamicIndex, false);
		NodeLong index = root.simplify();
		
		root = null;
		
		System.gc();
		Thread.currentThread().sleep(1000);
		System.gc();
		Thread.currentThread().sleep(1000);
		
//		oout = new ObjectOutputStream(new FileOutputStream("ma-long.idx"));
////		ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream("tz_world.idx"));
//		oout.writeObject(index);
//		oout.close();
		
//		setMark(dynamicIndex, false);
//		PrintStream mout = new PrintStream("map-index-simple.txt");
//		writeMap(index, mout);
//		mout.close();
//		System.out.println(new Date()+" index saved.");

		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;
		int n = 0;
		Point2d q = new Point2d(0, 0);
		ArrayList<Point2d> points = new ArrayList<Point2d>(1100000);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("IntervalTree_tmap_MA.out")));
		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			q.x = ls.nextDouble();
			q.y = ls.nextDouble();
			int id = ls.nextInt();
			
			points.add(new Point2d(q.x, q.y));
			
			long bg = index.locate(q);
//			String bgd = dynamicIndex.locate(q);

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
			
			if(bg == -1) {
				out.println(id+",");
			} else {
				n++;
				out.println(id+","+bg);
			}
			
			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		out.close();
		
		N = 0;
		n = 0;
		for(Point2d q2 : points) {			
			long bg = index.locate(q2);
			
			if(bg != -1) {
				n++;
			}
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}			
		}
		
		System.out.println("Points examined: "+N+". Geometry hits: "+n+".");
		s.close();	
		
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
