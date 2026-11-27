import com.tarkrakov.tearsofink.entity.TrollTerritoryRules;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;
public final class TrollTerritoryRulesTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        check(separated(0,0,150,0));check(!separated(0,0,149,0));
        check(separated(-90,-120,0,0));check(!separated(-90,-119,0,0));
        check(within(0,0,25,0,ROAM));check(!within(0,0,25.01,0,ROAM));
        check(DETECT==18&&DEFEND==35&&CHASE==45&&ROAM==25);
        int families=0;for(int i=0;i<100;i++)if(size(i)==2)families++;
        check(families==20);check(REPLENISH==36000);
        System.out.println("Troll territory geometry, size and cooldown OK");
    }
}
