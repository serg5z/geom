package geom.tmap_long;

import java.io.Serializable;

import geom.Point2d;

class Trapezoid implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Trapezoid(Point2d left, Point2d right, DynamicSegmentLong top, DynamicSegmentLong bottom) {
		if(left.x > right.x) {
			throw new RuntimeException(left+" > "+right);
		}
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}
	
	public boolean contains(Point2d p) {
		boolean result;
		
		if((p.x == left.x) && (p.x == right.x)) {
			result = top.above(p) && bottom.below(p);
		} else {
			result = (p.x > left.x) && (p.x < right.x) && top.above(p) && bottom.below(p); 
		}
		
		return result; 
//		return  (p.x >= left.x) && (p.x < right.x) && top.above(p) && bottom.below(p); 
	}
	
	public void update(Trapezoid ul, Trapezoid ll, Trapezoid ur, Trapezoid lr) {
		updateLeft(ul, ll);
		updateRight(ur, lr);
	}
	
	public void updateLeft(Trapezoid ul, Trapezoid ll) {
	    upper_left = ul;
	    lower_left = ll;
	    
	    if(ul != null) ul.upper_right = this;
	    if(ll != null) ll.lower_right = this;
	    
	    if((upper_left == this) || (lower_left == this)) {
	    	throw new RuntimeException("Cyclic left links "+id);
	    }
	}
	
	public void updateRight(Trapezoid ur, Trapezoid lr) {
	    upper_right = ur;
	    lower_right = lr;
	    
	    if(ur != null) ur.upper_left = this;
	    if(lr != null) lr.lower_left = this;
	    
	    if((upper_left == this) || (lower_left == this)) {
	    	throw new RuntimeException("Cyclic right links "+id);
	    }
	}
	
	@Override
	public int hashCode() {
		return Double.hashCode(left.x) + Double.hashCode(right.x) + top.hashCode()+bottom.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		
		if(obj instanceof Trapezoid) {
			Trapezoid t = (Trapezoid)obj;
			
			result = (left.x == t.left.x) && (right.x == t.right.x) && top.equals(t.top) && bottom.equals(t.bottom);
		}
		
		return result;
	};
	
	@Override
	public String toString() {
		return "{"+left+" <-> "+right+" ^ "+top+" v "+bottom +" id:"+id+"}";
	}
	
	long id;
	Point2d left;
	Point2d right;
	DynamicSegmentLong top;
	DynamicSegmentLong bottom;
	transient Trapezoid upper_left;
	transient Trapezoid lower_left;
	transient Trapezoid upper_right;
	transient Trapezoid lower_right;
	transient DynamicNode node;
}