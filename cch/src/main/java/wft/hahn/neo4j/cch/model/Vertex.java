package wft.hahn.neo4j.cch.model;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.neo4j.graphdb.Node;

import java.util.*;
import java.util.stream.Stream;

import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

@ToString(of = {"name", "rank"}) @AllArgsConstructor
public final class Vertex implements PathElement, Comparable<Vertex> {
    public static final int UNSET = -1;
    public final String name;
    public int rank;
    public int contractedLevel;
    public final String elementId;
    private final Map<Vertex, Arc> inArcs;
    private final Map<Vertex, Arc> outArcs;

    public Vertex(String elementId, String name) {
        this.name = name;
        rank = UNSET;
        this.elementId = elementId;
        this.inArcs = new HashMap<>();
        this.outArcs = new HashMap<>();
        contractedLevel = 0;
    }

    public Vertex(Node node) {
        this(node.getElementId(), getProperty(node, "name"));
    }

    public Vertex(int rank) {
        this.name = null;
        this.elementId = null;
        this.rank = rank;
        inArcs = new HashMap<>();
        outArcs = new HashMap<>();
    }

    public Arc getArcTo(Vertex other) {
        return outArcs.get(other);
    }

    public Collection<Arc> outArcs() {
        return outArcs.values();
    }

    public Collection<Arc> inArcs() {
        return inArcs.values();
    }

    public Set<Vertex> outNeighbors() {
        return outArcs.keySet();
    }

    public Set<Vertex> inNeighbors() {
        return inArcs.keySet();
    }

    public Stream<Arc> arcs() {
        return Stream.concat(outArcs.values().stream(), inArcs.values().stream());
    }


    public ArcUpdateStatus addArc(Vertex other, float weight) {
        return addArc(other, null, weight, 1);
    }

    public ArcUpdateStatus addArc(Vertex other, Vertex middle, float weight, int hopLength) {
        if (!outArcs.containsKey(other)) {
            Arc arc = new Arc(this, other, weight, middle, hopLength);
            this.outArcs.put(other, arc);
            other.inArcs.put(this, arc);
            return ArcUpdateStatus.CREATE;
        }
        if (weight < outArcs.get(other).weight) {
            Arc arc = outArcs.get(other);
            arc.weight = weight;
            arc.middle = middle;
            arc.hopLength = hopLength;
            return ArcUpdateStatus.UPDATED;
        }
        return ArcUpdateStatus.REJECTED;
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

    public int sumOfAtoDxHa() {
        int sum = 0;
        for (Arc arc : inArcs()) sum += arc.hopLength;
        for (Arc arc : outArcs()) sum += arc.hopLength;
        return sum;
    }

    @Override
    public int compareTo(Vertex o) {
        if (rank == UNSET && o.rank != UNSET) return 1;
        return Integer.compare(rank, o.rank);
    }

    public boolean smallerThan(Vertex o) {
        return compareTo(o) < 0;
    }
    public enum ArcUpdateStatus {CREATE, UPDATED, REJECTED}
}
