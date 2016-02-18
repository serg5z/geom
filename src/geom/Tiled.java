package geom;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Tiled {
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
		
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(args[0])));
		@SuppressWarnings("unchecked")
		LinkedList<Timezone>[][] tiled = (LinkedList<Timezone>[][])in.readObject();
		in.close();
		System.out.println(new Date()+" index read.");

		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[1]), 64000000));
		
		if(s.hasNext()) {
			s.next();
		}
		
		List<Point> points = new LinkedList<Point>();
		int N = 0;
		int n = 0;
		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			double x = ls.nextDouble();
			double y = ls.nextDouble();
			Point p = gf.createPoint(new Coordinate(x, y));
			points.add(p);
			
			int ix = (int)Math.floor(x)+180;
			int iy = (int)Math.floor(y)+90;
			
			if((ix < 360) && (iy < 180)) {
				if(tiled[ix][iy] != null) {
					for(Timezone tz : tiled[ix][iy]) {
						if(tz.geom.intersects(p)) {
							n++;
							break;
						}
					}
				}
			}
			
			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		
		System.out.println(new Date()+" Points examined: "+N+". Geometry hits: "+n+".");
		s.close();
		
		System.out.println(new Date()+" Second pass start.");
		N = 0;
		n = 0;
		for(Point p : points) {
			int ix = (int)Math.floor(p.getX())+180;
			int iy = (int)Math.floor(p.getY())+90;
			
			if((ix < 360) && (iy < 180)) {
				if(tiled[ix][iy] != null) {
					for(Timezone tz : tiled[ix][iy]) {
						if(tz.geom.intersects(p)) {
							n++;
							break;
						}
					}
				}
			}
						
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}			
		}
		System.out.println(new Date()+" Points examined: "+N+". Geometry hits: "+n+".");

		System.out.println(new Date()+" done.");
	}
	
	static GeometryFactory gf = new GeometryFactory();
	static double BUFFER = 0.0001;
}
