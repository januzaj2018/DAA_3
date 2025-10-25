package aitu.edu;

public interface MSTStepListener {
    // Called once at start with total vertices and all edges
    void onInit(int V, Iterable<Edge> edges);
    // For Prim: when a vertex is added to MST frontier
    void onVisitVertex(int v);
    // When algorithm considers an edge (relax/scan)
    void onConsiderEdge(int v, int w, double weight);
    // When an edge is accepted into MST
    void onAcceptEdge(int v, int w, double weight);
    // When an edge is rejected (cycle or not optimal)
    void onRejectEdge(int v, int w, double weight);
    // Called once when algorithm finishes
    void onFinish();
}
