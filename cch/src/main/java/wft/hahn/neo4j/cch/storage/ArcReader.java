package wft.hahn.neo4j.cch.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ArcReader extends Reader {

    private final PositionReader positionReader;

    public ArcReader(Mode mode, Path basePath) {
        super(mode, basePath, ".cch");
        positionReader = new PositionReader(mode, basePath);
    }

    public List<DiskArc> getArcs(int rank) {
        final int blockPositionForRank = positionReader.getPositionForRank(rank);
        final List<DiskArc> diskArcs = new LinkedList<>();
        try {
            buffer.position(0);
            file.seek(blockPositionForRank * 4096L);
            file.read(buffer.array());
            while (buffer.position() < buffer.capacity()) {
                final DiskArc diskArc = new DiskArc(buffer);
                if (diskArc.isArc()) {
                    diskArcs.add(diskArc);
                }
            }
            return diskArcs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void close() throws Exception {
        super.close();
        positionReader.close();
    }
}
