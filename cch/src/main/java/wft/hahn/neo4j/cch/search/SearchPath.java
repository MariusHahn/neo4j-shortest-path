package wft.hahn.neo4j.cch.search;

import java.util.Iterator;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.PathElement;
import wft.hahn.neo4j.cch.model.Vertex;

public interface SearchPath {
    SearchVertex start();
    SearchVertex end();
    SearchArc lastArc();
    Iterable<SearchArc> arcs();
    Iterable<SearchArc> reverseArcs();
    Iterable<SearchVertex> vertices();
    Iterable<SearchVertex> reverseVertices();
    int length();
    @Override
    String toString();
    Iterator<SearchPathElement> iterator();
    float weight();
}
