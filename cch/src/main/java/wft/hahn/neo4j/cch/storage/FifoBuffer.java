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
        Set<DiskArc> arcs = new LinkedHashSet<>();
        if (!alreadyLoaded(rank)) {
            loadArcs(rank);
        }
        for (int readPointer = positions.get(rank) + bufferSize; continueReadArcs(rank, readPointer); readPointer--) {
            final DiskArc diskArc = buffer[readPointer % (bufferSize)];
            arcs.add(diskArc);
        }
        return arcs;
    }

    private boolean alreadyLoaded(int rank) {
        if (positions.containsKey(rank)) {
            final int position = positions.get(rank);
            if (buffer[position] == null || buffer[position].start() != rank) {
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
        return arc.start() == rank;
    }

    private void loadArcs(int rank) {
        final List<DiskArc> arcs = arcReader.getArcs(rank);
        for (DiskArc arc : arcs) {
            buffer[position] = arc;
            positions.put(arc.start(), position);
            position = (position + 1) % bufferSize;
        }
        removeProbablyInCompleteArcSet();
    }

    private void removeProbablyInCompleteArcSet() {
        final DiskArc diskArc = buffer[(position) % bufferSize];
        if (diskArc != null) {
            positions.remove(diskArc.start());
        }
    }

    @Override
    public void close() throws Exception {
        arcReader.close();
    }
}
