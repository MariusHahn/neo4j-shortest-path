package wft.hahn.neo4j.cch.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;

@EqualsAndHashCode(of = {"start", "end"}) @ToString
public final class Arc implements PathElement {
    public static final int BYTE_SIZE = 16;
    public final Vertex start;
    public final Vertex end;
    public final float weight;
    public final Vertex middle;
    public final int hopLength;

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

    public byte[] toBytes() {
        return ByteBuffer.allocate(BYTE_SIZE)
                .putInt(0, start.rank)
                .putInt(4, end.rank)
                .putInt(8, middle == null ? Vertex.UNSET : middle.rank)
                .putFloat(12, weight)
                .array();
    }
}
