package wft.hahn.neo4j.cch.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import lombok.ToString;
import org.neo4j.graphdb.Node;
import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

@ToString(of = {"name"})
public final class Vertex implements PathElement {
    public static final int UNSET = -1;
    private final String name;
    public int rank;
    public int contractedLevel;
    public final String elementId;
    private final Set<Arc> inArcs;
    private final Set<Arc> outArcs;

    public Vertex(String elementId, String name) {
        this.name = name;
        rank = UNSET;
        this.elementId = elementId;
        this.inArcs = new HashSet<>();
        this.outArcs = new HashSet<>();
        contractedLevel = 0;
    }

    public Vertex(Node node) {
        this(node.getElementId(), getProperty(node, "name"));
    }

    public Set<Arc> outArcs() {
        return outArcs;
    }

    public Set<Arc> inArcs() {
        return inArcs;
    }

    public Stream<Vertex> outNeighbors() {
        return outArcs.stream().map(arc -> arc.end);
    }

    public Stream<Vertex> inNeighbors() {
        return inArcs.stream().map(arc -> arc.start);
    }

    public Stream<Arc> arcs() {
        return Stream.concat(outArcs.stream(), inArcs.stream());
    }

    public void addArc(Arc arc) {
        if (arc.start.equals(this)){
            outArcs.add(arc);
        } else if (arc.end.equals(this)) {
            inArcs.add(arc);
        } else {
            throw new IllegalStateException("arc doesn't belong to vertex");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Vertex other = (Vertex) o;
        return  (rank == UNSET || other.rank == UNSET)
                ? elementId.equals(other.elementId)
                : rank == other.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId);
    }

    public int getDegree() {
        return inArcs.size() + outArcs.size();
    }
}
