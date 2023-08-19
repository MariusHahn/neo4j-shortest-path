package wft.hahn.neo4j.cch.storage;

import java.nio.file.Path;
import java.util.Arrays;

public class PositionWriter extends Writer {

    private final int[] positions;

    public PositionWriter(Mode mode, Path basePath, int vertexCount) {
        super(mode, basePath.resolve(mode.name() + ".pos"));
        positions = new int[vertexCount];
        Arrays.fill(positions, -1);
    }

    public void write(int rank, int position) {
        positions[rank] = position;
    }

    @Override
    protected void flushBuffer() {
        for (int position : positions) {
            if (buffer.position() == buffer.capacity()) {
                write(file, buffer);
                buffer.position(0);
            }
            buffer.putInt(position);
        }
        while (buffer.position() < buffer.capacity()) {
            buffer.putInt(-1);
        }
        write(file, buffer);
        buffer.position(0);
    }

    public boolean alreadyWritten(int rank) {
        return positions[rank] != -1;
    }
}
