package wtf.hahn.neo4j.cch.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wft.hahn.neo4j.cch.storage.PositionReader;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.PositionWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.IntStream;

public class PositionReaderWriterTest {

    @TempDir private static Path tempPath;

    @Test
    void positionsTest() throws Exception {
        final int vertexCount = 12238;
        final int[] expected = IntStream.range(0, vertexCount).map(i -> new Random().nextInt(0, 283)).toArray();
        try (PositionWriter positionWriter = new PositionWriter(Mode.OUT, tempPath, vertexCount)) {
            for (int i = 0; i < expected.length; i++) {
                Assertions.assertFalse(positionWriter.alreadyWritten(i));
            }
            for (int i = 0; i < expected.length; i++) {
                positionWriter.write(i, expected[i]);
            }
            for (int i = 0; i < expected.length; i++) {
                Assertions.assertTrue(positionWriter.alreadyWritten(i));
            }
        }
        try (PositionReader reader = new PositionReader(Mode.OUT, tempPath)) {
            for (int i = 0; i < expected.length; i++) {
                System.out.println(i);
                Assertions.assertEquals(expected[i] ,reader.getPositionForRank(i));
            }
        }
    }

    @Test
    void outToPlainText() throws Exception {
        toPlainText("OUT.pos");
    }

    @Test
    void inToPlainText() throws IOException {
        toPlainText("IN.pos");
    }

    private static void toPlainText(String first) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(Paths.get(first)));
        for (int i = 0; buffer.position() < buffer.capacity(); i++) {
            final int filePos = buffer.getInt();
            System.out.printf("%d -> %d%n", i, filePos);
        }
    }
}
