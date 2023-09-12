package wft.hahn.neo4j.cch.storage;

import org.apache.commons.collections.functors.WhileClosure;
import static wft.hahn.neo4j.cch.storage.Writer.DISK_BLOCK_SIZE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ArcReader extends Reader {

    private final PositionReader positionReader;

    public ArcReader(Mode mode, Path basePath) {
        super(mode, basePath, ".cch");
        positionReader = new PositionReader(mode, basePath);
    }

    public Stream<DiskArc> getAllArcs() {
        return IntStream.range(0, getLength()).map(i -> i / DISK_BLOCK_SIZE).mapToObj(i -> {
            Stream.Builder<DiskArc> diskArcs = Stream.builder();
            buffer.position(0);
            try {
                file.seek((long) i * DISK_BLOCK_SIZE);
                file.read(buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (buffer.position() < buffer.capacity()) {
                final DiskArc diskArc = new DiskArc(buffer);
                if (diskArc.isArc()) {
                    diskArcs.add(diskArc);
                }
            }
            return diskArcs.build();
        }).flatMap(Function.identity());

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
