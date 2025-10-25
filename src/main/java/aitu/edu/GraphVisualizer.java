package aitu.edu;

import com.google.gson.Gson;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.Shape;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static guru.nidi.graphviz.model.Factory.*;

public class GraphVisualizer {

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
        String inputPath = args.length > 0 ? args[0] : "src/main/java/aitu/edu/random_graphs.json";
        String outDirPath = args.length > 1 ? args[1] : "target/graphs";
        String formatArg = args.length > 2 ? args[2] : "svg"; // svg or png
        int width = args.length > 3 ? Integer.parseInt(args[3]) : 3000;
        int maxNodesToRender = args.length > 4 ? Integer.parseInt(args[4]) : 100; // skip rendering if graph bigger
        boolean omitLabels = args.length > 5 && Boolean.parseBoolean(args[5]); // don't render per-edge weight labels

        File outDir = new File(outDirPath);
        Files.createDirectories(outDir.toPath());
        if (isDotAvailable()) {
            try {
                Graphviz.useEngine(new GraphvizCmdLineEngine());
                System.out.println("Using native Graphviz 'dot' command-line engine for rendering.");
            } catch (Throwable t) {
                System.out.println("Native Graphviz engine detection succeeded but failed to initialize: " + t.getMessage());
            }
        } else {
            System.out.println("Native Graphviz 'dot' not found on PATH. Falling back to default graphviz-java engine.\n" +
                    "For large graphs (>100 nodes) consider installing Graphviz and ensuring 'dot' is on PATH to avoid wasm memory issues.");
        }

        Format format = "png".equalsIgnoreCase(formatArg) ? Format.PNG : Format.SVG;

        Gson gson = new Gson();
        try (Reader r = new FileReader(inputPath)) {
            InputFile input = gson.fromJson(r, InputFile.class);
            if (input == null || input.graphs == null) {
                System.out.println("No graphs found in input JSON: " + inputPath);
                return;
            }
            System.out.println("Found " + input.graphs.size() + " graph(s) in input.");
            for (GraphDef g : input.graphs) {
                System.out.println("\nProcessing graph id=" + g.id + " ...");

                int declaredNodes = g.nodes == null ? 0 : g.nodes.size();
                int declaredEdges = g.edges == null ? 0 : g.edges.size();
                System.out.println("Declared nodes: " + declaredNodes + ", declared edges: " + declaredEdges);
                MutableGraph mg = mutGraph("graph" + g.id).setDirected(false);

                // Global styling aimed at better readability on medium/large graphs
                boolean isLarge = declaredNodes >= 80 || declaredEdges >= 150;
                boolean hideNodeLabels = (args.length > 5 && Boolean.parseBoolean(args[5])) || isLarge; // auto-hide labels when graph is large

                // Graph-level attributes
                mg.graphAttrs().add(
                        Attributes.attr("bgcolor", "white"),
                        Attributes.attr("splines", "true"),          // smoother edges
                        Attributes.attr("overlap", "false"),         // try to reduce overlaps (mainly for neato/sfdp)
                        Attributes.attr("outputorder", "edgesfirst"),
                        Attributes.attr("nodesep", isLarge ? "0.1" : "0.2"),
                        Attributes.attr("ranksep", isLarge ? "0.2" : "0.4")
                );
                // Default node attributes
                if (isLarge) {
                    mg.nodeAttrs().add(
                            Shape.POINT,
                            Attributes.attr("width", "0.05"),         // tiny points for large graphs
                            Attributes.attr("label", ""),
                            Attributes.attr("color", "#9ECAE1")
                    );
                } else {
                    mg.nodeAttrs().add(
                            Shape.CIRCLE,
                            Style.FILLED,
                            Attributes.attr("color", "#9ECAE1"),
                            Attributes.attr("fontname", "Helvetica"),
                            Attributes.attr("fontsize", "10")
                    );
                }
                // Default edge attributes
                mg.linkAttrs().add(
                        Attributes.attr("color", "#BBBBBB"),
                        Attributes.attr("penwidth", isLarge ? "0.8" : "1.2")
                );

                Map<String, MutableNode> nodeMap = new HashMap<>();

                if (g.nodes != null) {
                    for (String n : g.nodes) {
                        if (n == null) continue;
                        MutableNode mn = mutNode(safeLabel(n));
                        if (!hideNodeLabels) {
                            mn = mn.add(Label.of(truncateLabel(n)));
                        }
                        mn = mn.add(Style.FILLED).add(Color.LIGHTBLUE);
                        nodeMap.put(n, mn);
                        mg.add(mn);
                    }
                }

                Set<String> seen = new HashSet<>();
                if (g.edges != null) {
                    int edgeCount = 0;
                    for (EdgeDef e : g.edges) {
                        String a = e.from;
                        String b = e.to;
                        if (a == null || b == null) continue;
                        String key = a.compareTo(b) <= 0 ? a + "||" + b : b + "||" + a;
                        if (!seen.add(key)) continue; // skip duplicates

                        MutableNode na = nodeMap.computeIfAbsent(a, k -> {
                            MutableNode mn = mutNode(safeLabel(k));
                            if (!hideNodeLabels) {
                                mn = mn.add(Label.of(truncateLabel(k)));
                            }
                            mn = mn.add(Style.FILLED).add(Color.LIGHTBLUE);
                            mg.add(mn);
                            return mn;
                        });
                        MutableNode nb = nodeMap.computeIfAbsent(b, k -> {
                            MutableNode mn = mutNode(safeLabel(k));
                            if (!hideNodeLabels) {
                                mn = mn.add(Label.of(truncateLabel(k)));
                            }
                            mn = mn.add(Style.FILLED).add(Color.LIGHTBLUE);
                            mg.add(mn);
                            return mn;
                        });
                        // show per-edge weight labels for small graphs when not explicitly omitted
                        if (!omitLabels && !isLarge) {
                            try {
                                na.addLink(to(nb).with(Label.of(String.format("%.2f", e.weight))));
                            } catch (Exception ex) {
                                // fallback to unlabeled link if something goes wrong
                                na.addLink(to(nb));
                            }
                        } else {
                            na.addLink(to(nb));
                        }


                        edgeCount++;
                        if (edgeCount % 10000 == 0) {
                            System.out.println("  Added " + edgeCount + " edges so far...");
                        }
                    }
                    System.out.println("Added unique edges: " + seen.size());
                }

                int totalNodesInGraph = nodeMap.size();
                File svgOut = new File(outDir, "graph-" + g.id + ".svg");
                File pngOut = new File(outDir, "graph-" + g.id + ".png");
                File summaryOut = new File(outDir, "graph-" + g.id + ".txt");

                if (totalNodesInGraph > maxNodesToRender) {
                    System.out.println("Graph id=" + g.id + " has " + totalNodesInGraph + " nodes which exceeds maxNodesToRender=" + maxNodesToRender + ". Skipping image render to avoid OOM. Writing summary: " + summaryOut.getPath());
                    try {
                        String summary = "Graph id=" + g.id + " summary\n" +
                                "declaredNodes=" + declaredNodes + "\n" +
                                "declaredEdges=" + declaredEdges + "\n" +
                                "uniqueNodesPresent=" + totalNodesInGraph + "\n" +
                                "uniqueEdgesPresent=" + (g.edges == null ? 0 : new HashSet<>(seen).size()) + "\n" +
                                "omitLabels=" + omitLabels + "\n";
                        Files.writeString(summaryOut.toPath(), summary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException io) {
                        System.out.println("Failed to write summary for graph id=" + g.id + ": " + io.getMessage());
                    }
                    continue;
                }

                try {
                    Engine engineChoice = isLarge ? Engine.FDP : Engine.NEATO; // FDP/NEATO improve readability; SFDP may be unavailable in this version
                    System.out.println("Rendering graph id=" + g.id + " with layout=" + engineChoice + " to " + format + " (width=" + width + ")...");
                    if (format == Format.SVG) {
                        Graphviz.fromGraph(mg).engine(engineChoice).width(width).render(Format.SVG).toFile(svgOut);
                        System.out.println("Wrote: " + svgOut.getPath());
                    } else {
                        Graphviz.fromGraph(mg).engine(engineChoice).width(width).render(Format.PNG).toFile(pngOut);
                        System.out.println("Wrote: " + pngOut.getPath());
                    }
                } catch (Throwable t) {
                    // Common failure mode when using wasm engine is OOM/malloc aborts. Give clear guidance.
                    System.out.println("Failed to render graph id=" + g.id + ": " + t.getMessage());
                    System.out.println("If this is due to wasm/native memory limits, install native Graphviz (dot) and ensure it's on PATH to let graphviz-java use the native engine.");
                }
            }
        }
    }

    private static String safeLabel(String s) {
        return s.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String truncateLabel(String s) {
        if (s == null) return "";
        if (s.length() > 30) return s.substring(0, 14) + "..." + s.substring(s.length() - 13);
        return s;
    }

    private static boolean isDotAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-V");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Thread.sleep(200);
            try {
                p.exitValue();
            } catch (IllegalThreadStateException itse) {
                p.destroy();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
