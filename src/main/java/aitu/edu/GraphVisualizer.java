package aitu.edu;

import com.google.gson.Gson;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
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
        String inputPath = args.length > 0 ? args[0] : "src/main/java/aitu/edu/ass_3_input.json";
        String outDirPath = args.length > 1 ? args[1] : "target/graphs";
        File outDir = new File(outDirPath);
        Files.createDirectories(outDir.toPath());

        Gson gson = new Gson();
        try (Reader r = new FileReader(inputPath)) {
            InputFile input = gson.fromJson(r, InputFile.class);
            if (input == null || input.graphs == null) {
                System.out.println("No graphs found in input JSON: " + inputPath);
                return;
            }
            for (GraphDef g : input.graphs) {
                MutableGraph mg = mutGraph("graph" + g.id).setDirected(false);
                if (g.nodes != null) {
                    for (String n : g.nodes) {
                        mg.add(mutNode(n).add(Label.of(n)));
                    }
                }
                Set<String> seen = new HashSet<>();
                if (g.edges != null) {
                    for (EdgeDef e : g.edges) {
                        String a = e.from;
                        String b = e.to;
                        if (a == null || b == null) continue;
                        String key = a.compareTo(b) <= 0 ? a + "||" + b : b + "||" + a;
                        if (seen.add(key)) {
                            mg.add(mutNode(a).addLink(mutNode(b).add(Label.of(String.valueOf(e.weight)))));
                        }
                    }
                }
                File svgOut = new File(outDir, "graph-" + g.id + ".svg");
                File pngOut = new File(outDir, "graph-" + g.id + ".png");
                Graphviz.fromGraph(mg).width(800).render(Format.SVG).toFile(svgOut);
                Graphviz.fromGraph(mg).width(800).render(Format.PNG).toFile(pngOut);
                System.out.println("Wrote: " + svgOut.getPath() + " and " + pngOut.getPath());
            }
        }
    }
}
