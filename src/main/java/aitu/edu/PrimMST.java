package aitu.edu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrimMST {

    public static class MSTResult {
        public List<Edge> edges = new ArrayList<>();
        public double totalWeight = 0.0;
        public long operationsCount = 0;
        public double executionTimeMs = 0.0;
    }

    public static MSTResult run(EdgeWeightedGraph G) {
        long start = System.nanoTime();
        MSTResult res = new MSTResult();
        int V = G.V();
        boolean[] marked = new boolean[V];
        Edge[] edgeTo = new Edge[V];
        double[] distTo = new double[V];
        Arrays.fill(distTo, Double.POSITIVE_INFINITY);

        IndexMinPQ<Double> pq = new IndexMinPQ<>(V);

        distTo[0] = 0.0;
        pq.insert(0, 0.0);
        res.operationsCount++;

        while (!pq.isEmpty()) {
            int v = pq.delMin();
            res.operationsCount++;
            marked[v] = true;
            for (Edge e : G.adj(v)) {
                int w = e.other(v);
                if (marked[w]) continue;
                res.operationsCount++;
                if (e.weight() < distTo[w]) {
                    distTo[w] = e.weight();
                    edgeTo[w] = e;
                    if (pq.contains(w)) {
                        pq.changeKey(w, distTo[w]);
                        res.operationsCount++;
                    } else {
                        pq.insert(w, distTo[w]);
                        res.operationsCount++;
                    }
                }
            }
        }

        for (int v = 0; v < V; v++) {
            if (edgeTo[v] != null) {
                res.edges.add(edgeTo[v]);
                res.totalWeight += edgeTo[v].weight();
            }
        }

        long end = System.nanoTime();
        res.executionTimeMs = (end - start) / 1_000_000.0;
        return res;
    }
}
