package aitu.edu.visual;

import aitu.edu.mst.core.Edge;

public class GraphStreamAnimator implements MSTStepListener, AutoCloseable {
    private final boolean verbose;

    public GraphStreamAnimator(boolean showWindow, boolean exportFrames, String framesDir) {
        this(showWindow, exportFrames, framesDir, true);
    }

    public GraphStreamAnimator(boolean showWindow, boolean exportFrames, String framesDir, boolean verbose) {
        this.verbose = verbose;
        if (verbose) {
            System.out.println("[GraphStreamAnimator-Stub] showWindow=" + showWindow + ", exportFrames=" + exportFrames + ", framesDir=" + framesDir);
            System.out.println("GraphStream is not included. Use GraphvizAnimator for image frames.");
        }
    }

    @Override
    public void onInit(int V, Iterable<Edge> edges) {
        if (verbose) System.out.println("[Step] Init V=" + V);
    }

    @Override
    public void onVisitVertex(int v) {
        if (verbose) System.out.println("[Step] Visit vertex " + v);
    }

    @Override
    public void onConsiderEdge(int v, int w, double weight) {
        if (verbose) System.out.println("[Step] Consider " + v + "-" + w + " w=" + weight);
    }

    @Override
    public void onAcceptEdge(int v, int w, double weight) {
        if (verbose) System.out.println("[Step] Accept   " + v + "-" + w + " w=" + weight);
    }

    @Override
    public void onRejectEdge(int v, int w, double weight) {
        if (verbose) System.out.println("[Step] Reject   " + v + "-" + w + " w=" + weight);
    }

    @Override
    public void onFinish() {
        if (verbose) System.out.println("[Step] Finish");
    }

    @Override
    public void close() {
        if (verbose) System.out.println("[GraphStreamAnimator-Stub] Closed");
    }
}
