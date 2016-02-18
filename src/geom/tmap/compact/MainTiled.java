package geom.tmap.compact;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;

import geom.Point2d;
import geom.tmap.DynamicNode;

public class MainTiled {
	static int tileid(double x, double y) {
		return ((int)Math.ceil(y)+90)*360+((int)Math.ceil(x)+180);
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		ObjectOutputStream oout;
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT MIN(st_xmin(geom)) as xmin, MIN(st_ymin(geom)) as ymin, MAX(st_xmax(geom)) as xmax, MAX(st_ymax(geom)) as ymax FROM bg15b WHERE statefp not in ('02', '15', '60', '64', '66', '68', '69', '70', '72', '74', '78', '81', '84', '86', '67', '89', '71', '76', '95', '79')");
//		PreparedStatement stmt = c.prepareStatement("SELECT MIN(st_xmin(geom)) as xmin, MIN(st_ymin(geom)) as ymin, MAX(st_xmax(geom)) as xmax, MAX(st_ymax(geom)) as ymax FROM tl_2015_25_bg");
		ResultSet rs = stmt.executeQuery();
		
		rs.next();
		double xmin = rs.getDouble(1);
		double ymin = rs.getDouble(2);
		double xmax = rs.getDouble(3);
		double ymax = rs.getDouble(4);
		
		System.out.println("Building tiled index for region ("+xmin+", "+ymin+") - ("+xmax+", "+ymax+")");

		HashMap<Integer, Index> tile_index = new HashMap<Integer, Index>();
		
		stmt = c.prepareStatement("SELECT (st_dump(st_intersection(geom, ST_SetSRID(ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)), 4326)))).geom as geom, geoid FROM bg15b "
				+ "WHERE "
				+ "	st_intersects(geom, ST_SetSRID(ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)), 4326))"
				+ " AND statefp not in ('02', '15', '60', '64', '66', '68', '69', '70', '72', '74', '78', '81', '84', '86', '67', '89', '71', '76', '95', '79')");
//		stmt = c.prepareStatement("SELECT (st_dump(st_intersection((st_dump(st_boundary(geom))).geom, ST_SetSRID(ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)), 4326)))).geom as geom, geoid FROM tl_2015_25_bg");
		for(double x = Math.floor(xmin); x < Math.ceil(xmax); x += 1.0) {
			for(double y = Math.floor(ymin); y < Math.ceil(ymax); y += 1.0) {
				int tid = tileid(x+0.5, y+0.5);
				IndexLong index = new IndexLong();
				
				stmt.setDouble(1, x);
				stmt.setDouble(2, y);
				stmt.setDouble(3, x+1.0);
				stmt.setDouble(4, y+1.0);
				stmt.setDouble(5, x);
				stmt.setDouble(6, y);
				stmt.setDouble(7, x+1.0);
				stmt.setDouble(8, y+1.0);
				
				rs = stmt.executeQuery();
				
				System.out.println(new Date()+" query executed for tile "+tid);
				
				if(rs.isBeforeFirst()) {
					while(rs.next()) {
						Geometry geom = ((PGgeometry)rs.getObject(1)).getGeometry();
						long geoid = Long.valueOf(rs.getString(2)).longValue();
						
						if(geom.getType() == Geometry.LINESTRING) {
							LineString line = (LineString)geom;
							Point p = line.getPoint(0);
							Point2d p1 = new Point2d(p.x, p.y);
	
							for(int k = 1; k <  line.numPoints(); k++) {
								p = line.getPoint(k);
								
								Point2d p2 = new Point2d(p.x, p.y);
	
								index.add(p1, p2, geoid);
								
								p1 = p2;
							}
						} else if(geom.getType() == Geometry.POINT) {
							System.out.println("Ignored: "+geom);
						} else {
							throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
						}
					}
					DynamicNode.reset();
					index.close();
					tile_index.put(tid, index);
					System.out.println(new Date()+" index built for tile "+tid);					
				} else {
					tile_index.put(tid, IndexLong.EMPTY);
					System.out.println("Empty tile "+tid);
				}
				rs.close();
				
				oout = new ObjectOutputStream(new FileOutputStream("ma-long-compact-"+tid+".idx"));
				oout.writeObject(index);
				oout.close();
			}
		}
		stmt.close();
		c.close();
		
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM bg15 WHERE statefp not in ('02', '15', '60', '64', '66', '68', '69', '70', '72', '74', '78', '81', '84', '86', '67', '89', '71', '76', '95', '79')");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, tzid FROM tz_world");
		
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		br.readLine();
		
		int N = 0;
		int n = 0;
		Point2d q = new Point2d(0, 0);
		ArrayList<Point2d> points = new ArrayList<Point2d>(1100000);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("IntervalTree_tmap_compact_MA.out")));
		String line;
		while((line = br.readLine()) != null) {
			String[] p = line.split(",");

			q.x = Double.parseDouble(p[0]);
			q.y = Double.parseDouble(p[1]);
			int id = Integer.parseInt(p[2]);
			
			points.add(new Point2d(q.x, q.y));
			
			int tid = tileid(q.x, q.y);
			Index index = tile_index.get(tid);
			long bg = 0;
			
			if(index != null) {
				bg = index.locate(q);
			} else {
				System.out.println("No index for tile: "+tid+", point: "+q);
			}
						
			if(bg == 0) {
				out.println(id+",");
			} else {
				n++;
				out.println(id+","+bg);
			}
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		
		br.close();
		
		N = 0;
		n = 0;
		for(Point2d q1 : points) {
			int tid = tileid(q1.x, q1.y);
			Index index = tile_index.get(tid);
			long bg = 0;
			
			if(index != null) {
				bg = index.locate(q1);
			} else {
				System.out.println("No index for tile: "+tid+", point: "+q1);
			}
				
			if(bg != 0) {
				n++;
			}

			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		out.close();
	}
}
