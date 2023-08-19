package wft.hahn.neo4j.cch.storage;

import java.nio.ByteBuffer;

public record DiskArc(int start, int end, int middle, float weight) {

    public static final int BYTE_SIZE = 16;

    public DiskArc(ByteBuffer buffer) {
        this(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getFloat());
    }

    public byte[] toBytes() {
        return ByteBuffer.allocate(BYTE_SIZE).putInt(start).putInt(end).putInt(middle).putFloat(weight).array();
    }

    boolean isArc() {
        return start != -1 && end != -1;
    }

    @Override
    public String toString() {
        return "(%02d)-[%.2f]->(%02d), via: %02d".formatted(start, weight, end, middle);
    }

}
