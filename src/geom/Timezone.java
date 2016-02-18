package geom;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Geometry;

class Timezone implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Timezone(String tzid, Geometry g) {
		this.tzid = tzid;
		this.geom = g;
	}
	
	public String tzid;
	public Geometry geom;
}