package wft.hahn.neo4j.cch.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public abstract class Writer implements AutoCloseable {
    public static final int DISK_BLOCK_SIZE = 4096 ;
    protected final Mode mode;
    protected final RandomAccessFile file;
    protected final ByteBuffer buffer;


    protected Writer(Mode mode, Path path) {
        this.mode = mode;
        this.file = openFile(path);
        this.buffer = ByteBuffer.allocate(DISK_BLOCK_SIZE);
    }

    private static RandomAccessFile openFile(Path path) {
        try {
            return new RandomAccessFile(path.toFile(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected abstract void flushBuffer();

    protected static void write(RandomAccessFile file, ByteBuffer buffer) {
        try {
            file.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        flushBuffer();
        file.close();
    }
}
