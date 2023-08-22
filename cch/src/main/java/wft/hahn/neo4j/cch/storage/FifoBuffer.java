package wft.hahn.neo4j.cch.storage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public Set<DiskArc> arcs(int rank) {
        return arcs(rank, 0);
    }
    public Set<DiskArc> arcs(int rank, int count) {
        Set<DiskArc> arcs = new LinkedHashSet<>();
        if (!alreadyLoaded(rank)) {
            loadArcs(rank);
        }
        for (int readPointer = positions.get(rank) + bufferSize; continueReadArcs(rank, readPointer); readPointer--) {
            DiskArc diskArc = buffer[readPointer % (bufferSize)];
            arcs.add(diskArc);
        }
        return arcs;
    }

    private boolean alreadyLoaded(int rank) {
        if (positions.containsKey(rank)) {
            final int position = positions.get(rank);
            if (mode == Mode.OUT) {
                if (buffer[position] == null || buffer[position].start() != rank) {
                    positions.remove(rank);
                } else {
                    return true;
                }
            }
            if (mode == Mode.IN) {
                if (buffer[position] == null || buffer[position].end() != rank) {
                    positions.remove(rank);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean continueReadArcs(int rank, int readPointer) {
        if (buffer[readPointer % bufferSize] == null) {
            return false;
        }
        return mode == Mode.OUT
                ? buffer[readPointer % bufferSize].start() == rank
                : buffer[readPointer % bufferSize].end() == rank;
    }

    private void loadArcs(int rank) {
        final List<DiskArc> arcs = arcReader.getArcs(rank);
        for (DiskArc arc : arcs) {
            buffer[position] = arc;
            positions.put(mode == Mode.OUT ? arc.start() : arc.end(), position);
            position = (position + 1) % bufferSize;
        }
        DiskArc diskArc = buffer[(position) % bufferSize];
        if (diskArc != null) {
            if (mode == Mode.OUT) {
                positions.remove(diskArc.start());
            } else {
                positions.remove(diskArc.end());
            }
        }
    }

    @Override
    public void close() throws Exception {
        arcReader.close();
    }
}
