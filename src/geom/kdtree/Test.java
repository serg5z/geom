package kdtree;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import com.yourkit.api.Controller;

import geom.Point2d;
import proj.Mercator;

public class Test {
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");

		LinkedList<POI> pois = new LinkedList<POI>();
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String line;
		while((line = br.readLine()) != null) {
			String[] p = line.split(",");
			double x = Double.parseDouble(p[0]);
			double y = Double.parseDouble(p[1]);
			for(int i = 2; i < p.length; i++) {
				long id = Long.parseLong(p[i]);
				pois.add(new POI(id, new Point2d(x, y), 100));
			}
		}
		br.close();
		System.out.println(new Date()+" POIs read.");
		
		POITree2D index = new POITree2D(pois);
		System.out.println(new Date()+" index built.");
		PrintStream out;
		
		Collection<POI> res = index.nearestN(new Point2d(-7925276.61780101, 5215576.73188631), 10);
/*		
		PrintStream out = new PrintStream(new FileOutputStream("poi-check.txt"));
		for(POI p : pois) {
			Collection<POI> c = index.nearestN(p.location(), 10);			
			out.print(p+" -> ");
			for(POI poi : c) {
				out.print(poi+" ");
			}
			out.println();
		}
		out.close();
		System.out.println(new Date()+" POIs check complete.");
*/		
//		Controller controller = new Controller();
//		controller.startCPUTracing(null);
		
		Mercator m = new Mercator();
		PrintStream out2 = new PrintStream(new FileOutputStream("1M-venue-test-points-fixed.txt"));
		out = new PrintStream(new FileOutputStream("poi-1M-test.txt"));
		double[] x = new double[1000001];
		double[] y = new double[1000001];
		Point2d[] points = new Point2d[1000001];
		int i = 0;
		br = new BufferedReader(new FileReader(args[1]));
		while((line = br.readLine()) != null) {
			String[] p = line.split(",");
			x[i] = Double.parseDouble(p[0]);
			y[i] = Double.parseDouble(p[1]);
			points[i] = new Point2d(x[i], y[i]);
			if(x[i] > m.mercX(180)) {
				x[i] -= m.mercX(360);
			}
			out2.println(x[i]+","+y[i]);
			Point2d location = new Point2d(x[i], y[i]);
			i++;
			
			StringBuilder sb = new StringBuilder(1024);

			Collection<POI> c = index.nearestN(location, 10);			
			sb.append(x[i]);
			sb.append(",");
			sb.append(y[i]);
			for(POI poi : c) {
				sb.append(",");
				sb.append(poi.id);
			}
			if(c.size() == 0) {
				sb.append(",none");
			}
			out.println(sb.toString());
		}
		out.close();
		out2.close();
		br.close();
		System.out.println(new Date()+" 1M done.");

//		Controller controller = new Controller();
//		controller.startCPUTracing(null);
		out = new PrintStream(new FileOutputStream("poi-1M-postgis-test.txt"));
		for(i = 0; i < 1000000; i++) {
			StringBuilder sb = new StringBuilder(1024);
			Collection<POI> c = index.nearestN(points[i], 10);			
			sb.append(x[i]);
			sb.append(",");
			sb.append(y[i]);
			for(POI poi : c) {
				sb.append(",");
				sb.append(poi.id);
			}
			if(c.size() == 0) {
				sb.append(",none");
			}
			out.println(sb.toString());
		}
		out.close();

		System.out.println(new Date()+" done.");
//		controller.stopCPUProfiling();
	}
}
