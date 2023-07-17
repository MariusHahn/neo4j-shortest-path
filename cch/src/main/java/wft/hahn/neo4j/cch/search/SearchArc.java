package wft.hahn.neo4j.cch.search;

public class SearchArc implements SearchPathElement{
    public final SearchVertex start, end, middle;
    public final float weight;

    public SearchArc(SearchVertex start, SearchVertex end, SearchVertex middle, float weight) {
        assert start != null;
        assert end != null;
        this.start = start;
        this.end = end;
        this.middle = middle;
        this.weight = weight;
    }

    public SearchVertex otherVertex(SearchVertex vertex) {
        return vertex.equals(start) ? end : start;
    }
}
