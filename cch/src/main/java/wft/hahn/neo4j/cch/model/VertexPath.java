package wft.hahn.neo4j.cch.model;

import java.util.Iterator;

public interface VertexPath {
    Vertex start();
    Vertex end();
    Arc lastArc();
    Iterable<Arc> arcs();
    Iterable<Arc> reverseArcs();
    Iterable<Vertex> vertices();
    Iterable<Vertex> reverseVertices();
    int length();
    @Override
    String toString();
    Iterator<PathElement> iterator();
    float weight();
}
