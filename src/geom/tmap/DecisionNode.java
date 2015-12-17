package geom.tmap;

abstract class DecisionNode<T> implements Node<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DecisionNode(Node<T> left, Node<T> right) {
		this.left = left;
		this.right = right;
	}
	
	public Node<T> left;
	public Node<T> right;
}