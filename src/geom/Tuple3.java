package geom;

public class Tuple3<T1, T2, T3> {
	public Tuple3(T1 e1, T2 e2, T3 e3) {
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
	}
	
	@Override
	public int hashCode() {
		return (e1 == null ? 0 : e1.hashCode()) 
				+ (e2 == null ? 0 : e2.hashCode())
				+ (e3 == null ? 0 : e3.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		boolean result = false;

		if (o instanceof Tuple3) {
 			@SuppressWarnings("rawtypes")
			Tuple3 t = (Tuple3)o;

			result = (e1 == null ? t.e1 == null : e1.equals(t.e1)) 
					&& (e2 == null ? t.e2 == null : e2.equals(t.e2)
					&& (e3 == null ? t.e3 == null : e3.equals(t.e3)));
		}
		
		return result;
	}
	
	T1 e1;
	T2 e2;
	T3 e3;
}
