package geom.tmap.compact;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;

import geom.Point2d;

public class Main {
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		ObjectOutputStream oout;
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
//		PreparedStatement stmt = c.prepareStatement("SELECT (st_dump(st_boundary(geom))).geom as geom, geoid FROM tl_2015_25_bg");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
//		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM bg15 WHERE statefp not in ('02', '15', '60', '64', '66', '68', '69', '70', '72', '74', '78', '81', '84', '86', '67', '89', '71', '76', '95', '79')");
		PreparedStatement stmt = c.prepareStatement("SELECT (st_dump(st_boundary(geom))).geom as geom, tzid FROM world_tz_clean");
		ResultSet rs = stmt.executeQuery();
		
		System.out.println(new Date()+" query executed.");
		
		IndexLong index = new IndexLong();

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
			} else {
				throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
			}
		}
		rs.close();
		stmt.close();
		c.close();
		
		index.close();
		System.out.println(new Date()+" index built. "+"Points: "+index.x.length+"; nodes: "+index.node.length);
		
		oout = new ObjectOutputStream(new FileOutputStream("ma-long-compact.idx"));
		oout.writeObject(index);
		oout.close();

		System.gc();
		Thread.currentThread();
		Thread.sleep(1000);
		System.gc();
		Thread.currentThread();
		Thread.sleep(1000);
		
		System.out.println(new Date()+" index saved.");
		
		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;
		int n = 0;
		Point2d q = new Point2d(0, 0);
		ArrayList<Point2d> points = new ArrayList<Point2d>(1100000);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("IntervalTree_tmap_compact_MA.out")));
		while(s.hasNext()) {
			String[] p = s.next().split(",");

			q.x = Double.parseDouble(p[0]);
			q.y = Double.parseDouble(p[1]);
			int id = Integer.parseInt(p[2]);
			
			points.add(new Point2d(q.x, q.y));
			
			long bg = index.locate(q);
						
			if((bg == 0) || (bg == -1)) {
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
		s.close();
		out.close();
	}
}
