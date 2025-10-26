package aitu.edu.mst;

import aitu.edu.mst.core.Edge;
import aitu.edu.mst.core.EdgeWeightedGraph;
import aitu.edu.mst.core.UF;
import aitu.edu.visual.MSTStepListener;

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
        return run(G, null);
    }

    public static MSTResult run(EdgeWeightedGraph G, MSTStepListener listener) {
        long start = System.nanoTime();
        MSTResult res = new MSTResult();

        if (listener != null) listener.onInit(G.V(), G.edges());

        List<Edge> edges = new ArrayList<>();
        for (Edge e : G.edges()) edges.add(e);
        Collections.sort(edges);
        res.operationsCount += edges.size();

        UF uf = new UF(G.V());

        for (Edge e : edges) {
            int v = e.either();
            int w = e.other(v);
            if (listener != null) listener.onConsiderEdge(v, w, e.weight());
            res.operationsCount++;
            if (uf.find(v) != uf.find(w)) {
                uf.union(v, w);
                res.edges.add(e);
                res.totalWeight += e.weight();
                if (listener != null) listener.onAcceptEdge(v, w, e.weight());
                res.operationsCount++;
            } else {
                if (listener != null) listener.onRejectEdge(v, w, e.weight());
            }
            if (res.edges.size() == G.V() - 1) break;
        }

        long end = System.nanoTime();
        if (listener != null) listener.onFinish();
        res.executionTimeMs = (end - start) / 1_000_000.0;
        return res;
    }
}
