package wft.hahn.neo4j.cch.storage;

import static wft.hahn.neo4j.cch.storage.Writer.DISK_BLOCK_SIZE;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public abstract class Reader implements AutoCloseable {

    protected final RandomAccessFile file;
    protected final ByteBuffer buffer = ByteBuffer.allocate(DISK_BLOCK_SIZE);

    protected Reader(Mode mode, Path basePath, String fileEnding) {
        this.file = openFile(basePath.resolve(mode.name() + fileEnding));
    }

    protected static RandomAccessFile openFile(Path path) {
        try {
            return new RandomAccessFile(path.toFile(), "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        file.close();
    }

}
