package wft.hahn.neo4j.cch.storage;

import static wft.hahn.neo4j.cch.storage.Writer.DISK_BLOCK_SIZE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PositionReader extends Reader {
    public static final int BLOCK_DIVISOR = DISK_BLOCK_SIZE / 4;
    // rank -> file block
    private final Map<Integer, Integer> positions;


    public PositionReader(Mode mode, Path basePath) {
        super(mode, basePath, ".pos");
        this.positions = new HashMap<>();

    }

    public int getPositionForRank(int rank) {
        if (!positions.containsKey(rank)) {
            loadPositions(rank);
        }
        final Integer position = positions.get(rank);
        if (position == null) {
            throw new IllegalStateException();
        }
        return position;
    }

    private void loadPositions(int rank) {
        int blockNumber = rank / BLOCK_DIVISOR;
        try {
            buffer.position(0);
            file.seek((long) blockNumber * DISK_BLOCK_SIZE);
            file.read(buffer.array());
            int readRank = blockNumber * BLOCK_DIVISOR;
            while (buffer.position() < buffer.capacity()) {
                final int fileBlockPosition = buffer.getInt();
                positions.put(readRank++, fileBlockPosition);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
