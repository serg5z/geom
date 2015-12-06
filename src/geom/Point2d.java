package geom;

import java.io.Serializable;

public class Point2d implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Point2d(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Point2d(Point2d p) {
		this.x = p.x;
		this.y = p.y;
	}
	
	public Point2d add(Point2d p) {
		x += p.x;
		y += p.y;
		
		return this;
	}
	
	public Point2d scale(double f) {
		x *= f;
		y *= f;
		
		return this;
	}
	
	@Override
	public String toString() {
		return "("+x+", "+y+")";
	}
	
	@Override
	public int hashCode() {
		return Double.hashCode(x) + Double.hashCode(y);
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		
		if(obj instanceof Point2d) {
			Point2d p = (Point2d)obj;
			
			result = (p.x == x) && (p.y == y);
		}
		
		return result;
	}

	public double x;
	public double y;
}
