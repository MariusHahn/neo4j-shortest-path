package wft.hahn.neo4j.cch.storage;

import static wft.hahn.neo4j.cch.storage.Mode.OUT;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

public class ArcWriter extends Writer implements AutoCloseable {

    public static final byte[] INVALID_ARC =
            ByteBuffer.allocate(16).putInt(-1).putInt(-1).putInt(-1).putInt(-1).array();
    int blockPosition = 0;

    public ArcWriter(Mode mode, Path basePath) {
        super(mode, basePath.resolve(mode.name() + ".cch"));
    }

    public int write(Vertex vertex) {
        final Set<Arc> arcs = new HashSet<>(mode == OUT ? vertex.outArcs() : vertex.inArcs());
        List<DiskArc> diskArcs = getDiskArcs(arcs);
        return write(diskArcs);
    }

    public int write(List<DiskArc> diskArcs) {
        if (buffer.capacity() < diskArcs.size() * DiskArc.BYTE_SIZE) {
            throw new IllegalStateException();
        }
        if (buffer.capacity() - buffer.position() < DiskArc.BYTE_SIZE * diskArcs.size()) {
            flushBuffer();
        }
        for (DiskArc diskArc : diskArcs) {
            buffer.put(diskArc.toBytes());
        }
        return blockPosition;
    }

    private List<DiskArc> getDiskArcs(Set<Arc> arcs) {
        final List<DiskArc> diskArcs = new LinkedList<>();
        for (Arc arc : arcs) {
            if (isSearchArc(arc)) {
                final int rank = arc.middle == null ? -1 : arc.middle.rank;
                diskArcs.add(new DiskArc(arc.start.rank, arc.end.rank, rank, arc.weight));
            }
        }
        return diskArcs;
    }

    protected void flushBuffer() {
        while (buffer.position() != buffer.capacity()) {
            buffer.put(INVALID_ARC);
        }
        write(file, buffer);
        buffer.position(0);
        blockPosition++;
    }

    private boolean isSearchArc(Arc arc) {
        return mode == OUT
                ? arc.start.rank < arc.end.rank
                : arc.end.rank < arc.start.rank;
    }
}
