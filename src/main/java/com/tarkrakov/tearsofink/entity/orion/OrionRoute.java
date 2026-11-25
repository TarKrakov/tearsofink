package com.tarkrakov.tearsofink.entity.orion;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Flat arena routing for the actual boss footprint, not a vanilla mob's block-sized path. */
final class OrionRoute {
    private static final double CELL = .75;
    private static final int MAX_EXPANSIONS = 1600;
    private static final int[][] DIRECTIONS = {{1,0},{0,1},{-1,0},{0,-1},{1,1},{-1,1},{-1,-1},{1,-1}};
    private record Cell(int x, int z) {}
    private record Node(Cell cell, double cost, double remaining, Node parent) {
        double priority() { return cost + remaining; }
    }

    static List<Vec3> find(Vec3 origin, Vec3 target, Predicate<Vec3> goal, BiPredicate<Vec3,Vec3> traversable) {
        Cell start = new Cell(0, 0);
        Node root = new Node(start, 0, distance(origin, target), null);
        Map<Cell, Double> costs = new HashMap<>();
        costs.put(start, 0d);
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::priority)
                .thenComparingDouble(Node::remaining));
        open.add(root);
        Node best = root;
        int expanded = 0;
        while (!open.isEmpty() && expanded < MAX_EXPANSIONS) {
            Node node = open.remove();
            if (node.cost() > costs.getOrDefault(node.cell(), Double.POSITIVE_INFINITY)) continue;
            expanded++;
            Vec3 from = point(origin, node.cell());
            if (node.parent() != null && goal.test(from)) return path(origin, node);
            if (node.remaining() < best.remaining()) best = node;
            for (int[] dir : DIRECTIONS) {
                Cell cell = new Cell(node.cell().x() + dir[0], node.cell().z() + dir[1]);
                double cost = node.cost() + CELL * (dir[0] != 0 && dir[1] != 0 ? Math.sqrt(2) : 1);
                if (cost >= costs.getOrDefault(cell, Double.POSITIVE_INFINITY)) continue;
                Vec3 to = point(origin, cell);
                if (!traversable.test(from, to)) continue;
                costs.put(cell, cost);
                open.add(new Node(cell, cost, distance(to, target), node));
            }
        }
        return path(origin, best);
    }

    private static Vec3 point(Vec3 origin, Cell cell) {
        return origin.add(cell.x() * CELL, 0, cell.z() * CELL);
    }

    private static double distance(Vec3 a, Vec3 b) { return a.subtract(b).horizontalDistance(); }

    private static List<Vec3> path(Vec3 origin, Node node) {
        List<Vec3> result = new ArrayList<>();
        for (Node at = node; at.parent() != null; at = at.parent()) result.add(point(origin, at.cell()));
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private OrionRoute() {}
}
