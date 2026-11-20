package com.tarkrakov.tearsofink.compat.jaber;

import java.util.*;

/** Pure two-level routing math. The local graph is supplied by the live collision sampler. */
public final class JaberPath {
    private JaberPath() {}
    public record Point(double x, double z) {
        public double distance(Point p) { return Math.hypot(x - p.x, z - p.z); }
    }
    public record Node(int x, int y2, int z) {
        public Point point() { return new Point(x + 0.5, z + 0.5); }
        public double y() { return y2 * 0.5; }
    }
    public record Edge(Node node, double cost) {}
    public interface Graph { Iterable<Edge> edges(Node node); }
    public record Result(List<Node> path, boolean reached, int expanded) {}
    private record Open(Node node, double cost, double priority) {}
    private record RoadNode(Point point, int road, int index) {}

    /** Roads are optional, disconnected lines, not a promise that their deck exists.
     * Direct links represent off-road travel; every link is subsequently checked locally. */
    public static List<Point> guide(Point start, Point goal, List<List<Point>> roads) {
        List<RoadNode> nodes = new ArrayList<>();
        nodes.add(new RoadNode(start, -1, -1));
        nodes.add(new RoadNode(goal, -2, -1));
        for (int r = 0; r < roads.size(); r++) {
            for (int i = 0; i < roads.get(r).size(); i++) nodes.add(new RoadNode(roads.get(r).get(i), r, i));
        }
        int n = nodes.size();
        double[] dist = new double[n]; Arrays.fill(dist, Double.POSITIVE_INFINITY); dist[0] = 0;
        int[] prev = new int[n]; Arrays.fill(prev, -1);
        boolean[] done = new boolean[n];
        for (int count = 0; count < n; count++) {
            int u = -1;
            for (int i = 0; i < n; i++) if (!done[i] && (u < 0 || dist[i] < dist[u])) u = i;
            if (u < 0 || !Double.isFinite(dist[u]) || u == 1) break;
            done[u] = true;
            RoadNode a = nodes.get(u);
            for (int v = 0; v < n; v++) {
                if (done[v] || v == u) continue;
                RoadNode b = nodes.get(v);
                boolean road = a.road >= 0 && a.road == b.road && Math.abs(a.index - b.index) == 1;
                double w = a.point.distance(b.point) * (road ? 1.0 : 2.6);
                if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; prev[v] = u; }
            }
        }
        List<Point> result = new ArrayList<>();
        for (int i = 1; i > 0; i = prev[i]) {
            result.add(nodes.get(i).point);
            if (prev[i] < 0) return List.of(goal);
        }
        Collections.reverse(result);
        return List.copyOf(result);
    }

    /** Radius, expansion budget and strictly improving partial paths prevent blind walking.
     * No diagonal corner cutting: the real graph exposes only validated cardinal moves. */
    public static Result find(Node start, Point goal, Graph graph, int radius, int budget, double tolerance) {
        PriorityQueue<Open> queue = new PriorityQueue<>(Comparator.comparingDouble(Open::priority)
                .thenComparingDouble(Open::cost));
        Map<Node, Double> scores = new HashMap<>();
        Map<Node, Node> parents = new HashMap<>();
        Set<Node> closed = new HashSet<>();
        scores.put(start, 0.0);
        double initial = start.point().distance(goal);
        queue.add(new Open(start, 0, initial));
        Node best = start;
        double bestH = initial;
        int expanded = 0;
        boolean reached = false;
        while (!queue.isEmpty() && expanded < budget) {
            Open open = queue.remove();
            Node node = open.node;
            if (!closed.add(node)) continue;
            expanded++;
            double h = node.point().distance(goal);
            if (h < bestH) { best = node; bestH = h; }
            if (h <= tolerance) { best = node; reached = true; break; }
            for (Edge edge : graph.edges(node)) {
                Node next = edge.node;
                if (closed.contains(next) || Math.abs(next.x - start.x) > radius || Math.abs(next.z - start.z) > radius
                        || Math.abs(next.y2 - start.y2) > 32 || !Double.isFinite(edge.cost) || edge.cost < 1) continue;
                double g = open.cost + edge.cost;
                if (g >= scores.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                scores.put(next, g); parents.put(next, node);
                queue.add(new Open(next, g, g + next.point().distance(goal)));
            }
        }
        if (best.equals(start) || (!reached && initial - bestH < 1.5)) return new Result(List.of(), reached, expanded);
        List<Node> path = new ArrayList<>();
        for (Node step = best; !step.equals(start); step = parents.get(step)) path.add(step);
        Collections.reverse(path);
        return new Result(List.copyOf(path), reached, expanded);
    }
}
