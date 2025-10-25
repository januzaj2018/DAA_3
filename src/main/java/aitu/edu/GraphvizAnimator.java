package aitu.edu;

import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.io.File;
import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * Step-by-step frame exporter using graphviz-java.
 * Writes one image per step to the given directory.
 */
public class GraphvizAnimator implements MSTStepListener, AutoCloseable {
    private final File outDir;
    private final Format format;
    private final int width;
    private final int frameSkip;
    private int stepCounter = 0;

    private int V = 0;
    private final List<Edge> allEdges = new ArrayList<>();
    private boolean[] visited;
    private final Map<String, Status> edgeStatus = new HashMap<>();

    private enum Status { NONE, CONSIDER, ACCEPT, REJECT }

    public GraphvizAnimator(String framesDir) {
        this(framesDir, Format.PNG, 1000, 1);
    }

    public GraphvizAnimator(String framesDir, Format format, int width, int frameSkip) {
        this.outDir = new File(framesDir);
        this.format = format == null ? Format.PNG : format;
        this.width = Math.max(200, width);
        this.frameSkip = Math.max(1, frameSkip);
        // ensure directory exists
        this.outDir.mkdirs();
    }

    @Override
    public void onInit(int V, Iterable<Edge> edges) {
        this.V = V;
        this.visited = new boolean[V];
        this.allEdges.clear();
        for (Edge e : edges) this.allEdges.add(e);
        this.edgeStatus.clear();
        renderMaybe();
    }

    @Override
    public void onVisitVertex(int v) {
        if (v >= 0 && v < V) visited[v] = true;
        renderMaybe();
    }

    @Override
    public void onConsiderEdge(int v, int w, double weight) {
        edgeStatus.put(key(v, w), Status.CONSIDER);
        renderMaybe();
    }

    @Override
    public void onAcceptEdge(int v, int w, double weight) {
        edgeStatus.put(key(v, w), Status.ACCEPT);
        // When an edge is accepted into the MST, consider both endpoints as connected/visited
        if (v >= 0 && v < V) visited[v] = true;
        if (w >= 0 && w < V) visited[w] = true;
        renderMaybe();
    }

    @Override
    public void onRejectEdge(int v, int w, double weight) {
        edgeStatus.put(key(v, w), Status.REJECT);
        renderMaybe();
    }

    @Override
    public void onFinish() {
        renderMaybe();
    }

    private void renderMaybe() {
        stepCounter++;
        if (stepCounter % frameSkip != 0) return;
        try {
            renderFrame(stepCounter / frameSkip);
        } catch (Exception ex) {
            System.err.println("[GraphvizAnimator] Failed to render frame: " + ex.getMessage());
        }
    }

    private void renderFrame(int frameIndex) throws Exception {
        MutableGraph mg = mutGraph("mst-step").setDirected(false);
        // graph-level styling
        mg.graphAttrs().add(
                Attributes.attr("bgcolor", "white"),
                Attributes.attr("splines", "true"),
                Attributes.attr("overlap", "false"),
                Attributes.attr("outputorder", "edgesfirst"),
                Attributes.attr("nodesep", "0.2"),
                Attributes.attr("ranksep", "0.3")
        );
        // default node appearance
        mg.nodeAttrs().add(
                Shape.CIRCLE,
                Attributes.attr("fontname", "Helvetica"),
                Attributes.attr("fontsize", "10")
        );
        // default edge appearance
        mg.linkAttrs().add(
                Attributes.attr("color", "#BBBBBB"),
                Attributes.attr("penwidth", "1.0")
        );

        // build nodes
        Map<Integer, MutableNode> nodes = new HashMap<>();
        for (int v = 0; v < V; v++) {
            MutableNode n = mutNode("n" + v)
                    .add(Label.of(String.valueOf(v)))
                    .add(visited[v] ? Color.GREEN : Color.LIGHTBLUE);
            mg.add(n);
            nodes.put(v, n);
        }
        // build edges with per-status styling
        boolean isLarge = V >= 80 || allEdges.size() >= 150; // same threshold used in GraphVisualizer
        for (Edge e : allEdges) {
            int v = e.either();
            int w = e.other(v);
            Status st = edgeStatus.getOrDefault(key(v, w), Status.NONE);
            var link = to(nodes.get(w));
            switch (st) {
                case CONSIDER -> link = link.with(Attributes.attr("color", "#ff9800"), Attributes.attr("penwidth", "2.0"));
                case ACCEPT -> link = link.with(Attributes.attr("color", "#2ca25f"), Attributes.attr("penwidth", "3.0"));
                case REJECT -> link = link.with(Attributes.attr("color", "#ef5350"), Attributes.attr("penwidth", "1.5"));
                default -> link = link.with(Attributes.attr("color", "#BBBBBB"), Attributes.attr("penwidth", "1.0"));
            }
            // Show numeric edge weight labels for small graphs (keeps frames readable for tiny/medium graphs)
            if (!isLarge) {
                try {
                    link = link.with(Label.of(String.format("%.2f", e.weight())));
                } catch (Exception ignored) {
                    // if for any reason formatting/labeling fails, silently continue without a label
                }
            }
            nodes.get(v).addLink(link);
        }

        String fname = String.format("frame_%05d", frameIndex);
        File out = new File(outDir, fname + (format == Format.SVG ? ".svg" : ".png"));
        Graphviz.fromGraph(mg).engine(Engine.NEATO).width(width).render(format).toFile(out);
    }

    private String key(int a, int b) {
        int x = Math.min(a, b);
        int y = Math.max(a, b);
        return x + "||" + y;
    }

    @Override
    public void close() {
        // nothing to close for graphviz-java renderer
    }
}
