package geom;

public class Edge<T> {
	public Edge(Point2d p, Point2d q) {
		this(p, q, null, null);
	}
	
	public Edge(Point2d p, Point2d q, T left, T right) {
		this.p = p;
		this.q = q;
		this.left = left;
		this.right = right;
	}

	@Override
	public int hashCode() {
		return p.hashCode() + q.hashCode() + left.hashCode() + right.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof Edge) {
			@SuppressWarnings("rawtypes")
			Edge s = (Edge)obj;

			result = p.equals(s.p) && q.equals(s.q) && left.equals(s.left) && right.equals(s.right);
		}
		
		return result;
	}

	@Override
	public String toString() {
		return "{" + p + " -> " + q + " left: " + left + "; right: " + right +"}";
	}

	public Point2d p;
	public Point2d q;
	public T left;
	public T right;
}