package wft.hahn.neo4j.cch.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.neo4j.fabric.eval.Catalog;

public class BufferManager implements AutoCloseable {
    private final RandomAccessFile arcFile;
    private final RandomAccessFile positionFile;
    private final Mode mode;
    private final Map<Integer, Collection<BufferArc>> buffer = new HashMap<>();
    private final Map<Integer, Integer> positions = new HashMap<>();

    public BufferManager(Mode mode) {
        this.arcFile = open(mode.name() + ".storage");
        this.positionFile = open(mode.name() + ".positions");
        this.mode = mode;
    }

    public Collection<BufferArc> arcs(int rank) {
        if (!buffer.containsKey(rank)) {
            if (!positions.containsKey(rank)) {
                final int block = rank / 1024;
                final byte[] bytes = new byte[4096];
                try {
                    positionFile.seek(block * 1024);
                    positionFile.read(bytes, 0, 4096);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
                for (int i = 0, end = bytes.length / 4; i < end; i++) {
                    positions.put(block * 1024 + i, ByteBuffer.wrap(bytes, i * 4, 4).getInt());
                }
            }
            final byte[] bytes = new byte[4096];
            try {
                arcFile.seek(positions.getOrDefault(rank, -1));
                arcFile.read(bytes, 0, 4096);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
            for (int i = 0, l = bytes.length / 16; i < l; i++) {
                final ByteBuffer wrap = ByteBuffer.wrap(bytes, i * 16, 16);
                final int startRank = wrap.getInt(0);
                final int endRank = wrap.getInt(4);
                final int middleRank = wrap.getInt(12);
                final float weight = wrap.getFloat(8);
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
        return buffer.get(rank);
    }

    @Override
    public void close() throws Exception {
        if (arcFile != null) arcFile.close();
        if (positionFile != null) positionFile.close();
    }

    private static RandomAccessFile open(String name) {
        try {
            return new RandomAccessFile(name, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public record BufferArc(int s, int t, int m, float weight) { }
}
