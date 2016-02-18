package geom;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.postgis.Geometry;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

public class IntervalTree_veb_MA {
	static Node[] index;
	static class Node implements Serializable {
		private static final long serialVersionUID = 1L;
		
		Polygon2d polygon;
		double max;
		int n;
	}
	
	static int fls(int f)
	{
	    int order;
	    for (order = 0; f != 0; f >>= 1, order++) ;

	    return order;
	}

	static int ilog2(int f)
	{
	    return fls(f) - 1;
	}
	
	static int hyperceil(int f)
	{
	    return 1 << fls(f-1);
	}
	
	static int bfs_to_veb(int bfs_number, int height)
	{
	    int split;
	    int top_height, bottom_height;
	    int depth;
	    int subtree_depth, subtree_root, num_subtrees;
	    int toptree_size, subtree_size;
	    int prior_length;

	    /* if this is a size-3 tree, bfs number is sufficient */
	    if (height <= 2)
	        return bfs_number;

	    /* depth is level of the specific node */
	    depth = ilog2(bfs_number);

	    /* the vEB layout recursively splits the tree in half */
	    split = hyperceil((height + 1) / 2);
	    bottom_height = split;
	    top_height = height - bottom_height;

	    /* node is located in top half - recurse */
	    if (depth < top_height)
	        return bfs_to_veb(bfs_number, top_height);

	    /*
	     * Each level adds another bit to the BFS number in the least
	     * position.  Thus we can find the subtree root by shifting off
	     * depth - top_height rightmost bits.
	     */
	    subtree_depth = depth - top_height;
	    subtree_root = bfs_number >> subtree_depth;

	    /*
	     * Similarly, the new bfs_number relative to subtree root has
	     * the bit pattern representing the subtree root replaced with
	     * 1 since it is the new root.  This is equivalent to
	     * bfs' = bfs / sr + bfs % sr.
	     */

	    /* mask off common bits */
	    num_subtrees = 1 << top_height;
	    bfs_number &= (1 << subtree_depth) - 1;

	    /* replace it with one */
	    bfs_number |= 1 << subtree_depth;

	    /*
	     * Now we need to count all the nodes before this one, then the
	     * position within this subtree.  The number of siblings before
	     * this subtree root in the layout is the bottom k-1 bits of the
	     * subtree root.
	     */
	    subtree_size = (1 << bottom_height) - 1;
	    toptree_size = (1 << top_height) - 1;

	    prior_length = toptree_size +
	        (subtree_root & (num_subtrees - 1)) * subtree_size;

	    return prior_length + bfs_to_veb(bfs_number, bottom_height);
	}
	
	static int toArray(RedBlackBST2<Polygon2d, Double>.Node n, int k, int h) {
		int result = 0;
		if(n != null) {
			int level = bfs_to_veb(k,  h);
			result = level;
			if(index[level] != null)
				throw new RuntimeException("entry "+level+" is occupied");
			index[level] = new Node();
			index[level].polygon = n.key;
			index[level].max = n.val;
			index[level].n = k;
			result = Math.max(result, toArray(n.left, 2*k, h));
			result = Math.max(result, toArray(n.right, 2*k+1, h));
		}
		return result;
	}
	
	static int maxDepth(RedBlackBST2<Polygon2d, Double>.Node n) {
		int result = 0;
		if(n != null) {
			int l = maxDepth(n.left);
			int r = maxDepth(n.right);
			
			result = Math.max(l,  r)+1;
		} 
		
		return result;
	}
	
	static int buildIndex(List<Polygon2d> polys) {
		polys.sort(new PolygonMinXComparator());
		RedBlackBST2<Polygon2d, Double> rbt = new RedBlackBST2<Polygon2d, Double>(new PolygonMinXComparator());
		
		for(Polygon2d p : polys) {
			rbt.put(p, p.max.x);
		}
		
		setMax(rbt.root);

		int n = 2*polys.size();
		int d = ilog2(n);
				
		index = new Node[n];
		
		int mi = toArray(rbt.root, 1, d);
		
		return d;
	}

	private static final class PolygonMinXComparator implements Comparator<Polygon2d> {
		@Override
		public int compare(Polygon2d o1, Polygon2d o2) {
			if(o1.min.x < o2.min.x)
				return -1;
			else if (o1.min.x > o2.min.x)
				return 1;
			else 
				return o1.id.compareTo(o2.id);
		}
	}

	static void add(Polygon poly, List<Polygon2d> polys, String id) {
		int n = poly.numRings();
		Point2d p0 = new Point2d(0, 0);
		Point2d[] ps = new Point2d[poly.numPoints()+(n > 1 ? 2*n : 0)];
		int j = 0;
		
		for(int i = 0; i < n; i++) {
			LinearRing r = poly.getRing(i);
			
			if(n > 1) {
				ps[j++] = p0; 
			}
			
			for(int k = 0; k < r.numPoints(); k++) {
				Point p = r.getPoint(k);
				
				ps[j] = new Point2d(p.x, p.y);
				p0.add(ps[j]);
				j++;
			}

			if(n > 1) {
				ps[j++] = p0; 
			}
		}
		
		p0.scale(1.0/j);
		
		polys.add(new Polygon2d(id, ps));
	}
	
	static void setMax(RedBlackBST2<Polygon2d, Double>.Node n) {
		if(n != null) {
			setMax(n.left);
			setMax(n.right);
			
			if((n.left != null) && (n.val < n.left.val)) {
				n.val = n.left.val;
			}
			if((n.right != null) && (n.val < n.right.val)) {
				n.val = n.right.val;
			}
		}
	}
	
	static Polygon2d query(Point2d q, int h) {
		return query(q, 1, h, 1);
	}
	
	static Polygon2d query(Point2d q, int i, int h, int d) {	
		Polygon2d result = null;
		if(i < index.length) {
			Node n = index[i];
	
			if(n != null)  {
				if(n.polygon.contains(q)) {
					result = n.polygon;
				} else {
					int left = bfs_to_veb(2*n.n, h); 
					if((2*n.n < index.length) && (index[left] != null) && (index[left].max >= q.x)) {
						result = query(q, left, h, d+1);					
					} 
					
					if((result == null) && (2*n.n+1 < index.length)){
						result = query(q, bfs_to_veb(2*n.n+1, h), h, d+1);
					}
				}
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		for(int i = 1; i < 17; i++) {
			System.out.println(i+" -> "+bfs_to_veb(i, 4));
		}
		System.out.println(new Date()+" start.");
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg");
		ResultSet rs = stmt.executeQuery();
		List<Polygon2d> polys = new ArrayList<Polygon2d>(10000);
		
		System.out.println(new Date()+" query executed.");

		while(rs.next()) {
			Geometry geom = ((PGgeometry)rs.getObject(1)).getGeometry();
			String geoid = rs.getString(2);
			
			if(geom.getType() == Geometry.POLYGON) {
				add((Polygon)geom, polys, geoid);
			} else if(geom.getType() == Geometry.MULTIPOLYGON) {
				for(Polygon p : ((MultiPolygon)geom).getPolygons()) {
					add(p, polys, geoid);
				}
			} else {
				throw new Exception("Unsupported geometry type: '"+geom.getTypeString()+"'");
			}
		}
		rs.close();
		stmt.close();
		c.close();

		int h = buildIndex(polys);
		
		System.out.println(new Date()+" index built.");

		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;
		int n = 0;
		Point2d q = new Point2d(0, 0);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("IntervalTree_veb_MA.out")));
		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			q.x = ls.nextDouble();
			q.y = ls.nextDouble();
			int id = ls.nextInt();
			
			Polygon2d p = query(q, h);
			
			if(p == null) {
				out.println(id+",");
			} else {
				n++;
				out.println(id+","+p.id);
			}
			
			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		out.close();
		
		System.out.println("Points examined: "+N+". Geometry hits: "+n+".");
		s.close();	
		
		System.out.println(new Date()+" done.");
	}
}
