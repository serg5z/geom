package geom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class Geom {
	static class Timezone {
		public Timezone(String tzid, Geometry g, long tid, short level) {
			this.tzid = tzid;
			this.geom = g;
			this.tileid = tid;
			this.level = level;
		}
		
		public String tzid;
		public long tileid;
		public short level;
		public Geometry geom;
	}
	
	static long tileid(double x, double y, int level) {
		double x0 = Math.floor(x);
		double y0 = Math.floor(y);
		double d = .5;
		long l0 =  (long)((x0+180)+(y0+90)*360);
		
		x -= x0+d;
		y -= y0+d;
		while(level > 0) {
			l0 <<= 2;
			d /= 2;
			
			int q = 0;
			
			if(x > 0) {
				q = 1;
				x -= d;
			} else {
				x += d;
			}
			
			if(y > 0) {
				q += 2;
				y -= d;
			} else {
				y += d;
			}
			
			l0 += q;
			level--;
		}
		
		return l0;
	}
	
	static long tileid2(double x, double y, int level) {
		double x0 = Math.floor(x);
		double y0 = Math.floor(y);
		long l0 =  (int)((x0+180)+(y0+90)*360);
		
		x = 2*(x-x0);
		y = 2*(y-y0);
		while(level > 0) {
			l0 <<= 2;

			x0 = Math.floor(x);
			y0 = Math.floor(y);
			
			l0 += Math.floor(x0) + 2*Math.floor(y0);
			
			x = 2*(x-x0);
			y = 2*(y-y0);
			
			level--;
		}
		
		return l0;
	}
	
	static void tile(String tzid, Geometry geom, int max, List<Timezone> tiles, double tile_size, short level) {
		if(tile_size > BUFFER) {
			Envelope bbox = geom.getEnvelopeInternal();
			
			for(double x = Math.floor(bbox.getMinX()); x <= Math.ceil(bbox.getMaxX()); x += tile_size) {
				for(double y = Math.floor(bbox.getMinY()); y <= Math.ceil(bbox.getMaxY()); y += tile_size) {
					Envelope e = new Envelope(x-BUFFER, x+tile_size+BUFFER, y-BUFFER, y+tile_size+BUFFER);
					Geometry g = geom.intersection(gf.toGeometry(e));
					
					if(!g.isEmpty()) {
						if(g.getNumPoints() <= max) {
							Coordinate c = e.centre();
							Timezone tz = new Timezone(tzid, g, tileid2(c.x, c.y, level), level);
							
							tiles.add(tz);
						} else {
							tile(tzid, g, max, tiles, tile_size/2, (short)(level+1));
						}
					}
				}				
			}
		}
	}
	
	static List<Timezone> tile(String tzid, Geometry geom, int max) {
		List<Timezone> result = new LinkedList<Timezone>();
		
		tile(tzid,  geom, max, result, 1.0, (short)0);
		
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(Calendar.getInstance().getTime());
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT tzid, ST_AsText(geom), ST_SRID(geom) FROM tz_world");
//		PreparedStatement stmt = c.prepareStatement("SELECT geoid as tzid, ST_AsText(geom), ST_SRID(geom) FROM tl_2015_25_bg");
		ResultSet rs = stmt.executeQuery();
		
//		System.out.println(tileid(-179.6, -89.6, 1)+" "+tileid2(-179.6, -89.6, 1));
//		System.out.println(tileid(-179.3, -89.6, 1)+" "+tileid2(-179.3, -89.6, 1));
//		System.out.println(tileid(-179.6, -89.3, 1)+" "+tileid2(-179.6, -89.3, 1));
//		System.out.println(tileid(-179.3, -89.3, 1)+" "+tileid2(-179.3, -89.3, 1));
//		System.out.println(tileid(179.3, -89.6, 0)+" "+tileid2(179.3, -89.6, 0));
//		System.out.println(tileid(179.3, -89.6, 1)+" "+tileid2(179.3, -89.6, 1));
//		System.out.println(tileid(179.6, -89.6, 1)+" "+tileid2(179.6, -89.6, 1));
//		System.out.println(tileid(179.3, -89.3, 1)+" "+tileid2(179.3, -89.3, 1));
//		System.out.println(tileid(179.6, -89.3, 1)+" "+tileid2(179.6, -89.3, 1));
		
		WKTReader reader = new WKTReader();		
		
		LinkedList<Timezone> tiled = new LinkedList<Timezone>();
		while(rs.next()) {
			String tzid = rs.getString(1);
			String wkt = rs.getString(2);
			int srid = rs.getInt(3);
			
			Geometry geom = reader.read(wkt);
			geom.setSRID(srid);
			
			tiled.addAll(tile(tzid, geom, 50));
		}

		rs.close();
		stmt.close();		
		
		c.close();
		
		System.out.println(Calendar.getInstance().getTime());

		WKTWriter wkt_writer = new WKTWriter();
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("tz50q.txt")));
		for(Timezone tz : tiled) {
			out.println(tz.tzid+"|"+tz.tileid+"|"+tz.level+"|"+wkt_writer.write(tz.geom));
		}
		out.close();
		
		System.out.println(Calendar.getInstance().getTime());
	}
	
	static GeometryFactory gf = new GeometryFactory();
	static double BUFFER = 0.0001;
}
