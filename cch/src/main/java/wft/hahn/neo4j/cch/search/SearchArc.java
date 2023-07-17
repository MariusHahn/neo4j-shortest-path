package wft.hahn.neo4j.cch.search;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SearchArc implements SearchPathElement{
    private final SearchVertex start, end, middle;
    public final float weight;

    public SearchVertex otherVertex(SearchVertex vertex) {
        return vertex.equals(start) ? end : start;
    }
}
