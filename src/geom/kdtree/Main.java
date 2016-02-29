package kdtree;

import java.util.LinkedList;

import geom.Point2d;

public class Main {
	public static void main(String[] args) {
		LinkedList<POI> POIs = new LinkedList<POI>();
		
		//(2,3), (5,4), (9,6), (4,7), (8,1), (7,2)
//		POIs.add(new POI(1, new Point2d(2, 3), 1));
//		POIs.add(new POI(2, new Point2d(5, 4), 1));
//		POIs.add(new POI(3, new Point2d(9, 6), 1));
//		POIs.add(new POI(4, new Point2d(4, 7), 3));
//		POIs.add(new POI(5, new Point2d(8, 1), 1));
//		POIs.add(new POI(6, new Point2d(7, 2), 1));
//		POIs.add(new POI(6, new Point2d(7, 3), 1));
//		venues.add(new Venue(new Point2d(7, 2), -1));
		
//		POIs.add(new POI(85L, new Point2d(1, 4), 1));
//		POIs.add(new POI(87L, new Point2d(2, 1), 1));
//		POIs.add(new POI(69L, new Point2d(3, 5), 1));
//		POIs.add(new POI(35L, new Point2d(4, 3), 1));
//		POIs.add(new POI(05L, new Point2d(0, 0), 1));
//		POIs.add(new POI(37L, new Point2d(5, 6), 1));
//		POIs.add(new POI(52L, new Point2d(6, 7), 1));
//		POIs.add(new POI(33L, new Point2d(7, 2), 1));
		
		POIs.add(new POI(3001015085L, new Point2d(-9382073.475751573, 4715385.223642889), 1));
		POIs.add(new POI(3001015087L, new Point2d(-9382040.079904335, 3560363.170658943), 1));
		POIs.add(new POI(3001015269L, new Point2d(-9380192.176357167, 4814863.842940784), 1));
		POIs.add(new POI(3001015335L, new Point2d(-9379312.752379898, 4060777.6263909563), 1));
		POIs.add(new POI(3000949005L, new Point2d(-1.0881402301398937E7, 3299726.674357184), 1));
		POIs.add(new POI(3001015337L, new Point2d(-9379268.224583581, 4836573.507436473), 1));
		POIs.add(new POI(3001015352L, new Point2d(-9379134.641194629, 4837196.548352786), 1));
		POIs.add(new POI(3001015433L, new Point2d(-9378455.59230079, 3567867.179603937), 1));
		POIs.add(new POI(3001015451L, new Point2d(-9378310.876962759, 4734686.251671088), 1));
		POIs.add(new POI(3001015458L, new Point2d(-9378266.349166442, 3986235.414932118), 1));
		POIs.add(new POI(3001015480L, new Point2d(-9378043.710184855, 4836703.908125621), 1));
		POIs.add(new POI(3001015494L, new Point2d(-9377887.862897744, 5412822.40593071), 1));
		POIs.add(new POI(3001015615L, new Point2d(-9376730.140193496, 3968697.1785151144), 1));
		POIs.add(new POI(3001015672L, new Point2d(-9376095.619095974, 5406146.83001828), 1));
		POIs.add(new POI(3001015680L, new Point2d(-9376028.827401496, 3558542.7346392353), 1));
		POIs.add(new POI(3001015708L, new Point2d(-9375839.584267149, 5503015.552472216), 1));
		
		POITree2D tree = new POITree2D(POIs);
		
		System.out.println(tree.root);
		
		for(POI p : POIs) {
			System.out.println(p + " -> " + tree.nearest(p.location()));
		}
		
//		System.out.println("nearest to (2,3)");
//		System.out.println(tree.nearest(new Point2d(2,3)));
//		System.out.println();
//		System.out.println("nearest to (4,4)");
//		System.out.println(tree.nearest(new Point2d(4,4)));
//		System.out.println();
//		System.out.println("nearest to (4,6)");
//		System.out.println(tree.nearest(new Point2d(4,6)));
//		System.out.println();
//		System.out.println("nearest to (8,4)");
//		System.out.println(tree.nearest(new Point2d(8,4)));
//		System.out.println();
//		
//		System.out.println("2 nearest to (2,3)");
//		System.out.println(tree.nearestN(new Point2d(2,3), 2));
//		System.out.println();
//		System.out.println("2 nearest to (4,4)");
//		System.out.println(tree.nearestN(new Point2d(4,4), 2));
//		System.out.println();
//		System.out.println("2 nearest to (4,6)");
//		System.out.println(tree.nearestN(new Point2d(4,6), 2));
//		System.out.println();
//		System.out.println("2 nearest to (8,4)");
//		System.out.println(tree.nearestN(new Point2d(8,4), 2));
//		System.out.println();
//		
//		System.out.println("3 nearest to (2,3)");
//		System.out.println(tree.nearestN(new Point2d(2,3), 3));
//		System.out.println();
//		System.out.println("3 nearest to (4,4)");
//		System.out.println(tree.nearestN(new Point2d(4,4), 3));
//		System.out.println();
//		System.out.println("3 nearest to (4,6)");
//		System.out.println(tree.nearestN(new Point2d(4,6), 3));
//		System.out.println();
//		System.out.println("3 nearest to (8,4)");
//		System.out.println(tree.nearestN(new Point2d(8,4), 3));
//		System.out.println();
	}
}
