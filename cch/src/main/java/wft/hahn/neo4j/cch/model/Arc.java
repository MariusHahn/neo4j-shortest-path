package wft.hahn.neo4j.cch.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;

@EqualsAndHashCode(of = {"start", "end"}) @ToString
public final class Arc implements PathElement {
    public static final int BYTE_SIZE = 16;
    public final Vertex start;
    public final Vertex end;
    public float weight;
    public Vertex middle;
    public int hopLength;

    public Arc(Vertex start, Vertex end, float weight) {
        this.start = start;
        this.end = end;
        this.weight = weight;
        this.middle = null;
        hopLength = 1;
    }

    public Arc(Vertex start, Vertex end, float weight, Vertex middle, int hopLength) {
        this.start = start;
        this.end = end;
        this.weight = weight;
        this.middle = middle;
        this.hopLength = hopLength;
    }

    public Vertex otherVertex(Vertex vertex) {
        return start.equals(vertex) ? end : start;
    }

}
