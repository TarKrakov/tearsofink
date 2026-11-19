package com.tarkrakov.tearsofink.world;

import java.util.List;
import java.util.function.ToIntFunction;
import static com.tarkrakov.tearsofink.world.TempleExitSearch.Point;

public final class TempleExitSearchTest {
    private static void require(boolean condition,String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // Two lane sources. A wet barrier blocks the direct route; its only
        // winding dry gap is near the second source.
        ToIntFunction<Point> h = p -> p.x()<0 || p.x()>12 || p.z()<0 || p.z()>8
                || p.z()==3 && p.x()!=10 ? Integer.MIN_VALUE : 64;
        var sources = List.of(new Point(0,0),new Point(10,0));
        var a = TempleExitSearch.find(sources,h,p -> p.z()==8,
                p -> p.z()>8?0:8-p.z()+Math.abs(p.x()-2),()->true,400,20,8);
        require(!a.exits().isEmpty(),"multi-source winding exit missing");
        require(a.expanded()<=400,"expansion bound");
        var b = TempleExitSearch.find(sources.reversed(),h,p -> p.z()==8,
                p -> 8-p.z()+Math.abs(p.x()-2),()->true,400,20,8);
        require(a.equals(b),"source order changed result");
        var cliff = TempleExitSearch.find(List.of(new Point(0,0)),
                p -> p.x()!=0 || p.z()<0 || p.z()>8 ? Integer.MIN_VALUE : p.z()<3?64:66,
                p -> p.z()==8,p -> 8-p.z(),()->true,100,20,8);
        require(cliff.exits().isEmpty(),"two-block cliff accepted");
        var shortWalk = TempleExitSearch.find(sources,h,p -> p.z()==8,
                p -> 8-p.z(),()->true,400,7,8);
        require(shortWalk.exits().isEmpty(),"step bound ignored");
        var stopped = TempleExitSearch.find(sources,h,p -> p.z()==8,
                p -> 8-p.z(),()->false,400,20,8);
        require(stopped.expanded()==0 && stopped.exits().isEmpty(),"budget ignored");
        System.out.println("TempleExitSearchTest passed");
    }
}
