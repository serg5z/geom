package geom;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.postgis.Geometry;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

public class IntervalTree_bfs_MA {
	static class Node implements Serializable {
		private static final long serialVersionUID = 1L;
		
		Polygon2d polygon;
		double max;
	}
	
	static int fls(int f)
	{
	    int order;
	    for (order = 0; f != 0; f >>= 1, order++) ;

	    return order;
	}

	static int ilog2(int f)
	{
	    return fls(f) - 1;
	}
	
	static int hyperceil(int f)
	{
	    return 1 << fls(f-1);
	}
	
	
	static void toArray(Node[] index, RedBlackBST2<Polygon2d, Double>.Node n, int level) {
		if(n != null) {
			assert(index[level] == null);
			index[level] = new Node();
			index[level].polygon = n.key;
			index[level].max = n.val;
			toArray(index, n.left, 2*level);
			toArray(index, n.right, 2*level+1);
		}
	}
	
	static Node[] buildIndex(List<Polygon2d> polys) {
		polys.sort(new PolygonMinXComparator());
		RedBlackBST2<Polygon2d, Double> rbt = new RedBlackBST2<Polygon2d, Double>(new PolygonMinXComparator());
		
		for(Polygon2d p : polys) {
			rbt.put(p, p.max.x);
		}
		
		setMax(rbt.root);

		int n = polys.size()*2;
		n--;
		n |= n >> 1;
		n |= n >> 2;
		n |= n >> 4;
		n |= n >> 8;
		n |= n >> 16;
	    n++;
				
		Node[] result = new Node[n];
		
		toArray(result, rbt.root, 1);
		
		return result;
	}

	private static final class PolygonMinXComparator implements Comparator<Polygon2d> {
		@Override
		public int compare(Polygon2d o1, Polygon2d o2) {
			if(o1.min.x < o2.min.x)
				return -1;
			else if (o1.min.x > o2.min.x)
				return 1;
			else 
				return o1.id.compareTo(o2.id);
		}
	}

	static void add(Polygon poly, List<Polygon2d> polys, String id) {
		int n = poly.numRings();
		Point2d p0 = new Point2d(0, 0);
		Point2d[] ps = new Point2d[poly.numPoints()+(n > 1 ? 2*n : 0)];
		int j = 0;
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			
			if(n > 1) {
				ps[j++] = p0; 
			}
			
			for(int k = 0; k < r.numPoints(); k++) {
				Point p = r.getPoint(k);
				
				ps[j] = new Point2d(p.x, p.y);
				p0.add(ps[j]);
				j++;
			}

			if(n > 1) {
				ps[j++] = p0; 
			}
		}
		
		p0.scale(1.0/j);
		
		polys.add(new Polygon2d(id, ps));
	}
	
	static void setMax(RedBlackBST2<Polygon2d, Double>.Node n) {
		if(n != null) {
			setMax(n.left);
			setMax(n.right);
			
			if((n.left != null) && (n.val < n.left.val)) {
				n.val = n.left.val;
			}
			if((n.right != null) && (n.val < n.right.val)) {
				n.val = n.right.val;
			}
		}
	}
	
	static Polygon2d query(Node[] index, Point2d q) {
		return query(index, q, 1);
	}
	
	static Polygon2d query(Node[] index, Point2d q, int i) {	
		Polygon2d result = null;
		Node n = index[i];

		if(n != null)  {
			if(n.polygon.contains(q)) {
				result = n.polygon;
			} else {
				int left = 2*i; 
				if((left < index.length) && (index[left] != null) && (index[left].max >= q.x)) {
					result = query(index, q, left);					
				} 
				
				if(result == null){
					result = query(index, q, left+1);
				}
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
		ResultSet rs = stmt.executeQuery();
		List<Polygon2d> polys = new ArrayList<Polygon2d>(10000);
		
		System.out.println(new Date()+" query executed.");

		while(rs.next()) {
			Geometry geom = ((PGgeometry)rs.getObject(1)).getGeometry();
			String geoid = rs.getString(2);
			
			if(geom.getType() == Geometry.POLYGON) {
				add((Polygon)geom, polys, geoid);
			} else if(geom.getType() == Geometry.MULTIPOLYGON) {
				for(Polygon p : ((MultiPolygon)geom).getPolygons()) {
					add(p, polys, geoid);
				}
			} else {
				throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
			}
		}
		rs.close();
		stmt.close();
		c.close();

		Node[] index = buildIndex(polys);
		
		System.out.println(new Date()+" index built.");

		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;
		int n = 0;
		Point2d q = new Point2d(0, 0);
		ArrayList<Point2d> points = new ArrayList<Point2d>(1100000);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("IntervalTree_bfs_MA.out")));
		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			q.x = ls.nextDouble();
			q.y = ls.nextDouble();
			int id = ls.nextInt();
			
			points.add(q);
			
			Polygon2d p = query(index, q);
			
			if(p == null) {
				out.println(id+",");
			} else {
				n++;
				out.println(id+","+p.id);
			}
			
			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		out.close();
		
		N = 0;
		for(Point2d q2 : points) {
			query(index, q2);
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}			
		}
		
		System.out.println("Points examined: "+N+". Geometry hits: "+n+".");
		s.close();	
		
		System.out.println(new Date()+" done.");
	}
}
