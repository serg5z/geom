package geom.tmap;

import java.io.Serializable;

import geom.Point2d;

public class Trapezoid<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Trapezoid(Point2d left, Point2d right, Segment top, Segment bottom) {
		this(null, left, right, top, bottom);
	}
	
	public Trapezoid(T id, Point2d left, Point2d right, Segment top, Segment bottom) {
		if(left.x > right.x) {
			throw new RuntimeException(left+" > "+right);
		}
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		this.node = new DynamicNode<T>(this);
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
	
	public void update(Trapezoid<T> ul, Trapezoid<T> ll, Trapezoid<T> ur, Trapezoid<T> lr) {
		updateLeft(ul, ll);
		updateRight(ur, lr);
	}
	
	public void updateLeft(Trapezoid<T> ul, Trapezoid<T> ll) {
	    upper_left = ul;
	    lower_left = ll;
	    
	    if(ul != null) ul.upper_right = this;
	    if(ll != null) ll.lower_right = this;
	    
	    if((upper_left == this) || (lower_left == this)) {
	    	throw new RuntimeException("Cyclic left links "+id);
	    }
	}
	
	public void updateRight(Trapezoid<T> ur, Trapezoid<T> lr) {
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
			@SuppressWarnings("unchecked")
			Trapezoid<T> t = (Trapezoid<T>)obj;
			
			result = (left.x == t.left.x) && (right.x == t.right.x) && top.equals(t.top) && bottom.equals(t.bottom);
		}
		
		return result;
	};
	
	@Override
	public String toString() {
		return "{"+left+" <-> "+right+" ^ "+top+" v "+bottom +" id:"+id+"}";
	}
	
	public T id;
	public Point2d left;
	public Point2d right;
	public Segment top;
	public Segment bottom;
	transient public Trapezoid<T> upper_left;
	transient public Trapezoid<T> lower_left;
	transient public Trapezoid<T> upper_right;
	transient public Trapezoid<T> lower_right;
	transient public DynamicNode<T> node;
}