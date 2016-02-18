package geom;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.postgis.Geometry;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

public class IntervalTree_debug {
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
	
	static Polygon2d query(RedBlackBST2<Polygon2d, Double>.Node n, Point2d q) {	
		Polygon2d result = null;

		if(n != null)  {
			System.out.print(n.key.id+" "+n.key.min+" -- "+n.key.max+" max: "+n.val+" @ "+q+" ");
			if(n.key.contains(q)) {
				result = n.key;
				System.out.println("match");
			} else {
				if((n.left != null) && (n.left.val >= q.x)) {
					System.out.println("<-");
					result = query(n.left, q);
					if(result == null) {
						System.out.print(n.key.id+" "+n.key.min+" -- "+n.key.max+" max: "+n.val+" @ "+q+" ");
					}
				} 
				
				if(result == null){
					System.out.println("->");
					result = query(n.right, q);
				}
			}
		}
		
		return result;
	}
	
	static Polygon2d query2(RedBlackBST2<Polygon2d, Double>.Node n, Point2d q) {	
		Polygon2d result = null;
		
		if(n != null) {
			System.out.print(n.key.id+" "+n.key.min+" -- "+n.key.max+" max: "+n.val+" @ "+q+" ");
			if(q.x <= n.val) {
				if(n.key.contains(q)) {
					result = n.key;
					System.out.println("match");
				} else {
					System.out.println("<-");
					result = query2(n.left, q);
					
					if((result == null) && (q.x >= n.key.min.x)) {
						System.out.println(n.key.id+" "+n.key.min+" -- "+n.key.max+" max: "+n.val+" @ "+q+" ->");
						result = query2(n.right, q);
					}
				}
			}
		}
		
		return result;
	}
	
	static Polygon2d query4(RedBlackBST2<Polygon2d, Double>.Node n, Point2d q) {	
		Polygon2d result = null;

		if(n != null)  {
			if(n.key.contains(q)) {
				result = n.key;
			} else {
				result = query(n.right, q);
				
				if((result == null) && (n.left != null) && (n.left.val >= q.x)) {
					result = query(n.left, q);					
				} 
			}
		}
		
		return result;
	}

	static Polygon2d query3(RedBlackBST2<Polygon2d, Double> index, Point2d q) {
		Polygon2d result = null;
		
		RedBlackBST2<Polygon2d, Double>.Node x = index.root;
		
		while((x != null) && !x.key.contains(q)) {
			System.out.print(x.key.id+" "+x.key.min+" -- "+x.key.max+" max: "+x.val+" @ "+q+" ");
			if((x.left != null) && (x.left.val >= q.x)) {
				x = x.left;
				System.out.println("<-");
			} else {
				x = x.right;
				System.out.println("->");
			}
		}
		
		return result;
	}
	
	static Polygon2d find(RedBlackBST2<Polygon2d, Double>.Node n, String id) {
		Polygon2d result = null;

		if(n != null) {
			if(n.key.id.equals(id)) {
				result = n.key;
				System.out.println(n.key.id);
			} else {
				result = find(n.left, id);
				
				if(result == null) {
					result = find(n.right, id);
					
					if(result != null) {
						System.out.println(n.key.id+" ->");
					}
				} else {
					System.out.println(n.key.id+" <-");
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
			
			if("250173672004".equals(geoid)){
				System.out.println("250173672004 Here");
			}
			
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
		
		polys.sort(new PolygonMinXComparator());
		RedBlackBST2<Polygon2d, Double> rbt = new RedBlackBST2<Polygon2d, Double>(new PolygonMinXComparator());
		
		for(Polygon2d p : polys) {
			rbt.put(p, p.max.x);
		}
		
		setMax(rbt.root);
		
		System.out.println(new Date()+" index built.");
		
		Point2d q = new Point2d(-71.298943209636249,42.327448407587518);
		Polygon2d p = find(rbt.root, "250173672004");
		p = query(rbt.root, q);		
		System.out.println("Polygon: "+p);
		p = query2(rbt.root, q);		
		System.out.println("Polygon: "+p);
		p = query3(rbt, q);
		System.out.println("Polygon: "+p);
		p = query4(rbt.root, q);
		System.out.println("Polygon: "+p);
		
		System.out.println(new Date()+" done.");
	}
}
