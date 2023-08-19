package wft.hahn.neo4j.cch.model;


import java.util.Iterator;

import wft.hahn.neo4j.cch.search.SearchArc;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchPathElement;
import wft.hahn.neo4j.cch.search.SearchVertex;
import wtf.hahn.neo4j.util.iterable.JoinIterable;
import wtf.hahn.neo4j.util.iterable.MappingIterable;
import wtf.hahn.neo4j.util.iterable.ReverseIterator;
import wtf.hahn.neo4j.util.iterable.ZipIterable;

public record BidirectionalSearchPath(SearchPath forward, SearchPath backward) implements SearchPath, Comparable<BidirectionalSearchPath> {

    @Override
    public SearchVertex start() {
        return forward.start();
    }

    @Override
    public SearchVertex end() {
        return backward.start();
    }

    @Override
    public SearchArc lastArc() {
        return backward.arcs().iterator().next();
    }

    @Override
    public Iterable<SearchArc> arcs() {
        return new JoinIterable<>(forward.arcs(), new MappingIterable<>(new ReverseIterator<>(backward.arcs()), SearchArc::reverse));
    }

    @Override
    public Iterable<SearchArc> reverseArcs() {
        return new ReverseIterator<>(arcs());
    }

    @Override
    public Iterable<SearchVertex> vertices() {
        return new JoinIterable<>(forward.vertices(), new ReverseIterator<>(backward.vertices()));
    }

    @Override
    public Iterable<SearchVertex> reverseVertices() {
        return new ReverseIterator<>(vertices());
    }

    @Override
    public int length() {
        return forward.length() + backward.length();
    }

    @Override
    public Iterator<SearchPathElement> iterator() {
        return new ZipIterable<>(vertices(), arcs()).iterator();
    }

    @Override
    public float weight() {
        return forward.weight() + backward.weight();
    }

    @Override
    public int compareTo(BidirectionalSearchPath o) {
        return Float.compare(weight(), o.weight());
    }
}
