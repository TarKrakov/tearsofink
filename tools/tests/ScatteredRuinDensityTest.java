import com.tarkrakov.tearsofink.world.ruins.ScatteredRuinDensity;

public final class ScatteredRuinDensityTest {
    public static void main(String[] args) {
        for (long seed : new long[]{0,42,777,-1234567,Long.MAX_VALUE}) {
            int kept=0, total=0, changed=0;
            for (int x=-140; x<=140; x+=5) for (int z=-140; z<=140; z+=5) {
                boolean a=ScatteredRuinDensity.keep(seed,x,z);
                if (a != ScatteredRuinDensity.keep(seed,x,z)) throw new AssertionError("unstable");
                if (a) kept++;
                if (a != ScatteredRuinDensity.keep(seed+1,x,z)) changed++;
                total++;
            }
            double ratio=kept/(double)total, diff=changed/(double)total;
            if (ratio<.34 || ratio>.41 || diff<.43 || diff>.51) throw new AssertionError(seed+": "+ratio+", "+diff);
            System.out.println(seed+": kept "+kept+"/"+total+" ("+ratio+")");
        }
    }
}
