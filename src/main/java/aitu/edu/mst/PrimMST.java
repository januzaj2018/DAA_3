package aitu.edu.mst;

import aitu.edu.visual.MSTStepListener;

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
        return run(G, null);
    }

    public static MSTResult run(EdgeWeightedGraph G, MSTStepListener listener) {
        long start = System.nanoTime();
        MSTResult res = new MSTResult();
        int V = G.V();
        boolean[] marked = new boolean[V];
        Edge[] edgeTo = new Edge[V];
        double[] distTo = new double[V];
        Arrays.fill(distTo, Double.POSITIVE_INFINITY);

        if (listener != null) listener.onInit(V, G.edges());

        IndexMinPQ<Double> pq = new IndexMinPQ<>(V);

        distTo[0] = 0.0;
        pq.insert(0, 0.0);
        res.operationsCount++;

        while (!pq.isEmpty()) {
            int v = pq.delMin();
            res.operationsCount++;
            marked[v] = true;
            if (listener != null) listener.onVisitVertex(v);
            if (edgeTo[v] != null) {
                Edge e = edgeTo[v];
                int w = e.other(v);
                if (listener != null) listener.onAcceptEdge(v, w, e.weight());
                res.edges.add(e);
                res.totalWeight += e.weight();
            }
            for (Edge e : G.adj(v)) {
                int w = e.other(v);
                if (marked[w]) continue;
                if (listener != null) listener.onConsiderEdge(v, w, e.weight());
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
                } else {
                    if (listener != null) listener.onRejectEdge(v, w, e.weight());
                }
            }
        }

        long end = System.nanoTime();
        if (listener != null) listener.onFinish();
        res.executionTimeMs = (end - start) / 1_000_000.0;
        return res;
    }
}
