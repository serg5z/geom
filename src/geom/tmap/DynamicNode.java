package geom.tmap;

import geom.Point2d;

class DynamicNode<T> extends DecisionNode<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DynamicNode(Point2d p) {
		this(p, null, null, null, null);
	}
	
	public DynamicNode(Point2d p, DynamicNode<T> left, DynamicNode<T> right) {
		this(p, null, null, left, right);
	}
	
	public DynamicNode(DynamicSegment<T> s) {
		this(null, s, null, null, null);
	}
	
	public DynamicNode(DynamicSegment<T> s, DynamicNode<T> left, DynamicNode<T> right) {
		this(null, s, null, left, right);
	}
	
	public DynamicNode(Trapezoid<T> d) {
		this(null, null, d, null, null);
		d.node = this;
	}
	
	public DynamicNode(Point2d p, DynamicSegment<T> s, Trapezoid<T> d) {
		this(p, s, d, null, null);
	}
	
	private DynamicNode(Point2d p, DynamicSegment<T> s, Trapezoid<T> d, DynamicNode<T> left, DynamicNode<T> right) {
		this.p = p;
		this.s = s;
		this.d = d;
		this.left = left;
		this.right = right;
	}
	
	/* (non-Javadoc)
	 * @see geom.tmap.Node#locate(geom.Point2d)
	 */
	@Override
	public T locate(Point2d q) {
		return locateT(q).id;
	}
	
	public Trapezoid<T> locateT(Point2d q) {
		Trapezoid<T> result = d;
		
		if(d == null) {
			DynamicNode<T> n = (DynamicNode<T>)right;
			
			if(p != null){
				if((q.x < p.x) || ((q.x == p.x) && (q.y < p.y))) { // use point comparator
					n = (DynamicNode<T>)left;
				}
			} else if(s != null) {
				if(s.below(q)) {
					n = (DynamicNode<T>)left;
				}
			}
			
			result = n.locateT(q);
		}
		
		return result;
	}
	
	@Override
	public DynamicNode<T> step(Point2d q) {
		DynamicNode<T> result = null;
		
		if(d == null) {
			result = (DynamicNode<T>)right;
			
			if(p != null){
				if((q.x < p.x) || ((q.x == p.x) && (q.y < p.y))){
					result = (DynamicNode<T>)left;
				}
			} else if(s != null) {
				if(s.below(q)) {
					result = (DynamicNode<T>)left;
				}
			}
		}
		
		return result;
	}
	
	public Trapezoid<T> locateT(DynamicSegment<T> q) {
		Trapezoid<T> result = d;
		
		if(d == null) {
			DynamicNode<T> n = (DynamicNode<T>)right;
			
			if(p != null){
				if((q.p.x < p.x) || ((q.p.x == p.x) && (q.p.y < p.y))){
					n = (DynamicNode<T>)left;
				}
			} else if(s != null) {
				if(s.below(q)) {
					n = (DynamicNode<T>)left;
				}
			}
			
			result = n.locateT(q);
		}
		
		return result;
	}
	
	Node<T> simplify() {
		if(n == null) {
			if(d != null) {
				n = new TNode<T>(d.id);
			} else if(s != null) {
				YNode<T> yn = new YNode<T>(s.s);
				yn.left = ((DynamicNode<T>)left).simplify();
				yn.right = ((DynamicNode<T>)right).simplify();
				
				n = yn;
			} else if(p != null) {
				XNode<T> xn = new XNode<T>(p);
				xn.left = ((DynamicNode<T>)left).simplify();
				xn.right = ((DynamicNode<T>)right).simplify();
				
				n = xn;
			} else {
				throw new RuntimeException("Bad DynamicNode");
			}
		}
		
		return n;
	}
	
	Point2d p;
	DynamicSegment<T> s;
	Trapezoid<T> d;
	Node<T> n;
		
	boolean mark;
}