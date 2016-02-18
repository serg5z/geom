package geom.tmap.compact;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.postgis.Geometry;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import geom.Point2d;
import geom.tmap.DynamicSegment;
import geom.tmap.Trapezoid;

public class EdgeCollection {
	static class Context {
		Trapezoid<Long> above;
		Trapezoid<Long> below;		
	}
	
	static class PointRegistry {
		public PointRegistry(int N) {
			point_index = new HashMap<Point2d, Integer>(N);
			point = new Point2d[N];
			next = 0;			
		}
		
		public Point2d get(Point2d p) {
			Integer i = point_index.get(p);
			
			if(i == null) {
				point_index.put(p, next);
				i = next;
				point[i] = p;
				next++;
			}
			
			return point[i];
		}
		
		public int size() {
			return point_index.size();
		}
		
		public void clear() {
			point_index.clear();
			point_index = null;
		}
		
		HashMap<Point2d, Integer> point_index;
		Point2d[] point;
		int next;
	}
	
	static void add(Polygon poly, EdgeRegistryLong edges, long id) {
		int n = poly.numRings();
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			Point p = r.getPoint(0);
//			Point2d p1 = points.get(new Point2d(p.x, p.y));
			Point2d p1 = new Point2d(p.x, p.y);
			
			for(int k = 1; k < r.numPoints(); k++) {
				p = r.getPoint(k);
//				Point2d p2 = points.get(new Point2d(p.x, p.y));
				Point2d p2 = new Point2d(p.x, p.y);

				// look for other half of the edge
				edges.add(p1, p2, id);
								
				p1 = p2;
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
	
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		List<DynamicSegment<Long>> S;
		int N;
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM bg15 WHERE statefp not in ('02', '15', '60', '64', '66', '68', '69', '70', '72', '74', '78', '81', '84', '86', '67', '89', '71', '76', '95', '79')");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, tzid FROM tz_world");
		ResultSet rs = stmt.executeQuery();
		
		System.out.println(new Date()+" query executed.");

		EdgeRegistryLong edges = new EdgeRegistryLong(1000000);
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
		
		edges.close();
		System.out.println(new Date()+" edges created.");

//		S = new ArrayList<DynamicSegment<Long>>(edges.edge_index.size());
		
		PrintStream out = new PrintStream("edges.txt");
		for(EdgeLong e : edges.edge) {			
//			S.add(new DynamicSegment<Long>(e.p, e.q, e.left, e.right));
			out.println("LINESTRING ("+edges.point[e.p].x+" "+edges.point[e.p].y+", "+edges.point[e.p+1].x+" "+edges.point[e.p+1].y+")"
					+ "|"+e.p
					+ "|"+e.left
					+ "|"+e.right);
		}
		out.close();
		
//		edges.clear();
//		edges = null;
		
//		PrintStream out = new PrintStream("edges.txt");
		
//		Collections.sort(S, new Comparator<DynamicSegment<Long>>() {
//			@Override
//			public int compare(DynamicSegment<Long> s1, DynamicSegment<Long> s2) {
//				int result = s1.left_id.compareTo(s2.left_id);
//
//				if(result == 0) {
//					result = EdgeCollection.compare(s1.q, s2.p);
//					if(result != 0) {
//						result = -EdgeCollection.compare(s2.q, s1.p);
//					} else {
//						result = 1;
//					}
//				}
//				
//				return result;
//			}
//		});
		
//		for(int i = 0; i < S.size(); i++) {
//			DynamicSegment<Long> s = S.get(i);
//			out.println("LINESTRING ("+s.p.x+" "+s.p.y+", "+s.q.x+" "+s.q.y+")"
//					+ "|"+i
//					+ "|"+s.left_id
//					+ "|"+s.right_id);
//		}
//		
//		out.close();
		
		System.out.println(new Date()+" segments saved.");
	}
}
