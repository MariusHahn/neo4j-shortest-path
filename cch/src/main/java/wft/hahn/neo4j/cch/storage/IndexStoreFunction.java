package wft.hahn.neo4j.cch.storage;

import static wft.hahn.neo4j.cch.model.Arc.BYTE_SIZE;
import static wft.hahn.neo4j.cch.storage.Mode.IN;
import static wft.hahn.neo4j.cch.storage.Mode.OUT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.val;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

public class IndexStoreFunction implements AutoCloseable {
    private final Deque<Iterator<Vertex>> stack;
    private final Mode mode;
    private final Writer writer;

    public IndexStoreFunction(Vertex topNode, Mode mode, Path path) {
        stack = new LinkedList<>();
        stack.push(List.of(topNode).iterator());
        this.mode = mode;
        this.writer = new Writer(mode, path);

    }

    public void go() {
        while (!stack.isEmpty()) {
            if (stack.peek().hasNext()) {
                val vertex = stack.peek().next();
                if (!writer.positions.containsKey(vertex)) {
                    writer.write(vertex);
                    stack.addFirst(neighbors(vertex));
                }
            } else stack.poll();
        }
    }

    private Iterator<Vertex> neighbors(Vertex vertex) {
        return (mode == IN ? vertex.outNeighbors() : vertex.inNeighbors())
                .sorted((Comparator.comparingInt(o -> o.rank))).iterator();
    }

    private static class Writer implements AutoCloseable{
        public static final int DISK_BLOCK_SIZE = 4096;
        private final Mode mode;
        private final Map<Vertex, Integer> positions = new HashMap<>();
        private final byte[] writeBuffer = new byte[DISK_BLOCK_SIZE];
        private final Path path;
        private int writeBufferPosition = 0;
        private int blockPosition = 0;
        private RandomAccessFile arcFile = null;

        public Writer(Mode mode, Path path) {
            this.path = path;
            try {
                arcFile = new RandomAccessFile( path.resolve(mode.name()).toFile() + ".storage", "rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            this.mode = mode;

        }

        private void flushBuffer() {
            final byte[] minusOne = ByteBuffer.allocate(4).putInt(-1).array();
            for (int i = writeBufferPosition; i < DISK_BLOCK_SIZE; i = i+16) {
                writeBuffer[i] = minusOne[0];
                writeBuffer[i+1] = minusOne[1];
                writeBuffer[i+2] = minusOne[2];
                writeBuffer[i+3] = minusOne[3];
            }
            try {
                arcFile.seek((long) DISK_BLOCK_SIZE * blockPosition++);
                arcFile.write(writeBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writeBufferPosition = 0;
        }


        void write(Vertex vertex) {
            val arcs = mode == OUT ? vertex.inArcs() : vertex.outArcs();
            val pack = new byte[arcs.size() * BYTE_SIZE];
            int packIndex = 0;
            for (final Arc arc : arcs) if (isSearchArc(arc, vertex)) {
                for (final byte b : arc.toBytes()) pack[packIndex++] = b;
            }
            if (writeBuffer.length - (writeBufferPosition + pack.length) < 0) flushBuffer();
            System.arraycopy(pack, 0, writeBuffer, writeBufferPosition, packIndex);
            writeBufferPosition += packIndex;
            positions.put(vertex, blockPosition);
        }

        private boolean isSearchArc(Arc arc, Vertex vertex) {
            return arc.otherVertex(vertex).rank < vertex.rank;
        }

        @Override
        public void close() throws IOException {
            flushBuffer();
            if (arcFile != null) arcFile.close();
            writeBufferPosition = 0;
            for (int i = 0, writeBufferLength = writeBuffer.length; i < writeBufferLength; i++) writeBuffer[i] = -1;
            blockPosition = 0;
            try (val positionFile = new RandomAccessFile(path.resolve(mode.name() + ".positions").toFile(), "rw")) {
                final int packSize = 4;
                val pack = ByteBuffer.allocate(packSize);
                for (val entry : positionsSortedByRank(positions)) {
                    val position = entry.getValue();
                    pack.putInt(0, position);
                    if (writeBuffer.length - (writeBufferPosition + pack.array().length) < 0) {
                        flush(positionFile, pack.array());
                    }
                    System.arraycopy(pack.array(), 0, writeBuffer, writeBufferPosition, pack.array().length);
                    writeBufferPosition += packSize;
                }
                flush(positionFile, writeBuffer);
            }
        }

        private static Iterable<Map.Entry<Vertex, Integer>> positionsSortedByRank(Map<Vertex, Integer> positions) {
            return positions.entrySet().stream().sorted(Comparator.comparingInt(x -> x.getKey().rank))::iterator;
        }

        private void flush(RandomAccessFile positionFile, byte[] pack) throws IOException {
            positionFile.seek((long) DISK_BLOCK_SIZE * blockPosition++);
            positionFile.write(pack);
            writeBufferPosition = 0;
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
