package geom.tmap_long;

abstract class DecisionNode implements NodeLong {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DecisionNode(NodeLong left, NodeLong right) {
		this.left = left;
		this.right = right;
	}
	
	NodeLong left;
	NodeLong right;
}