package com.tarkrakov.tearsofink.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Bounded multi-source connectivity over unmodified natural/road elevations. */
final class TempleExitSearch {
    record Point(int x,int z) {}
    record Exit(int x,int y,int z,int steps) {}
    record Result(List<Exit> exits,int expanded) {}
    private record Node(Point point,int y,int steps,int score) {}
    private static final int[][] CARDINAL = {{-1,0},{0,-1},{0,1},{1,0}};

    private TempleExitSearch() {}

    /** Heights return MIN_VALUE for blocked columns. Each step changes Y by <=1.
     * Return diverse terminal exits; never traverse the outside of the corridor.
     * The heuristic ranks work only, never changes traversal legality.
     */
    static Result find(List<Point> sources,ToIntFunction<Point> height,Predicate<Point> exit,
            ToIntFunction<Point> distance,BooleanSupplier budget,int expansionLimit,int stepLimit,int exitLimit) {
        var known = new HashMap<Point,Integer>();
        var best = new HashMap<Point,Integer>();
        var open = new PriorityQueue<Node>(Comparator.comparingInt(Node::score)
                .thenComparingInt(Node::steps).thenComparingInt(n -> n.point().x())
                .thenComparingInt(n -> n.point().z()));
        for (Point p : sources) {
            if (!budget.getAsBoolean()) break;
            int y = known.computeIfAbsent(p,height::applyAsInt);
            if (y == Integer.MIN_VALUE || best.putIfAbsent(p,0)!=null) continue;
            open.add(new Node(p,y,0,distance.applyAsInt(p)));
        }
        List<Exit> result = new ArrayList<>();
        int expanded = 0;
        while (!open.isEmpty() && expanded<expansionLimit && result.size()<exitLimit && budget.getAsBoolean()) {
            Node at = open.remove();
            if (best.getOrDefault(at.point(),Integer.MAX_VALUE)!=at.steps()) continue;
            expanded++;
            if (exit.test(at.point())) {
                if (result.stream().noneMatch(e -> Math.abs(e.x()-at.point().x())+Math.abs(e.z()-at.point().z())<5))
                    result.add(new Exit(at.point().x(),at.y(),at.point().z(),at.steps()));
                continue;
            }
            if (at.steps()>=stepLimit) continue;
            for (int[] dir : CARDINAL) {
                if (!budget.getAsBoolean()) break;
                var p = new Point(at.point().x()+dir[0],at.point().z()+dir[1]);
                int steps = at.steps()+1;
                if (steps>=best.getOrDefault(p,Integer.MAX_VALUE)) continue;
                int y = known.computeIfAbsent(p,height::applyAsInt);
                if (y==Integer.MIN_VALUE || Math.abs(y-at.y())>1) continue;
                best.put(p,steps);
                open.add(new Node(p,y,steps,steps+distance.applyAsInt(p)));
            }
        }
        return new Result(List.copyOf(result),expanded);
    }
}
