package com.tarkrakov.tearsofink.world;

public final class TempleFoundationTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError("foundation regression"); }
    public static void main(String[] args) {
        check(TempleFoundation.top(66,0)==65);
        check(TempleFoundation.top(66,3)==62);
        check(TempleFoundation.distance(15,0,12,17)==3);
        check(TempleFoundation.fits(65,63,58,true,true));
        check(!TempleFoundation.fits(65,63,50,true,true));
        check(!TempleFoundation.fits(63,63,58,true,true));
        check(!TempleFoundation.fits(65,71,71,false,true));
        // Solid top from the structure, water below it, then the real bed.
        check(!TempleFoundation.stopAtBed(63,58,true));
        check(!TempleFoundation.stopAtBed(60,58,false));
        check(!TempleFoundation.stopAtBed(58,58,false));
        check(TempleFoundation.stopAtBed(58,58,true));
        check(TempleFoundation.stopAtBed(57,58,true));
        int[] ground={64,64,64},lower={64,65,64},upper={66,66,66};
        var walk=TempleApproachGrade.solve(64,64,ground,lower,upper);
        check(walk!=null && walk[1]>=65);
        lower[1]=67;
        check(TempleApproachGrade.solve(64,64,ground,lower,upper)==null);
        System.out.println("TempleFoundationTest passed");
    }
}
