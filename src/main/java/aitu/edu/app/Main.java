package aitu.edu.app;

import aitu.edu.mst.core.Edge;
import aitu.edu.mst.core.EdgeWeightedGraph;
import aitu.edu.mst.KruskalMST;
import aitu.edu.mst.PrimMST;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    static class EdgeInput {
        String from;
        String to;
        double weight;
    }

    static class GraphInput {
        int id;
        List<String> nodes;
        List<EdgeInput> edges;
    }

    static class InputRoot {
        List<GraphInput> graphs;
    }

    static class IOStats {
        int vertices;
        int edges;
    }

    static class EdgeOut {
        String from;
        String to;
        double weight;

        EdgeOut(String f, String t, double w) { from = f; to = t; weight = w; }
    }

    static class AlgoOut {
        @SerializedName("mst_edges")
        List<EdgeOut> mstEdges = new ArrayList<>();
        @SerializedName("total_cost")
        double totalCost;
        @SerializedName("operations_count")
        long operationsCount;
        @SerializedName("execution_time_ms")
        double executionTimeMs;
    }

    static class GraphResult {
        @SerializedName("graph_id")
        int graphId;
        @SerializedName("input_stats")
        IOStats inputStats;
        AlgoOut prim;
        AlgoOut kruskal;
    }

    static class OutputRoot {
        List<GraphResult> results = new ArrayList<>();
    }

    public static void main(String[] args) {
        String inputPath = "src/main/resources/random_graphs_dense.json";
        String outputPath = "ass_3_output_dense.json";
        if (args.length >= 1) inputPath = args[0];
        if (args.length >= 2) outputPath = args[1];

        try (Reader reader = new FileReader(inputPath)) {
            Gson gson = new Gson();
            InputRoot root = gson.fromJson(reader, InputRoot.class);

            OutputRoot outRoot = new OutputRoot();

            for (GraphInput gIn : root.graphs) {
                Map<String, Integer> idx = new HashMap<>();
                for (int i = 0; i < gIn.nodes.size(); i++) idx.put(gIn.nodes.get(i), i);
                EdgeWeightedGraph G = new EdgeWeightedGraph(gIn.nodes.size());

                for (EdgeInput e : gIn.edges) {
                    Integer v = idx.get(e.from);
                    Integer w = idx.get(e.to);
                    if (v == null || w == null) {
                        System.err.println("Unknown node in input graph: " + e.from + " or " + e.to);
                        continue;
                    }
                    G.addEdge(new Edge(v, w, e.weight));
                }

                PrimMST.MSTResult primRes = PrimMST.run(G);

                KruskalMST.MSTResult krusRes = KruskalMST.run(G);

                GraphResult gr = new GraphResult();
                gr.graphId = gIn.id;
                IOStats stats = new IOStats();
                stats.vertices = gIn.nodes.size();
                stats.edges = gIn.edges.size();
                gr.inputStats = stats;

                gr.prim = new AlgoOut();
                gr.prim.totalCost = primRes.totalWeight;
                gr.prim.operationsCount = primRes.operationsCount;
                gr.prim.executionTimeMs = primRes.executionTimeMs;
                for (Edge e : primRes.edges) {
                    String from = gIn.nodes.get(e.either());
                    String to = gIn.nodes.get(e.other(e.either()));
                    gr.prim.mstEdges.add(new EdgeOut(from, to, e.weight()));
                }

                gr.kruskal = new AlgoOut();
                gr.kruskal.totalCost = krusRes.totalWeight;
                gr.kruskal.operationsCount = krusRes.operationsCount;
                gr.kruskal.executionTimeMs = krusRes.executionTimeMs;
                for (Edge e : krusRes.edges) {
                    String from = gIn.nodes.get(e.either());
                    String to = gIn.nodes.get(e.other(e.either()));
                    gr.kruskal.mstEdges.add(new EdgeOut(from, to, e.weight()));
                }

                outRoot.results.add(gr);
            }

            Gson pretty = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter fw = new FileWriter(outputPath)) {
                pretty.toJson(outRoot, fw);
            }

            System.out.println("MST analysis completed. Output written to: " + outputPath);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Failed to process input: " + ex.getMessage());
            System.exit(1);
        }
    }
}