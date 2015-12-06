package geom;

import java.io.Serializable;

/*
 * last point assumed to be the same as first point
 */
public class Polygon2d implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Polygon2d(String id, Point2d[] points) {
		this.id = id;
		this.points = new Point2d[points.length];
		this.min = new Point2d(points[0]);
		this.max = new Point2d(points[0]);
		for(int i = 0; i < points.length; i++) {
			this.points[i] = new Point2d(points[i].x, points[i].y);
			if(points[i].x < min.x) {
				min.x = points[i].x;
			}
			if(points[i].x > max.x) {
				max.x = points[i].x;
			}
			if(points[i].y < min.y) {
				min.y = points[i].y;
			}
			if(points[i].y > max.y) {
				max.y = points[i].y;
			}
		}
	}
	
	public Polygon2d(String id, Iterable<Point2d> points) {
		this.id = id;
		int n = 0;
		for(Point2d p : points) {
			if(n == 0) {
				this.min = new Point2d(p);
				this.max = new Point2d(p);
			}
			n++;
		}
		
		this.points = new Point2d[n];
		n = 0;
		for(Point2d p : points) {
			this.points[n] = new Point2d(p.x, p.y);
			if(p.x < min.x) {
				min.x = p.x;
			}
			if(p.x > max.x) {
				max.x = p.x;
			}
			if(p.y < min.y) {
				min.y = p.y;
			}
			if(p.y > max.y) {
				max.y = p.y;
			}
			n++;
		}
	}
	/*
int pnpoly(int nvert, float *vertx, float *verty, float testx, float testy)
{
  int i, j, c = 0;
  for (i = 0, j = nvert-1; i < nvert; j = i++) {
    if ( ((verty[i]>testy) != (verty[j]>testy)) &&
	 (testx < (vertx[j]-vertx[i]) * (testy-verty[i]) / (verty[j]-verty[i]) + vertx[i]) )
       c = !c;
  }
  return c;
}
	 */
	public boolean contains(Point2d q) {
		boolean result = !(q.x < min.x || q.x > max.x || q.y < min.y || q.y > max.y);

		if(result) {
			result = false;
			
			for (int i = 0, j = points.length-1; i < points.length; i++) {
				if ((points[i].y < q.y && points[j].y >= q.y || points[j].y < q.y && points[i].y >= q.y)
						&& (points[i].x <= q.x || points[j].x <= q.x)) {
					result ^= (points[i].x + (q.y - points[i].y) / (points[j].y - points[i].y) * (points[j].x - points[i].x) < q.x);
				}
				j = i;
			}
		}

		return result;
	}
	
	@Override
	public String toString() {
		return "Polygon: "+id+"; "+min+" - "+max+"; N: "+points.length;
	}
	
	String id;
	Point2d[] points;
	Point2d min;
	Point2d max;
}
