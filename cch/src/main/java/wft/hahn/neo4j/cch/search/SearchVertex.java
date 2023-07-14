package wft.hahn.neo4j.cch.search;

import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashSet;

@RequiredArgsConstructor
public class SearchVertex implements SearchPathElement {
    public final int rank;
    public final Collection<SearchArc> arcs = new HashSet<>();

    public boolean isLoaded() { return arcs.size() !=0; }
    public void addArc(SearchArc arc) { arcs.add(arc); }
}
