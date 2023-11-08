package wft.hahn.neo4j.cch.search;

public class SearchArc implements SearchPathElement {
    public final SearchVertex start, end, middle;
    public final int weight;

    public SearchArc(SearchVertex start, SearchVertex end, SearchVertex middle, int weight) {
        assert start != null;
        assert end != null;
        assert start.rank != end.rank;
        this.start = start;
        this.end = end;
        this.middle = middle;
        this.weight = weight;
    }

    public SearchVertex otherVertex(SearchVertex vertex) {
        return vertex.equals(start) ? end : start;
    }

    public static SearchArc reverse(SearchArc arc) {return new SearchArc(arc.end, arc.start, arc.middle, arc.weight);}

    @Override
    public String toString() {
        return "(%d)-[%3d]->(%d)".formatted(start.rank, weight, end.rank);
    }
}
