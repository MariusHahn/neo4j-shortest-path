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

    public List<DiskArc> arcs(int rank) {
        List<DiskArc> arcs = new LinkedList<>();
        if (!alreadyLoaded(rank)) {
            loadArcs(rank);
        }
        for (int readPointer = positions.get(rank); continueReadArcs(rank, readPointer); readPointer++) {
            DiskArc diskArc = buffer[readPointer % (bufferSize)];
            arcs.add(diskArc);
        }
        return arcs;
    }

    private boolean alreadyLoaded(int rank) {
        if (positions.containsKey(rank)) {
            final int position = positions.get(rank);
            if (mode == Mode.OUT) {
                if (buffer[position].start() != rank) {
                    positions.remove(rank, position);
                } else {
                    return true;
                }
            }
            if (mode == Mode.IN) {
                if (buffer[position].end() != rank) {
                    positions.remove(rank, position);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean continueReadArcs(int rank, int readPointer) {
        if ((bufferSize * 2) + 1 < readPointer) {
            throw new IllegalStateException();
        }
        if (buffer[readPointer % bufferSize] == null) {
            return false;
        }
        return mode == Mode.OUT
                ? buffer[readPointer % bufferSize].start() == rank
                : buffer[readPointer % bufferSize].end() == rank;
    }

    private void loadArcs(int rank) {
        final List<DiskArc> arcs = arcReader.getArcs(rank);
        if (arcs.isEmpty()) return;
        for (DiskArc arc : arcs) {
            buffer[position] = arc;
            positions.computeIfAbsent(mode == Mode.OUT ? arc.start() : arc.end(), key ->  position);
            position = (position + 1) % bufferSize;
        }
    }

    @Override
    public void close() throws Exception {
        arcReader.close();
    }
}
