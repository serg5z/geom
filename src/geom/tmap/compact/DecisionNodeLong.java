package geom.tmap.compact;

abstract public class DecisionNodeLong implements NodeLong {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DecisionNodeLong(int left, int right) {
		this.left = left;
		this.right = right;
	}
	
	public int left;
	public int right;
}