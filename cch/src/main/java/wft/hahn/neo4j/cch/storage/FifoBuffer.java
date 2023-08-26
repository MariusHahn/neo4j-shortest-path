package wft.hahn.neo4j.cch.storage;

import java.nio.file.Path;
import java.util.Collections;
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
        if (!alreadyLoaded(rank)) {
            loadArcs(rank);
        }
        if (!positions.containsKey(rank)) return Collections.emptySet();
        Set<DiskArc> arcs = new LinkedHashSet<>();
        int readPointer = positions.getOrDefault(rank, -1) + bufferSize;
        while (continueReadArcs(rank, readPointer)) {
            final DiskArc diskArc = buffer[readPointer % (bufferSize)];
            arcs.add(diskArc);
            readPointer--;
        }
        return arcs;
    }

    private boolean alreadyLoaded(int rank) {
        if (positions.containsKey(rank)) {
            final int position = positions.get(rank);
            int sigRank = mode == Mode.OUT ? buffer[position].start() : buffer[position].end();
            if (buffer[position] == null || sigRank != rank) {
                positions.remove(rank);
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean continueReadArcs(int rank, int readPointer) {
        final DiskArc arc = buffer[readPointer % bufferSize];
        if (arc == null) {
            return false;
        }
        int edgeRank = mode == Mode.OUT ? arc.start() : arc.end();
        return edgeRank == rank;
    }

    private void loadArcs(int rank) {
        final List<DiskArc> arcs = arcReader.getArcs(rank);
        for (DiskArc arc : arcs) {
            buffer[position] = arc;
            positions.put(mode == Mode.OUT ? arc.start() : arc.end(), position);
            position = (position + 1) % bufferSize;
        }
        removeProbablyInCompleteArcSet();
    }

    private void removeProbablyInCompleteArcSet() {
        final DiskArc diskArc = buffer[(position) % bufferSize];
        if (diskArc != null) {
            positions.remove(mode == Mode.OUT ? diskArc.start() : diskArc.end());
        }
    }

    @Override
    public void close() throws Exception {
        arcReader.close();
    }
}
