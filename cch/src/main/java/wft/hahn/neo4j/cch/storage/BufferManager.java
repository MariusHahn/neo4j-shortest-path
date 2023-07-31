package wft.hahn.neo4j.cch.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.io.File;

public class BufferManager implements AutoCloseable {
    private final RandomAccessFile arcFile;
    private final RandomAccessFile positionFile;
    public final Mode mode;
    private final Map<Integer, Collection<BufferArc>> buffer = new HashMap<>();
    private final Map<Integer, Integer> positions = new HashMap<>();

    public BufferManager(Mode mode, Path path) {
        this.arcFile = open(path.resolve("%s.storage".formatted(mode.name())).toFile());
        this.positionFile = open(path.resolve("%s.positions".formatted(mode.name())).toFile());
        this.mode = mode;
    }

    public Collection<BufferArc> arcs(int rank) {
        final ByteBuffer allocate = ByteBuffer.allocate(4096);
        if (!buffer.containsKey(rank)) {
            if (!positions.containsKey(rank)) {
                final int block = rank / 1024;
                final byte[] bytes = allocate.array();
                try {
                    positionFile.seek(block * 1024);
                    positionFile.read(bytes, 0, 4096);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
                for (int i = 0, end = bytes.length / 4; i < end; i++) {
                    int position = allocate.getInt(i * 4);
                    if (position != -1) {
                        positions.put(block * 1024 + i, position);
                    }
                }
            }
            final ByteBuffer readBlock = ByteBuffer.allocate(4096);
            final byte[] bytes = readBlock.array();
            try {
                arcFile.seek(positions.getOrDefault(rank, -1));
                arcFile.read(bytes, 0, 4096);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
            for (int i = 0, l = bytes.length / 16; i < l; i++) {
                final int offset = i * 16;
                final int startRank = readBlock.getInt(offset);
                final int endRank = readBlock.getInt(offset + 4);
                final int middleRank = readBlock.getInt(offset + 8);
                final float weight = readBlock.getFloat(offset + 12);
                if (startRank == -1 || endRank == -1) continue;
                if (Mode.OUT.equals(mode)) {
                    Collection<BufferArc> arcs = buffer.computeIfAbsent(startRank, x -> new HashSet<>());
                    arcs.add(new BufferArc(startRank, endRank, middleRank, weight));
                }
                if (Mode.IN.equals(mode)) {
                    Collection<BufferArc> arcs = buffer.computeIfAbsent(endRank, x -> new HashSet<>());
                    arcs.add(new BufferArc(endRank, startRank, middleRank, weight));
                }
            }
        }
        return buffer.getOrDefault(rank, List.of());
    }

    @Override
    public void close() throws Exception {
        if (arcFile != null) arcFile.close();
        if (positionFile != null) positionFile.close();
    }

    private static RandomAccessFile open(File name) {
        try {
            return new RandomAccessFile(name, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public record BufferArc(int s, int t, int m, float weight) { }
}
