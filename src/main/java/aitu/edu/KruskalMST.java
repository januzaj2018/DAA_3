package aitu.edu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KruskalMST {

    public static class MSTResult {
        public List<Edge> edges = new ArrayList<>();
        public double totalWeight = 0.0;
        public long operationsCount = 0;
        public double executionTimeMs = 0.0;
    }

    public static MSTResult run(EdgeWeightedGraph G) {
        long start = System.nanoTime();
        MSTResult res = new MSTResult();

        List<Edge> edges = new ArrayList<>();
        for (Edge e : G.edges()) edges.add(e);
        Collections.sort(edges);
        res.operationsCount += edges.size();

        UF uf = new UF(G.V());

        for (Edge e : edges) {
            int v = e.either();
            int w = e.other(v);
            res.operationsCount++;
            if (uf.find(v) != uf.find(w)) {
                uf.union(v, w);
                res.edges.add(e);
                res.totalWeight += e.weight();
                res.operationsCount++;
            }
            if (res.edges.size() == G.V() - 1) break;
        }

        long end = System.nanoTime();
        res.executionTimeMs = (end - start) / 1_000_000.0;
        return res;
    }
}
