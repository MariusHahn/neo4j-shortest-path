package wft.hahn.neo4j.cch.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static wft.hahn.neo4j.cch.storage.Writer.DISK_BLOCK_SIZE;

public class ArcReader extends Reader {

    private final PositionReader positionReader;

    public ArcReader(Mode mode, Path basePath) {
        super(mode, basePath, ".cch");
        positionReader = new PositionReader(mode, basePath);
    }

    public DiskArc[] getAllArcs() {
        final int fileLength = getLength();
        final DiskArc[] diskArcs = new DiskArc[fileLength / 16];
        for (int i = 0; i < diskArcs.length; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            try {
                file.seek(i*16);
                file.read(buffer.array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            diskArcs[i] = new DiskArc(buffer);
        }
        return diskArcs;
    }

    private int getLength() {
        try {
            return (int) file.length();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<DiskArc> getArcs(int rank) {
        final int blockPositionForRank = positionReader.getPositionForRank(rank);
        final List<DiskArc> diskArcs = new LinkedList<>();
        try {
            buffer.position(0);
            file.seek((long) blockPositionForRank * DISK_BLOCK_SIZE);
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
