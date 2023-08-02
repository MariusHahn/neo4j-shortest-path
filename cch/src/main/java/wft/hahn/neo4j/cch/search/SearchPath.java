package wft.hahn.neo4j.cch.search;

import java.util.Iterator;

public interface SearchPath extends Iterable<SearchPathElement> {
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
