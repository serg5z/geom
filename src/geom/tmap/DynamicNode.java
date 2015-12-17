package geom.tmap;

import geom.Point2d;

class DynamicNode<T> implements Node<Trapezoid<T>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DynamicNode(Point2d p, DynamicNode<T> left, DynamicNode<T> right) {
		this(new XNode<Trapezoid<T>>(p, left, right));
	}
	
	public DynamicNode(DynamicSegment<T> s, DynamicNode<T> left, DynamicNode<T> right) {
		this(new YNode<Trapezoid<T>>(s, left, right));
	}
	
	public DynamicNode(Trapezoid<T> d) {
		this(new TNode<Trapezoid<T>>(d));
		d.node = this;
	}
	
	private DynamicNode(Node<Trapezoid<T>> nn) {
		this.nn = nn;
	}
	
	@Override
	public Trapezoid<T> locate(Point2d q) {
		return nn.locate(q);
	}
	
	@Override
	public Trapezoid<T> locate(Segment q) {
		return nn.locate(q);
	}
	
	Node<T> simplify() {
		if(n == null) {
			if(nn instanceof TNode) {
				TNode<Trapezoid<T>> t = (TNode<Trapezoid<T>>)nn;
				n = new TNode<T>(t.id.id);
			} else if(nn instanceof YNode) {
				YNode<Trapezoid<T>> y = (YNode<Trapezoid<T>>)nn;
				YNode<T> yn = new YNode<T>(((DynamicSegment<T>)y.s).s);
				yn.left = ((DynamicNode<T>)y.left).simplify();
				yn.right = ((DynamicNode<T>)y.right).simplify();
				
				n = yn;
			} else if(nn instanceof XNode) {
				XNode<Trapezoid<T>> x = (XNode<Trapezoid<T>>)nn;
				XNode<T> xn = new XNode<T>(x.p);
				xn.left = ((DynamicNode<T>)x.left).simplify();
				xn.right = ((DynamicNode<T>)x.right).simplify();
				
				n = xn;
			} else {
				throw new RuntimeException("Bad DynamicNode");
			}
		}
		
		return n;
	}

	Node<Trapezoid<T>> nn;
	Node<T> n;
		
	boolean mark;
}