import com.tarkrakov.tearsofink.entity.MorokDanceOrbit;
public class MorokDanceOrbitTest {
    static double dist(MorokDanceOrbit.Offset a,MorokDanceOrbit.Offset b){
        return Math.sqrt(Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2));
    }
    static void check(boolean ok){if(!ok)throw new AssertionError("Morok dance geometry");}
    public static void main(String[] args){
        for(int size:new int[]{3,4})for(int direction:new int[]{-1,1})for(int t=0;t<MorokDanceOrbit.PERIOD;t++){
            for(int i=0;i<size;i++){
                var a=MorokDanceOrbit.at(i,size,.73,direction,t);
                var b=MorokDanceOrbit.at(i,size,.73,direction,t+1);
                check(Math.abs(Math.hypot(a.x(),a.z())-3)<1e-8);
                check(dist(a,b)<.1);
                check(dist(a,MorokDanceOrbit.at(i,size,.73,direction,t+MorokDanceOrbit.PERIOD))<1e-8);
                for(int j=i+1;j<size;j++)check(dist(a,MorokDanceOrbit.at(j,size,.73,direction,t))>4);
            }
        }
        System.out.println("Morok dance orbit spacing, period and speed OK");
    }
}
