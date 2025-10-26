package aitu.edu.visual;

import aitu.edu.mst.core.Edge;

public interface MSTStepListener {
    void onInit(int V, Iterable<Edge> edges);
    void onVisitVertex(int v);
    void onConsiderEdge(int v, int w, double weight);
    void onAcceptEdge(int v, int w, double weight);
    void onRejectEdge(int v, int w, double weight);
    void onFinish();
}
