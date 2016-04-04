package geom.tmap_int;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import geom.Point2d;

public class Test1 {
	static <T> void write(Node n, PrintStream out, Set<Node> M) {
		if(!M.contains(n)) {
			M.add(n);
			out.println("n"+System.identityHashCode(n)+";");
			if(n instanceof XNode) {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\"];");
				if(((XNode)n).left != null) {
					out.println("edge [label=\"left\"];");
					out.println("n"+n.hashCode()+" -> n"+System.identityHashCode(((XNode)n).left)+" [label=\"left\"];");
					write(((XNode)n).left, out, M);
				}
				if(((XNode)n).right != null) {
					out.println("edge [label=\"right\"];");
					out.println("n"+n.hashCode()+" -> n"+System.identityHashCode(((XNode)n).right)+" [label=\"right\"];");
					write(((XNode)n).right, out, M);
				}
			} else if(n instanceof YNode) {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\", style=filled, color=\"gray80\"];");			
				if(((YNode)n).left != null) {
					out.println("edge [label=\"above\"];");
					out.println("n"+n.hashCode()+" -> n"+System.identityHashCode(((YNode)n).left)+" [label=\"above\"];");
					write(((YNode)n).left, out, M);
				}
				if(((YNode)n).right != null) {
					out.println("edge [label=\"below\"];");
					out.println("n"+n.hashCode()+" -> n"+System.identityHashCode(((YNode)n).right)+" [label=\"below\"];");
					write(((YNode)n).right, out, M);
				}
			} else {
				out.println("n"+n.hashCode()+" [label=\""+n.toString()+"\",shape=\"box\"];");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		IndexInt index = new IndexInt();
		index.add(new Point2d(1, 0), new Point2d(0, 1), 1);
		index.add(new Point2d(0, 1), new Point2d(2, 2), 2);
		index.add(new Point2d(5, 1), new Point2d(1, 0), 3);
		index.add(new Point2d(3, 2), new Point2d(5, 1), 4);
		index.add(new Point2d(0, 1), new Point2d(4, 1), 1);
		index.add(new Point2d(4, 1), new Point2d(0, 1), 2);
		
		index.close();
		
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("test1.idx"));
		out.writeObject(index);
		out.close();
		
		write(index.root, System.out, new HashSet<Node>());
		
		System.out.println("Done");
	}
}
