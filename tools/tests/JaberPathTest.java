import com.tarkrakov.tearsofink.compat.jaber.JaberPath;
import java.util.*;
import static com.tarkrakov.tearsofink.compat.jaber.JaberPath.*;

public class JaberPathTest {
    private static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); }
    private static Node n(int x,int z) { return new Node(x,0,z); }
    public static void main(String[] args) {
        Point a=new Point(0,0), b=new Point(60,0);
        List<Point> guide=JaberPath.guide(a,b,List.of(List.of(new Point(0,4),new Point(20,4),new Point(40,4),new Point(60,4))));
        check(guide.size()>2 && guide.getLast().equals(b),"long road detour preferred");
        check(JaberPath.guide(a,b,List.of(List.of(new Point(0,100),new Point(60,100)))).equals(List.of(b)),"far road ignored");
        Set<Node> cells=new HashSet<>();
        for(int x=0;x<=8;x++) for(int z=0;z<=2;z++) cells.add(n(x,z));
        cells.remove(n(4,0)); cells.remove(n(4,1));
        Graph graph=node->{
            List<Edge> edges=new ArrayList<>();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                Node t=n(node.x()+d[0],node.z()+d[1]);
                if(cells.contains(t))edges.add(new Edge(t,t.z()==2?1:2.4));
            }
            return edges;
        };
        Result path=JaberPath.find(n(0,0),n(8,0).point(),graph,32,900,0.1);
        check(path.reached()&&path.path().contains(n(4,2))&&path.path().stream().allMatch(cells::contains),"go around wall and prefer road");
        cells.remove(n(4,2));
        Result blocked=JaberPath.find(n(0,0),n(8,0).point(),graph,32,900,0.1);
        check(!blocked.reached()&&blocked.path().stream().noneMatch(t->t.x()>3),"missing deck never crossed");
        Result limited=JaberPath.find(n(0,0),n(8,0).point(),graph,32,2,0.1);
        check(limited.expanded()<=2,"budget");
        Result no=JaberPath.find(n(0,0),n(8,0).point(),node->List.of(),32,900,0.1);
        check(no.path().isEmpty()&&!no.reached(),"no blind motion");
        Node upper=new Node(1,2,0);
        Result stairs=JaberPath.find(n(0,0),upper.point(),node->node.equals(n(0,0))?List.of(new Edge(upper,1.5)):List.of(),32,900,0.1);
        check(stairs.reached()&&stairs.path().equals(List.of(upper)),"3D step kept");
        System.out.println("JaberPathTest OK");
    }
}
