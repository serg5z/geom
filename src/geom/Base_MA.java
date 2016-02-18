package geom;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Scanner;

public class Base_MA {
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/serg", "serg", "");
		
		PreparedStatement stmt = c.prepareStatement("SELECT geom, geoid FROM tl_2015_25_bg WHERE ST_intersects(geom, ST_SetSRID(ST_Point(?, ?), 4326))");
		
		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;
		int n = 0;
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("Base_MA.out"), 64000000));
		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			double x = ls.nextDouble();
			double y = ls.nextDouble();
			int id = ls.nextInt();
			
			stmt.setDouble(1, x);
			stmt.setDouble(2, y);
			
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				n++;
				out.println(id+","+rs.getString(2));
			} else {
				out.println(id+",");
			}

			rs.close();			
			ls.close();
			
			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N+", geometry hits: "+n);				
			}
		}
		out.close();
		
		System.out.println("Points examined: "+N+". Geometry hits: "+n+".");
		s.close();		
		stmt.close();
		c.close();
		
		System.out.println(new Date()+" done.");
	}
}
