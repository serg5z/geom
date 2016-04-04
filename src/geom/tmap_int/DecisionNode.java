package geom.tmap_int;

abstract public class DecisionNode implements Node {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DecisionNode(Node left, Node right) {
		this.left = left;
		this.right = right;
	}
	
	public Node left;
	public Node right;
}