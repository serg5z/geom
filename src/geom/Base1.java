package geom;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Scanner;

public class Base1 {
	public static void main(String[] args) throws Exception {
		System.out.println(new Date()+" start.");
		Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(args[0])));
		
		if(s.hasNext()) {
			s.next();
		}
		
		int N = 0;

		while(s.hasNext()) {
			Scanner ls = new Scanner(s.next());
			ls.useDelimiter(",");
			
			ls.nextDouble();
			ls.nextDouble();
			
			ls.close();

			N++;
			
			if(N%10000 == 0) {
				System.out.println(new Date()+" points examined: "+N);				
			}
		}
		
		s.close();
		System.out.println(new Date()+" done.");
	}
}
