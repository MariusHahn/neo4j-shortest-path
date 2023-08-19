package wft.hahn.neo4j.cch.storage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FifoBuffer implements AutoCloseable {

    private final DiskArc[] buffer;
    public final Mode mode;
    private int position = 0;
    // rank -> bufferPosition
    private final Map<Integer, Integer> positions;
    private final int bufferSize;
    private final ArcReader arcReader;

    public FifoBuffer(int bufferSize, Mode mode, Path basePath) {
        buffer = new DiskArc[bufferSize];
        positions = new HashMap<>(bufferSize * 4 / 3);
        this.bufferSize = bufferSize;
        this.mode = mode;
        arcReader = new ArcReader(this.mode, basePath);

    }

    public Iterable<DiskArc> arcs(int rank) {
        List<DiskArc> arcs = new LinkedList<>();
        if (!positions.containsKey(rank)) {
            loadArcs(rank);
        }
        for (int readPointer = positions.get(rank); allArcsRead(rank, readPointer); readPointer++) {
            arcs.add(buffer[readPointer % (bufferSize - 1)]);
        }
        return arcs;
    }

    private boolean allArcsRead(int rank, int readPointer) {
        if (readPointer + bufferSize > bufferSize * 2) {
            throw new IllegalStateException();
        }
        if (buffer[readPointer % bufferSize] == null) {
            return true;
        }
        return mode == Mode.OUT
                ? buffer[readPointer % bufferSize].start() == rank
                : buffer[readPointer % bufferSize].end() == rank;
    }

    private void loadArcs(int rank) {
        final Iterable<DiskArc> arcs = arcReader.getArcs(rank);
        for (DiskArc arc : arcs) {
            buffer[position] = arc;
            positions.put(mode == Mode.OUT ? arc.start() : arc.end(), position);
            position = (position + 1) % bufferSize;
        }
    }

    @Override
    public void close() throws Exception {
        arcReader.close();
    }
}
