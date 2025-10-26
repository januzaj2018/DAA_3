package aitu.edu.visual;

import aitu.edu.mst.Edge;
import aitu.edu.mst.EdgeWeightedGraph;
import aitu.edu.mst.KruskalMST;
import aitu.edu.mst.PrimMST;
import com.google.gson.Gson;
import guru.nidi.graphviz.engine.Format;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.*;
import java.util.*;

public class MSTAnimationDemo {
    static class InputFile {
        List<GraphDef> graphs;
    }

    static class GraphDef {
        int id;
        List<String> nodes;
        List<EdgeDef> edges;
    }

    static class EdgeDef {
        String from;
        String to;
        double weight;
    }

    public static void main(String[] args) throws Exception {
        String inputGlob = args.length > 0 ? args[0] : "src/main/java/aitu/edu/ass_3_input.json";
        String outRoot = args.length > 1 ? args[1] : "target/ass3_output";
        String formatArg = args.length > 2 ? args[2] : "svg";
        int width = args.length > 3 ? Integer.parseInt(args[3]) : 1200;

        Format format = "png".equalsIgnoreCase(formatArg) ? Format.PNG : Format.SVG;

        // Resolve directory and glob pattern (DirectoryStream requires dir + glob)
        Path globPath = Paths.get(inputGlob);
        Path dir = globPath.getParent() == null ? Paths.get(".") : globPath.getParent();
        String glob = globPath.getFileName().toString();

        System.out.println("Searching for input files in: " + dir.toAbsolutePath() + " matching: " + glob);

        Gson gson = new Gson();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            boolean foundAny = false;
            for (Path p : stream) {
                foundAny = true;
                File inputFile = p.toFile();
                System.out.println("Processing input file: " + inputFile.getPath());
                String baseName = inputFile.getName().replaceAll("\\.json$", "");

                // parse JSON
                InputFile in;
                try (Reader r = new FileReader(inputFile)) {
                    in = gson.fromJson(r, InputFile.class);
                }
                if (in == null || in.graphs == null || in.graphs.isEmpty()) {
                    System.out.println("  No graphs found in file: " + inputFile.getPath());
                    continue;
                }

                for (GraphDef g : in.graphs) {
                    System.out.println("  Graph id=" + g.id + " ...");
                    // build mapping for node labels -> int indices
                    Map<String, Integer> idMap = new LinkedHashMap<>();
                    if (g.nodes != null) {
                        for (String n : g.nodes) {
                            if (n == null) continue;
                            idMap.putIfAbsent(n, idMap.size());
                        }
                    }
                    if (g.edges != null) {
                        for (EdgeDef ed : g.edges) {
                            if (ed == null) continue;
                            if (ed.from != null) idMap.putIfAbsent(ed.from, idMap.size());
                            if (ed.to != null) idMap.putIfAbsent(ed.to, idMap.size());
                        }
                    }

                    int V = Math.max(0, idMap.size());
                    if (V == 0) {
                        System.out.println("    Graph has no nodes; skipping.");
                        continue;
                    }

                    EdgeWeightedGraph graph = new EdgeWeightedGraph(V);
                    if (g.edges != null) {
                        for (EdgeDef ed : g.edges) {
                            if (ed == null || ed.from == null || ed.to == null) continue;
                            Integer a = idMap.get(ed.from);
                            Integer b = idMap.get(ed.to);
                            if (a == null || b == null) continue;
                            graph.addEdge(new Edge(a, b, ed.weight));
                        }
                    }

                    // Prepare output directories per input file and graph id
                    Path graphOutBase = Paths.get(outRoot).resolve(baseName).resolve("graph-" + g.id);
                    Path primFrames = graphOutBase.resolve("prim_frames");
                    Path kruskalFrames = graphOutBase.resolve("kruskal_frames");
                    Files.createDirectories(primFrames);
                    Files.createDirectories(kruskalFrames);

                    // Run Prim
                    System.out.println("    Running Prim and exporting frames to: " + primFrames.toAbsolutePath());
                    try (GraphvizAnimator animator = new GraphvizAnimator(primFrames.toString(), format, width, 1)) {
                        PrimMST.MSTResult primRes = PrimMST.run(graph, animator);
                        System.out.println("      Prim total weight = " + primRes.totalWeight);
                    } catch (Throwable t) {
                        System.out.println("      Prim failed: " + t.getMessage());
                    }

                    // Run Kruskal
                    System.out.println("    Running Kruskal and exporting frames to: " + kruskalFrames.toAbsolutePath());
                    try (GraphvizAnimator animator = new GraphvizAnimator(kruskalFrames.toString(), format, width, 1)) {
                        KruskalMST.MSTResult krRes = KruskalMST.run(graph, animator);
                        System.out.println("      Kruskal total weight = " + krRes.totalWeight);
                    } catch (Throwable t) {
                        System.out.println("      Kruskal failed: " + t.getMessage());
                    }

                    System.out.println("    Done graph id=" + g.id + "\n");
                }
            }
            if (!foundAny) {
                System.out.println("No input files found for pattern: " + inputGlob);
            }
        }

        System.out.println("All input processing complete. Outputs written under: " + Paths.get(outRoot).toAbsolutePath());
    }
}
