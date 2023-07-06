package wft.hahn.neo4j.cch.model;

import org.neo4j.graphdb.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;


public final class Vertex implements PathElement {
    private static final int UNSET = -1;
    public int rank;
    public int contractedLevel;
    public final String elementId;
    private final Set<Arc> inArcs;
    private final Set<Arc> outArcs;

    public Vertex(String elementId) {
        rank = UNSET;
        this.elementId = elementId;
        this.inArcs = new HashSet<>();
        this.outArcs = new HashSet<>();
        contractedLevel = 0;
    }

    public Vertex(Node node) {
        this(node.getElementId());
    }

    public Set<Arc> outArcs() {
        return outArcs;
    }

    public Set<Arc> inArcs() {
        return inArcs;
    }

    public Stream<Vertex> outNeighbors() {
        return outArcs.stream().map(Arc::end);
    }

    public Stream<Vertex> inNeighbors() {
        return inArcs.stream().map(Arc::start);
    }

    public void addArc(Arc arc) {
        if (arc.start().equals(this)){
            outArcs.add(arc);
        } else if (arc.end().equals(this)) {
            inArcs.add(arc);
        } else {
            throw new IllegalStateException("arc doesn't belong to vertex");
        }
    }
}
