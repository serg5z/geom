package geom.tmap;

abstract class DecisionNode<T> implements Node<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Node<T> left;
	Node<T> right;
}