package geom.tmap_long;

import geom.Point2d;
import geom.tmap.Node;
import geom.tmap.Segment;
import geom.tmap.TNode;
import geom.tmap.XNode;
import geom.tmap.YNode;

class DynamicNode implements Node<Trapezoid> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DynamicNode(Point2d p, DynamicNode left, DynamicNode right) {
		this(new XNode<Trapezoid>(p, left, right));
	}
	
	public DynamicNode(DynamicSegmentLong s, DynamicNode left, DynamicNode right) {
		this(new YNode<Trapezoid>(s, left, right));
	}
	
	public DynamicNode(Trapezoid d) {
		this(new TNode<Trapezoid>(d));
		d.node = this;
	}
	
	private DynamicNode(Node<Trapezoid> nn) {
		this.nn = nn;
	}
	
	@Override
	public Trapezoid locate(Point2d q) {
		return nn.locate(q);
	}
	
	@Override
	public Trapezoid locate(Segment q) {
		return nn.locate(q);
	}
	
	NodeLong simplify() {
		if(n == null) {
			if(nn instanceof TNode) {
				TNode<Trapezoid> t = (TNode<Trapezoid>)nn;
				n = new TNodeLong(t.id.id);
			} else if(nn instanceof YNode) {
				YNode<Trapezoid> y = (YNode<Trapezoid>)nn;
				YNodeLong yn = new YNodeLong(((DynamicSegmentLong)y.s).s);
				yn.left = ((DynamicNode)y.left).simplify();
				yn.right = ((DynamicNode)y.right).simplify();
				
				n = yn;
			} else if(nn instanceof XNode) {
				XNode<Trapezoid> x = (XNode<Trapezoid>)nn;
				XNodeLong xn = new XNodeLong(x.p);
				xn.left = ((DynamicNode)x.left).simplify();
				xn.right = ((DynamicNode)x.right).simplify();
				
				n = xn;
			} else {
				throw new RuntimeException("Bad DynamicNode: "+nn.getClass());
			}
		}
		
		return n;
	}

	Node<Trapezoid> nn;
	NodeLong n;
		
	boolean mark;
}