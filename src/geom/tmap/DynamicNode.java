package geom.tmap;

import java.util.HashMap;

import geom.Point2d;
import geom.Tuple2;
import geom.tmap.compact.DecisionNodeLong;
import geom.tmap.compact.IndexLong;
import geom.tmap.compact.YNodeLong;

public class DynamicNode<T> implements Node<Trapezoid<T>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DynamicNode(Point2d p, DynamicNode<T> left, DynamicNode<T> right) {
		this(new XNode<Trapezoid<T>>(p, left, right));
	}
	
	public DynamicNode(Segment s, DynamicNode<T> left, DynamicNode<T> right) {
		this(new YNode<Trapezoid<T>>(s, left, right));
	}
	
	public DynamicNode(Trapezoid<T> d) {
		this(new TNode<Trapezoid<T>>(d));
		d.node = this;
	}
	
	private DynamicNode(Node<Trapezoid<T>> nn) {
		this.nn = nn;
		n = -1;
	}
	
	@Override
	public Trapezoid<T> locate(Point2d q) {
		return nn.locate(q);
	}
	
	@Override
	public Trapezoid<T> locate(Segment q) {
		return nn.locate(q);
	}
	
	public static void reset() {
		trapezoid_index = new HashMap<Long, Integer>(100000);
		ynode_index = new HashMap<YNodeLong, Integer>(100000);		
	}
	
	public int simplify() {
		if(n == -1) {
			if(nn instanceof TNode) {
				TNode<Trapezoid<T>> t = (TNode<Trapezoid<T>>)nn;
				Long id = (Long) t.id.id;
				
				if(id == null) {
					id = -1L;
				}
				Integer v = trapezoid_index.get(id);
				if(v == null) {
					n = index.addTNode(id);
					trapezoid_index.put(id, n);
				} else {
					n = v;
				}
			} else if(nn instanceof YNode) {
				YNode<Trapezoid<T>> y = (YNode<Trapezoid<T>>)nn;
				int yn;
				int left = -1;
				int right = -1;
				Integer p = edge_index.get(new Tuple2<Integer, Integer>(point_index.get(y.s.p), point_index.get(y.s.q)));
				if(p == null) {
					p = edge_index.get(new Tuple2<Integer, Integer>(point_index.get(y.s.q), point_index.get(y.s.p)));
					right = ((DynamicNode<T>)y.left).simplify();
					if(n == -1) {
						left = ((DynamicNode<T>)y.right).simplify();
					}
				} else {
					left = ((DynamicNode<T>)y.left).simplify();
					if( n == -1) {
						right = ((DynamicNode<T>)y.right).simplify();
					}
				}
				if(n == -1) {
					YNodeLong k = new YNodeLong(p, left, right);
					
					Integer v = ynode_index.get(k);
					if(v == null) {
						yn = index.addYNode(p);
						DecisionNodeLong dn = (DecisionNodeLong) index.nodes.get(yn);
//						DecisionNodeLong dn = (DecisionNodeLong) index.node[yn];
						dn.left = left;
						dn.right = right;
						n = yn;
						ynode_index.put(k, n);
					} else {
						n = v;
					}
				}
			} else if(nn instanceof XNode) {
				XNode<Trapezoid<T>> x = (XNode<Trapezoid<T>>)nn;
				int left = ((DynamicNode<T>)x.left).simplify();
				int right = -1;
				if(n == -1) {
					right = ((DynamicNode<T>)x.right).simplify();
				}
				if(n == -1) {
					int xn = index.addXNode(point_index.get(x.p));
					DecisionNodeLong dn = (DecisionNodeLong) index.nodes.get(xn);
//					DecisionNodeLong dn = (DecisionNodeLong) index.node[xn];
					dn.left = left;
					dn.right = right;
					
					n = xn;
				}
			} else {
				throw new RuntimeException("Bad DynamicNode");
			}
		}
		nn = null;
		
		return n;
	}
	
	public int count() {
		int result = 0;
		if(!mark) {
			mark = true;
			result++;
			if(nn instanceof DecisionNode<?>) {
				DecisionNode<?> dn = (DecisionNode<?>)nn;
				result += ((DynamicNode<T>)dn.left).count();
				result += ((DynamicNode<T>)dn.right).count();
			}
		}
		
		return result;
	}
	
//	Object writeReplace() throws ObjectStreamException {
////		Object n;
////		if (nn instanceof TNode) {
////			TNode<Trapezoid<T>> t = (TNode<Trapezoid<T>>) nn;
////			n = new TNode<T>(t.id.id);
////		} else if (nn instanceof YNode) {
////			YNode<Trapezoid<T>> y = (YNode<Trapezoid<T>>) nn;
////			YNode<T> yn = new YNode<T>(y.s);
////			yn.left = (Node<T>)y.left;
////			yn.right = (Node<T>)y.right;
////			n = yn;
////		} else if (nn instanceof XNode) {
////			XNode<Trapezoid<T>> x = (XNode<Trapezoid<T>>) nn;
////			XNode<T> xn = new XNode<T>(x.p);
////			xn.left = (Node<T>)x.left;
////			xn.right = (Node<T>)x.right;
////			n = xn;
////		} else {
////			throw new RuntimeException("Bad DynamicNode");
////		}
////		
////		return n;
//		return IndexLong.writeReplace((DynamicNode<Long>)this);
//	}

	public Node<Trapezoid<T>> nn;
	public int n;
	boolean mark;
	public static HashMap<Point2d, Integer> point_index;
	public static HashMap<Tuple2<Integer, Integer>, Integer> edge_index;
	public static IndexLong index;
	static HashMap<Long, Integer> trapezoid_index = new HashMap<Long, Integer>(100000);
	static HashMap<YNodeLong, Integer> ynode_index = new HashMap<YNodeLong, Integer>(100000);
}