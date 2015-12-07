package geom.tmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.HashSet;
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

public class TMap {
	static int no = 1;
	static Set<Trapezoid<String>> T = new HashSet<Trapezoid<String>>();
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
	
	static <T> void splitP(Trapezoid<T> t, DynamicSegment<T> s, int split_no, int subsplit) {
		try {
			dout.println("Start of segment(p) is in "+DandN(t));
//			Point2d r = (s.q.x == t.right.x) ? s.q :  t.right;
			Point2d r = min(s.q, t.right);
			
			Trapezoid<T> A = new Trapezoid<T>("Ap-"+split_no+"-"+subsplit, t.left, s.p, t.top, t.bottom);
			Trapezoid<T> B = new Trapezoid<T>("Bp-"+split_no+"-"+subsplit, s.p, r, t.top, s);
			Trapezoid<T> C = new Trapezoid<T>("Cp-"+split_no+"-"+subsplit, s.p, r, s, t.bottom);
			
			A.id = t.id;
			B.id = s.left_id;
			C.id = s.right_id;
			
			A.updateLeft(t.upper_left, t.lower_left);
			B.update(A, null, t.upper_right, null);
			C.update(null, A, null, t.lower_right);
			
			t.node.d = null;
			t.node.s = null;
			t.node.p = s.p;
			t.node.left = new DynamicNode<T>(A);
			t.node.right = new DynamicNode<T>(s, new DynamicNode<T>(B), new DynamicNode<T>(C));
			
			s.above = B;
			s.below = C;
			
	//		T.add(A);
	//		T.add(B);
	//		T.add(C);
	//		
			dout.println(" Split D into 3 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.q.x);
			throw e;
		}
	}
	
	static <T> void splitQ(Trapezoid<T> t, DynamicSegment<T> s, int split_no, int subsplit) {
		try {
			dout.println("End of segment(q) is in "+DandN(t));
//			Point2d l = (s.p.x == t.left.x) ? s.p : t.left; //reverse?
//			Point2d l = (s.p.x == t.left.x) ? t.left : s.p;
			Point2d l = max(s.p, t.left);
			
			Trapezoid<T> A = new Trapezoid<T>("Aq-"+split_no+"-"+subsplit, s.q, t.right, t.top, t.bottom);
			A.id = t.id;
			Trapezoid<T> B;
			Trapezoid<T> C;
			DynamicNode<T> nB;
			DynamicNode<T> nC;
			
			if((s.above != null) && t.top.equals(s.above.top)) {
				B = s.above;
				nB = B.node;
	//			T.remove(B);
//				B.upper_right = A;
				B.right = s.q;
			} else {
				B = new Trapezoid<T>("Bq-"+split_no+"-"+subsplit, l, s.q, t.top, s);
				nB = new DynamicNode<T>(B);
				B.id = s.left_id;
				B.updateLeft(t.upper_left, s.above);
			}
			
			if((s.below != null) && t.bottom.equals(s.below.bottom)) {
				C = s.below;
				nC = C.node;
	//			T.remove(C);
//				C.lower_right = A;
				C.right = s.q;
			} else {
				C = new Trapezoid<T>("Cq-"+split_no+"-"+subsplit, l, s.q, s, t.bottom);
				nC = new DynamicNode<T>(C);
				C.id = s.right_id;
				C.updateLeft(s.below, t.lower_left);
			}
			
			t.node.d = null;
			t.node.s = null;
			t.node.p = s.q;
			t.node.left = new DynamicNode<T>(s, nB, nC);
			t.node.right = new DynamicNode<T>(A);
			
			A.update(B, C, t.upper_right, t.lower_right);
			
	//		T.add(A);
	//		T.add(B);
	//		T.add(C);
			dout.println(" Split D into 3 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.p.x);
			throw e;
		}
	}
	
	static <T> void split(Trapezoid<T> t, DynamicSegment<T> s, int split_no, int subsplit) {
		try {
			dout.println("Segment end points outside of "+DandN(t));
			Point2d l = t.left;
			Point2d r = t.right;
	
			Trapezoid<T> A;
			Trapezoid<T> B;
			DynamicNode<T> nA;
			DynamicNode<T> nB;
			
			if((s.above != null) && t.top.equals(s.above.top)) {
				A = s.above;
				nA = A.node;
	//			T.remove(A);
				A.updateRight(t.upper_right, null);
				A.right = t.right;
			} else {
				A = new Trapezoid<T>("A2-"+split_no+"-"+subsplit, l, r, t.top, s);
				nA = new DynamicNode<T>(A);
				A.id = s.left_id;
				A.update(t.upper_left, s.above, t.upper_right, null);
				s.above = A;
			}
			
			if((s.below != null) && t.bottom.equals(s.below.bottom)) {
				B = s.below;
				nB = B.node;
	//			T.remove(B);
				B.updateRight(null, t.lower_right);
				B.right = t.right;
			} else {
				B = new Trapezoid<T>("B2-"+split_no+"-"+subsplit, l, r, s, t.bottom);
				nB = new DynamicNode<T>(B);
				B.id = s.right_id;
				B.update(s.below, t.lower_left, null, t.lower_right);
				s.below = B;
			}
			
			t.node.d = null;
			t.node.s = s;
			t.node.p = null;
			t.node.left = nA;
			t.node.right = nB;
			
	//		T.add(A);
	//		T.add(B);
			dout.println(" Split D into 2 trapezoids: "+DandN(A)+", "+DandN(B));
		} catch(Throwable e) {
			System.out.println(t);
			System.out.println(s);
			System.out.println(s.p.x);
			System.out.println(s.q.x);
			throw e;
		}
	}
	
	static <T> String DandN(Trapezoid<T> t) {
		return t.name
				+" left: "+t.left
				+" right: "+t.right
				+" top: "+t.top.name
				+" bottom: "+t.bottom.name
				+" (ul: "+ (t.upper_left == null ? "" : t.upper_left.name)
				+" ,ll: "+ (t.lower_left == null ? "" : t.lower_left.name)
				+" ,ur: "+ (t.upper_right == null ? "" : t.upper_right.name)
				+" ,lr: "+ (t.lower_right == null ? "" : t.lower_right.name)+")";
	}
	
	static <T> void split4(Trapezoid<T> t, DynamicSegment<T> s, int split_no, int subsplit) {
		dout.println("Both points in "+DandN(t));

		Trapezoid<T> A = new Trapezoid<T>("A1-"+split_no+"-"+subsplit, t.left, s.p, t.top, t.bottom);
		Trapezoid<T> B = new Trapezoid<T>("B1-"+split_no+"-"+subsplit, s.q, t.right, t.top, t.bottom);
		Trapezoid<T> C = new Trapezoid<T>("C1-"+split_no+"-"+subsplit, s.p, s.q, t.top, s);
		Trapezoid<T> D = new Trapezoid<T>("D1-"+split_no+"-"+subsplit, s.p, s.q, s, t.bottom);
		
		A.id = t.id;
		B.id = t.id;
		C.id = s.left_id;
		D.id = s.right_id;
		
		A.updateLeft(t.upper_left, t.lower_left);
		C.update(A, null, B, null);
		D.update(null, A, null, B);
		B.updateRight(t.upper_right, t.lower_right);
		
		t.node.d = null;
		t.node.s = null;
		t.node.p = s.p;
		t.node.left = new DynamicNode<T>(A);
		t.node.right = new DynamicNode<T>(s.q, new DynamicNode<T>(s, new DynamicNode<T>(C), new DynamicNode<T>(D)), new DynamicNode<T>(B));
		
//		T.add(A);
//		T.add(B);
//		T.add(C);
//		T.add(D);
		
		dout.println(" Split D into 4 trapezoids: "+DandN(A)+", "+DandN(B)+", "+DandN(C)+", "+DandN(D));
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
		
		Trapezoid<T> D0 = new Trapezoid<T>("box", new Point2d(xmin, ymin), new Point2d(xmax, ymax), 
				new DynamicSegment<T>("box-top", new Point2d(xmin, ymax), new Point2d(xmax, ymax)), 
				new DynamicSegment<T>("box-bottom", new Point2d(xmin, ymin), new Point2d(xmax, ymin)));
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

		for(DynamicSegment<T> s : S) {			
			split_no++;
			s.above = null;
			s.below = null;
			int subsplit = 0;
				PrintStream out;
				dout.print("Adding "+s+". ");
//				PrintStream out = new PrintStream("index-before-"+split_no+".dot");
//				out.println("digraph G {");
//				write(result, out);
//				out.println("}");
//				out.close();
//				mout = new PrintStream("map-before-"+split_no+".txt");
//				write(T, mout);
//				mout.close();

			Trapezoid<T> d0 = result.locateT(s);
			
			if(compare(s.q, d0.right) == 0) {
				if(compare(s.p, d0.left) == 0) {
					split(d0, s, split_no, subsplit);
				} else {
					splitP(d0, s, split_no, subsplit);
				}
			} else if(compare(s.q, d0.right) < 0) {
				if(compare(d0.left, s.p) == 0) {
					splitQ(d0, s, split_no, subsplit);					
				} else {
					split4(d0, s, split_no, subsplit);
				}				
			} else {
				if(compare(d0.left, s.p) == 0) {
					split(d0, s, split_no, subsplit);					
				} else {
					splitP(d0, s, split_no, subsplit);
				}				
				
				Trapezoid<T> t = d0;

				if(s.below(d0.right)) {
					t = d0.lower_right;
				} else {
					t = d0.upper_right;
				}
				
				while(compare(s.q, t.right) > 0) {
					subsplit++;
					
//						dout.println("t: "+t.name+" ur: "+(t.upper_right == null ? "" : t.upper_right.name) + "lr: "+(t.lower_right == null ? "" : t.lower_right.name)+" next: "+(next == null ? "" : next.name));
										
					split(t, s, split_no, subsplit);
					
					if(s.below(t.right)) {
						t = t.lower_right;
					} else {
						t = t.upper_right;
					}
				}
				if(compare(s.q, t.right) == 0) {
					split(t, s, split_no, subsplit);
				} else {
					splitQ(t, s, split_no, subsplit);
				}
			}
			n++;
			
			if((n*10/S.size()) != prev) {
				prev = n*10/S.size();
				System.out.println(prev*10+"% completed ("+n+" out of "+S.size()+").");
			}
//			setMark(result, false);
//			out = new PrintStream("index-after-"+split_no+".dot");
//			out.println("digraph G {");
//			write(result, out);
//			out.println("}");
//			out.close();

//			setMark(result, false);
//			out = new PrintStream("map-"+split_no+".txt");
//			writeMap(result, out);
//			out.close();
		}
		
		return result;
	}
	
	static <T> Trapezoid<T> find(DynamicNode<T> n, Trapezoid<T> x) {
		Trapezoid<T> result = null;
		
		if(!n.mark) {
			n.mark = true;
			if(n.d != null) {
				if(n.d.upper_left == x || n.d.lower_left == x || n.d.upper_right == x || n.d.lower_right == x) {
					result = n.d;
				}
			} else {
				result = find((DynamicNode<T>)n.left, x);
				if(result == null) {
					result = find((DynamicNode<T>)n.right, x);
				}
			}
		}
		
		return result;
	}
	
	static <T> void write(DynamicNode<T> n, PrintStream out) {
		if(!n.mark) {
			n.mark = true;
			int id = no++;
			out.println("n"+id+";");
			if(n.p != null) {
				out.println("n"+id+" [label=\""+n.p+"\"];");
			} else if(n.s != null) {
				out.println("n"+id+" [label=\""+n.s.name+"\", style=filled, color=\"gray80\"];");			
			} else {
				out.println("n"+id+" [label=\""+n.d.name+"("+n.d.id+")\",shape=\"box\"];");
			}
			if(n.left != null && !((DynamicNode<T>)n.left).mark) {
				if(n.p != null) {
					out.println("edge [label=\"left\"];");
				} else if(n.s != null) {
					out.println("edge [label=\"above\"];");
				}
				out.print("n"+id+"");
				out.print(" -> ");
				write((DynamicNode<T>)n.left, out);
			}
			if(n.right != null && !((DynamicNode<T>)n.right).mark) {
				if(n.p != null) {
					out.println("edge [label=\"right\"];");
				} else if(n.s != null) {
					out.println("edge [label=\"below\"];");
				}
				out.print("n"+id+"");
				out.print(" -> ");
				write((DynamicNode<T>)n.right, out);
			}
		}
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
			if(n.d != null/* && (n.d.left.x != n.d.right.x)*/) {
				Trapezoid<T> t = n.d;
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
					out.print("|"+t.top.name+"|"+t.bottom.name+"|"+t.top.left_id+"|"+t.bottom.right_id+"|");
					out.println("|"+t.name+
							"|"+t.left+
							"|"+t.right+
							"|"+(t.upper_left == null ? "null" : t.upper_left.name)+
							"|"+(t.lower_left == null ? "null" : t.lower_left.name)+
							"|"+(t.upper_right == null ? "null" : t.upper_right.name)+
							"|"+(t.lower_right == null ? "null" : t.lower_right.name));
					out.print(t.name+"-l|");
					out.print("POINT(");
					out.print(t.left.x+" "+t.left.y);
					out.print(")");
					out.println("|||||||||||");
					out.print(t.name+"-r|");
					out.print("POINT(");
					out.print(t.right.x+" "+t.right.y);
					out.print(")");
					out.println("|||||||||||");
				}			
			}
			writeMap((DynamicNode<T>)n.left, out);
			writeMap((DynamicNode<T>)n.right, out);
		}		
	}
	
	static <T> void setMark(DynamicNode<T> n, boolean mark) {
		if((n != null) && (n.mark != mark)) {
			n.mark = mark;
			if(n.d != null) {
				n.d.mark = mark;
			}
			setMark((DynamicNode<T>)n.left, mark);
			setMark((DynamicNode<T>)n.right, mark);
		}
	}
	
	static <T> void writeLinks(DynamicNode<T> n, PrintStream out) {
		if(n != null) {
			if(n.d != null && !n.d.mark) {
				Trapezoid<T> t = n.d;
				t.mark = true;
				out.println(t.name+
						" ul: "+(t.upper_left == null ? "null" : t.upper_left.name)+
						" ll: "+(t.lower_left == null ? "null" : t.lower_left.name)+
						" ur: "+(t.upper_right == null ? "null" : t.upper_right.name)+
						" lr: "+(t.lower_right == null ? "null" : t.lower_right.name));
			}
			writeLinks((DynamicNode<T>)n.left, out);
			writeLinks((DynamicNode<T>)n.right, out);
		}		
	}
	
	static <T> void writeLinks(Set<Trapezoid<T>> D, PrintStream out) {
		for(Trapezoid<T> t : D) {
			out.println(t.name+
					" ul: "+(t.upper_left == null ? "null" : t.upper_left.name)+
					" ll: "+(t.lower_left == null ? "null" : t.lower_left.name)+
					" ur: "+(t.upper_right == null ? "null" : t.upper_right.name)+
					" lr: "+(t.lower_right == null ? "null" : t.lower_right.name));
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
				out.print("|"+t.top.name+"|"+t.bottom.name+"|"+t.top.left_id+"|"+t.bottom.right_id+"|");
				out.println("|"+t.name+
						"|"+t.left+
						"|"+t.right+
						"|"+(t.upper_left == null ? "null" : t.upper_left.name)+
						"|"+(t.lower_left == null ? "null" : t.lower_left.name)+
						"|"+(t.upper_right == null ? "null" : t.upper_right.name)+
						"|"+(t.lower_right == null ? "null" : t.lower_right.name));
			}
		}
	}
		
	static void add(Polygon poly, HashMap<Tuple2<Point2d, Point2d>, Edge<String>> edges, String id) {
		int n = poly.numRings();
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			Point p = r.getPoint(0);
			Point2d p1 = new Point2d(p.x, p.y);
			
			for(int k = 1; k < r.numPoints(); k++) {
				p = r.getPoint(k);
				Point2d p2 = new Point2d(p.x, p.y);

				// look for other half of the edge
				Edge<String> e = edges.get(new Tuple2<Point2d, Point2d>(p2, p1));
				
				if(e == null) {
					// Other half not present. Add this edge					
					e = new Edge<String>(p1, p2);
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

	public static void main(String[] args) throws Exception {
		dout = new PrintStream("history.txt");
		System.out.println(new Date()+" start.");
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
		PreparedStatement stmt = c.prepareStatement("SELECT geom, tzid FROM tz_world");
		ResultSet rs = stmt.executeQuery();
		List<Polygon2d> polys = new ArrayList<Polygon2d>(10000);
		
		System.out.println(new Date()+" query executed.");

		HashMap<Tuple2<Point2d, Point2d>, Edge<String>> edges = new HashMap<Tuple2<Point2d, Point2d>, Edge<String>>(1000000);
		while(rs.next()) {
			Geometry geom = ((PGgeometry)rs.getObject(1)).getGeometry();
			String geoid = rs.getString(2);
			
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
		
		List<DynamicSegment<String>> S = new LinkedList<DynamicSegment<String>>();
		
		for(Edge<String> e : edges.values()) {
			S.add(new DynamicSegment<String>("", e.p, e.q, e.left, e.right));
		}
		
//		HashMap<Point2d, Point2d> P = new HashMap<Point2d, Point2d>(1000000);	
//
//		List<DynamicSegment<String>> S = new LinkedList<DynamicSegment<String>>();
//		while(rs.next()) {
//			Geometry geom = ((PGgeometry)rs.getObject("geom")).getGeometry();
//			String right_id = rs.getString("right_id");
//			String left_id = rs.getString("left_id");
//			String edge_id = rs.getString("edge_id");
//			
//			if(right_id == null) {
//				right_id = "0";
//			}
//			
//			if(left_id == null) {
//				left_id = "0";
//			}
//			
//			if(geom.getType() == Geometry.LINESTRING) {
//				Point[] points = ((LineString)geom).getPoints();
//				
//				for(int i = 1; i < points.length; i++) {
//					if(points[i].x != points[i-1].x) {
//						Point2d t = new Point2d(points[i-1].x, points[i-1].y);
//						Point2d p = P.get(t);
//						
//						if(p == null) {
//							p = t;
//							P.put(t, t);
//						}
//						
//						t = new Point2d(points[i].x, points[i].y);
//						Point2d q = P.get(t);
//						
//						if(q == null) {
//							q = t;
//							P.put(t, t);
//						}
//						
//						S.add(new DynamicSegment<String>(edge_id+"-"+(i-1), p, q, left_id, right_id));
//					}
//				}
//			} else {
//				throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
//			}
//		}
//		rs.close();
//		stmt.close();
//		c.close();

		DynamicNode<String> dynamicIndex = build(S);
		System.out.println(new Date()+" index built.");

//		PrintStream tout = new PrintStream("tree.dot");
//		tout.println("digraph G {");
//		setMark(dynamicIndex, false);
//		write(dynamicIndex, tout);
//		tout.println("}");
//		tout.close();
//		
		
		setMark(dynamicIndex, false);
//		PrintStream mout = new PrintStream("ma-index.txt");
		PrintStream mout = new PrintStream("tz_world-index.txt");
		writeMap(dynamicIndex, mout);
		mout.close();

//		HashMap<DynamicNode<String>, Node<String>> M = new HashMap<DynamicNode<String>, Node<String>>(1000000);
//		setMark(dynamicIndex, false);
		Node<String> index = dynamicIndex.simplify();
		
		dynamicIndex = null;
		
		System.gc();
		Thread.currentThread().sleep(1000);
		System.gc();
		Thread.currentThread().sleep(1000);
		
//		ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream("ma.idx"));
		ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream("tz_world.idx"));
		oout.writeObject(index);
		oout.close();
		
//		setMark(dynamicIndex, false);
//		mout = new PrintStream("map-index-simple.txt");
//		writeMap(index, mout);
//		mout.close();
		System.out.println(new Date()+" index saved.");

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
			
			String bg = index.locate(q);
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
			
			if(("0".equals(bg)) || (bg == null)) {
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
			String bg = index.locate(q2);
			
			if(("0".equals(bg)) || (bg == null)) {
			} else {
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
