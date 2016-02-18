package geom;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Scanner;

public class Base2 {
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT tzid FROM tz_world WHERE ST_intersects(geom, ST_SetSRID(ST_Point(?, ?), 4326))");
		
		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;
		int n = 0;
		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			double x = ls.nextDouble();
			double y = ls.nextDouble();
			
			stmt.setDouble(1, x);
			stmt.setDouble(2, y);
			
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				n++;
			}
			
			rs.close();			
			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		
		System.out.println("Points examined: "+N+". Geometry hits: "+n+".");
		s.close();		
		stmt.close();
		c.close();
		
		System.out.println(new Date()+" done.");
	}
}
