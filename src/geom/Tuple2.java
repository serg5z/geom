package geom;

public class Tuple2<T1, T2> {
	public Tuple2(T1 e1, T2 e2) {
		this.e1 = e1;
		this.e2 = e2;
	}
	
	@Override
	public int hashCode() {
		return (e1 == null ? 0 : e1.hashCode()) + (e2 == null ? 0 : e2.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		boolean result = false;

		if (o instanceof Tuple2) {
 			@SuppressWarnings("rawtypes")
			Tuple2 t = (Tuple2)o;

			result = (e1 == null ? t.e1 == null : e1.equals(t.e1)) && (e2 == null ? t.e2 == null : e2.equals(t.e2));
		}
		
		return result;
	}
	
	public T1 e1;
	public T2 e2;
}
