package wft.hahn.neo4j.cch.storage;

import java.nio.ByteBuffer;

public record DiskArc(int start, int end, int middle, int weight) {

    public static final int BYTE_SIZE = 16;

    public DiskArc(ByteBuffer buffer) {
        this(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
    }

    public byte[] toBytes() {
        return ByteBuffer.allocate(BYTE_SIZE).putInt(start).putInt(end).putInt(middle).putInt(weight).array();
    }

    boolean isArc() {
        return start != -1 && end != -1;
    }

    @Override
    public String toString() {
        return "(%02d)-[%9d]->(%02d), via: %02d".formatted(start, weight, end, middle);
    }

}
