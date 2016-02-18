package geom;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;

public class BuildTiledIndex {
	static long tileid(double x, double y, int level) {
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
	
	static void tile(String tzid, Geometry geom, List<Timezone>[][] tiles) {
		Envelope bbox = geom.getEnvelopeInternal();
		
		for(int x = (int)Math.floor(bbox.getMinX()); x <= Math.ceil(bbox.getMaxX()); x += 1) {
			for(int y = (int)Math.floor(bbox.getMinY()); y <= Math.ceil(bbox.getMaxY()); y += 1) {
				Envelope e = new Envelope(x-BUFFER, x+1+BUFFER, y-BUFFER, y+1+BUFFER);
				Geometry g = geom.intersection(gf.toGeometry(e));
				
				if(!g.isEmpty()) {
					int ix = x+180;
					int iy = y+90;
					if((ix < 360) && (iy < 180)) {
						if(tiles[ix][iy] == null) {
							tiles[ix][iy] = new LinkedList<Timezone>();
						}
						tiles[ix][iy].add(new Timezone(tzid, g));
					}
				}
			}				
		}
	}
		
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT tzid, ST_AsText(geom), ST_SRID(geom) FROM tz_world");
		ResultSet rs = stmt.executeQuery();
				
		WKTReader reader = new WKTReader();		
		
		@SuppressWarnings("unchecked")
		LinkedList<Timezone>[][] tiled = new LinkedList[360][180];
		while(rs.next()) {
			String tzid = rs.getString(1);
			String wkt = rs.getString(2);
			int srid = rs.getInt(3);
			
			Geometry geom = reader.read(wkt);
			geom.setSRID(srid);

			tile(tzid, geom, tiled);
		}

		rs.close();
		stmt.close();		
		
		c.close();
		
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));
		out.writeObject(tiled);
		out.close();
		
		System.out.println(new Date()+" index built.");
	}
	
	static GeometryFactory gf = new GeometryFactory();
	static double BUFFER = 0.0001;
}
