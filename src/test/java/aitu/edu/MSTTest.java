package aitu.edu;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MSTTest {

    private EdgeWeightedGraph buildSampleGraph() {
        EdgeWeightedGraph G = new EdgeWeightedGraph(5);
        G.addEdge(new Edge(0,1,4)); // A-B
        G.addEdge(new Edge(0,2,3)); // A-C
        G.addEdge(new Edge(1,2,2)); // B-C
        G.addEdge(new Edge(1,3,5)); // B-D
        G.addEdge(new Edge(2,3,7)); // C-D
        G.addEdge(new Edge(2,4,8)); // C-E
        G.addEdge(new Edge(3,4,6)); // D-E
        return G;
    }

    private EdgeWeightedGraph buildDisconnectedGraph() {
        EdgeWeightedGraph G = new EdgeWeightedGraph(5);
        G.addEdge(new Edge(0,1,1));
        G.addEdge(new Edge(1,2,2));
        G.addEdge(new Edge(3,4,3));
        return G;
    }

    private Set<String> edgeSetSignature(List<Edge> edges) {
        Set<String> s = new HashSet<>();
        for (Edge e : edges) {
            int a = e.either();
            int b = e.other(a);
            int mn = Math.min(a,b);
            int mx = Math.max(a,b);
            String sig = String.format("%d-%d-%.6f", mn, mx, e.weight());
            s.add(sig);
        }
        return s;
    }

    @Test
    public void testCorrectnessOnSampleGraph() {
        EdgeWeightedGraph G = buildSampleGraph();
        int V = G.V();

        PrimMST.MSTResult prim = PrimMST.run(G);
        KruskalMST.MSTResult kruskal = KruskalMST.run(G);

        System.out.println("[Correctness] Prim totalWeight=" + prim.totalWeight + ", edges=" + prim.edges.size() + ", timeMs=" + prim.executionTimeMs + ", ops=" + prim.operationsCount);
        System.out.println("[Correctness] Kruskal totalWeight=" + kruskal.totalWeight + ", edges=" + kruskal.edges.size() + ", timeMs=" + kruskal.executionTimeMs + ", ops=" + kruskal.operationsCount);
        System.out.println("[Correctness] Prim edges: " + edgeSetSignature(prim.edges));
        System.out.println("[Correctness] Kruskal edges: " + edgeSetSignature(kruskal.edges));

        assertEquals(prim.totalWeight, kruskal.totalWeight, 1e-9, "Total MST cost should match between algorithms");


        assertEquals(V-1, prim.edges.size(), "Prim should produce V-1 edges for connected graph");
        assertEquals(V-1, kruskal.edges.size(), "Kruskal should produce V-1 edges for connected graph");

        UF ufPrim = new UF(V);
        for (Edge e : prim.edges) {
            int v = e.either();
            int w = e.other(v);
            assertNotEquals(ufPrim.find(v), ufPrim.find(w), "Prim MST should be acyclic");
            ufPrim.union(v,w);
        }

        UF ufKrus = new UF(V);
        for (Edge e : kruskal.edges) {
            int v = e.either();
            int w = e.other(v);
            assertNotEquals(ufKrus.find(v), ufKrus.find(w), "Kruskal MST should be acyclic");
            ufKrus.union(v,w);
        }

        assertEquals(1, ufPrim.count(), "Prim MST should connect all vertices");
        assertEquals(1, ufKrus.count(), "Kruskal MST should connect all vertices");
    }

    @Test
    public void testDisconnectedGraphHandledGracefully() {
        EdgeWeightedGraph G = buildDisconnectedGraph();
        int V = G.V();

        PrimMST.MSTResult prim = PrimMST.run(G);
        KruskalMST.MSTResult kruskal = KruskalMST.run(G);

        System.out.println("[Disconnected] Prim edges=" + prim.edges.size() + ", totalWeight=" + prim.totalWeight + ", timeMs=" + prim.executionTimeMs + ", ops=" + prim.operationsCount);
        System.out.println("[Disconnected] Kruskal edges=" + kruskal.edges.size() + ", totalWeight=" + kruskal.totalWeight + ", timeMs=" + kruskal.executionTimeMs + ", ops=" + kruskal.operationsCount);
        System.out.println("[Disconnected] Prim edges: " + edgeSetSignature(prim.edges));
        System.out.println("[Disconnected] Kruskal edges: " + edgeSetSignature(kruskal.edges));

        assertTrue(prim.edges.size() < V-1, "Prim should not produce V-1 edges for disconnected graph");
        assertTrue(kruskal.edges.size() < V-1, "Kruskal should not produce V-1 edges for disconnected graph");

        assertTrue(prim.operationsCount >= 0, "Prim operations count non-negative");
        assertTrue(kruskal.operationsCount >= 0, "Kruskal operations count non-negative");

        UF ufPrim = new UF(V);
        for (Edge e : prim.edges) ufPrim.union(e.either(), e.other(e.either()));
        assertTrue(ufPrim.count() > 1, "Prim result should reflect disconnected components");

        UF ufKrus = new UF(V);
        for (Edge e : kruskal.edges) ufKrus.union(e.either(), e.other(e.either()));
        assertTrue(ufKrus.count() > 1, "Kruskal result should reflect disconnected components");
    }

    @Test
    public void testPerformanceAndReproducibility() {
        EdgeWeightedGraph G = buildSampleGraph();

        PrimMST.MSTResult p1 = PrimMST.run(G);
        PrimMST.MSTResult p2 = PrimMST.run(G);

        KruskalMST.MSTResult k1 = KruskalMST.run(G);
        KruskalMST.MSTResult k2 = KruskalMST.run(G);

        System.out.println("[Perf] Prim run1 timeMs=" + p1.executionTimeMs + ", ops=" + p1.operationsCount + ", totalWeight=" + p1.totalWeight);
        System.out.println("[Perf] Prim run2 timeMs=" + p2.executionTimeMs + ", ops=" + p2.operationsCount + ", totalWeight=" + p2.totalWeight);
        System.out.println("[Perf] Kruskal run1 timeMs=" + k1.executionTimeMs + ", ops=" + k1.operationsCount + ", totalWeight=" + k1.totalWeight);
        System.out.println("[Perf] Kruskal run2 timeMs=" + k2.executionTimeMs + ", ops=" + k2.operationsCount + ", totalWeight=" + k2.totalWeight);

        assertTrue(p1.executionTimeMs >= 0.0, "Prim execution time non-negative");
        assertTrue(k1.executionTimeMs >= 0.0, "Kruskal execution time non-negative");


        assertTrue(p1.operationsCount >= 0, "Prim operations count non-negative");
        assertTrue(k1.operationsCount >= 0, "Kruskal operations count non-negative");

        assertEquals(p1.totalWeight, p2.totalWeight, 1e-9, "Prim total weight reproducible");
        assertEquals(k1.totalWeight, k2.totalWeight, 1e-9, "Kruskal total weight reproducible");

        Set<String> ps1 = edgeSetSignature(p1.edges);
        Set<String> ps2 = edgeSetSignature(p2.edges);
        assertEquals(ps1, ps2, "Prim edge set reproducible");

        Set<String> ks1 = edgeSetSignature(k1.edges);
        Set<String> ks2 = edgeSetSignature(k2.edges);
        assertEquals(ks1, ks2, "Kruskal edge set reproducible");
    }

    @Test
    public void testOperationCountsConsistency() {
        EdgeWeightedGraph G = buildSampleGraph();
        PrimMST.MSTResult prim = PrimMST.run(G);
        KruskalMST.MSTResult kruskal = KruskalMST.run(G);

        System.out.println("[OpsConsistency] Prim ops=" + prim.operationsCount + ", timeMs=" + prim.executionTimeMs + ", totalWeight=" + prim.totalWeight + ", edges=" + prim.edges.size());
        System.out.println("[OpsConsistency] Kruskal ops=" + kruskal.operationsCount + ", timeMs=" + kruskal.executionTimeMs + ", totalWeight=" + kruskal.totalWeight + ", edges=" + kruskal.edges.size());

        assertTrue(prim.operationsCount >= 0, "Prim operations count non-negative");
        assertTrue(kruskal.operationsCount >= 0, "Kruskal operations count non-negative");


        assertTrue(prim.operationsCount < 10000, "Prim operations count within expected bound for tiny graph");
        assertTrue(kruskal.operationsCount < 10000, "Kruskal operations count within expected bound for tiny graph");
    }
}
