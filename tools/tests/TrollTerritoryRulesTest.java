package com.tarkrakov.tearsofink.entity;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;
public final class TrollTerritoryRulesTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        check(MIN_GAP==75);
        check(separated(0,0,75,0));check(!separated(0,0,74,0));
        check(separated(-45,-60,0,0));check(!separated(-45,-59,0,0));
        check(CLEARING_RADIUS==25&&CLEARING_MAX_CUT==32&&CLEARING_MAX_ADDED==12);
        check(clearingChance(14*14)==100&&clearingChance(15*15)==85&&clearingChance(21*21)==55);
        check(within(0,0,25,0,ROAM));check(!within(0,0,25.01,0,ROAM));
        check(DETECT==18&&DEFEND==35&&CHASE==45&&ROAM==25);
        int families=0;for(int i=0;i<100;i++)if(size(i)==2)families++;
        check(families==20);check(REPLENISH==36000);
        check(withinStrike(0, 2.7, 4.1, 0, 1, 4.1, 2.7, false));
        check(!withinStrike(0, 2.71, 4.1, 0, 1, 4.1, 2.7, false));
        check(!withinStrike(0, 0, 4.11, 0, 1, 4.1, 2.7, false));
        check(!withinStrike(4, 0, 0, 0, 1, 4.1, 2.7, false));
        check(!withinStrike(0, 0, -4, 0, 1, 4.1, 2.7, false));
        check(withinStrike(0, 0, -5, 0, 1, 5, 2.7, true));
        check(!withinStrike(0, 0, -5.01, 0, 1, 5, 2.7, true));
        check(SEARCH_INTERVAL==60&&SEARCH_COLUMNS==8);
        check(MOUTH_MIN>=SPAWN_MIN+6&&MOUTH_MAX<=SPAWN_MAX-6);
        check(overlapsProtectedColumn(13,0,0,0,10,10));
        check(!overlapsProtectedColumn(14,0,0,0,10,10));
        check(overlapsProtectedColumn(-3,-3,0,0,10,10));
        check(!overlapsProtectedColumn(-4,-4,0,0,10,10));
        check(PLAYER_PURSUIT_SPEED==2&&LEAP_RANGE==10&&LEAP_RISE==8);
        check(!needsLeap(9,99)&&needsLeap(9.01,0)&&needsLeap(0,100));
        check(leapHorizontal(0)==0&&leapHorizontal(1)==1);
        check(leapHeight(0,8)==0&&leapHeight(.42,8)==8.5&&leapHeight(1,8)==8);
        check(leapHeight(1,0)==0);
        System.out.println("Troll territory, family, search, leap and strike geometry OK");
    }
}
