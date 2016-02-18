package geom.tmap;

import java.io.PrintStream;

import geom.Point2d;

public class test {

	static void print(Trapezoid t, PrintStream out) {
		double at = t.top.slope();
		double ab = t.bottom.slope();
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
		out.println("))");
	}
	public static void main(String[] args) {
//		DynamicSegment s1 = new DynamicSegment("", new Point2d(-70.6926950000000005, 41.38365199999999788), 
//				new Point2d(-70.69091999999999132, 41.38084399999999619), "", "");
//		DynamicSegment s2 = new DynamicSegment("", new Point2d(-70.65094999999999459, 41.33896999999999622), 
//				new Point2d(-70.69162500000000193, 41.33220499999999475), "", "");
//
//		System.out.println(s1.below(s2));
//		System.out.println(s1.above(s2));
//		System.out.println(s2.below(s1));
//		System.out.println(s2.above(s1));
//		
//		s2 = new DynamicSegment("",  
//				new Point2d(-72.51458699999999169, 42.07619400000000098),
//				new Point2d(-72.51462999999999681, 42.07626899999999637));
//		s1 = new DynamicSegment("",  
//				new Point2d(-72.51458699999999169, 42.08696299999999724),
//				new Point2d(-72.51459400000000244, 42.08684999999999832));
//		Point2d pl = new Point2d(-72.511941, 42.084586);
//		Point2d pr = new Point2d(-72.51458699999999, 42.086963);
//		Trapezoid t1 = new Trapezoid("t1", pl, pr, s1, s2);
//		Trapezoid t2 = new Trapezoid("t2", pr, pl, s1, s2);
//		
//		print(t1, System.out);
//		print(t2, System.out);
		
		System.out.println(new Segment(new Point2d(-71.41380000000001, 42.283262), new Point2d(-71.41324399999999, 42.283513)).below(new Point2d(-71.413437, 42.309703)));
	}
}
