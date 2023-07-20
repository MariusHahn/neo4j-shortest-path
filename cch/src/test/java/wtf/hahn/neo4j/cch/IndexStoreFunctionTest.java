package wtf.hahn.neo4j.cch;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.IndexStoreFunction;
import wft.hahn.neo4j.cch.storage.Mode;

public class IndexStoreFunctionTest {
    private final Vertex[] vertices = new Vertex[11];

    @Test
    void outTest() throws IOException {
        fillVertices();
        fillUpwards();
        try (IndexStoreFunction indexStoreFunction = new IndexStoreFunction(vertices[10], Mode.OUT)) {
            indexStoreFunction.go();
        }
        Collection<Integer> ranks = new LinkedHashSet<>();
        try (RandomAccessFile outFile = new RandomAccessFile("OUT.storage", "r")) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            outFile.read(buffer.array());
            for (int i = 0; i < 4096; i=i+16) {
                int from = buffer.getInt(i);
                int to = buffer.getInt(i+4);
                int middle = buffer.getInt(i+8);
                float weight = buffer.getFloat(i+12);
                if (from == -1) break;
                System.out.printf("(%d)-[%.2f]->(%2d), via: %d%n", from, weight, to, middle);
                ranks.add(from);
            }
        }
        int[] ranksArray = ranks.stream().mapToInt(i -> i).toArray();
        Assertions.assertEquals(0, ranksArray[0]);
        Assertions.assertEquals(9, ranksArray[1]);
        Assertions.assertEquals(1, ranksArray[2]);
        Assertions.assertEquals(7, ranksArray[3]);
        Assertions.assertEquals(5, ranksArray[4]);
        Assertions.assertEquals(2, ranksArray[5]);
        Assertions.assertEquals(4, ranksArray[6]);
        Assertions.assertEquals(3, ranksArray[7]);
        Assertions.assertEquals(8, ranksArray[8]);
        Assertions.assertEquals(6, ranksArray[9]);
        Assertions.assertEquals(10, ranksArray.length);
    }
    @Test
    void inTest() throws IOException {
        fillVertices();
        fillDownwards();
        try (IndexStoreFunction indexStoreFunction = new IndexStoreFunction(vertices[10], Mode.IN)) {
            indexStoreFunction.go();
        }
        Collection<Integer> ranks = new LinkedHashSet<>();
        try (RandomAccessFile outFile = new RandomAccessFile("OUT.storage", "r")) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            outFile.read(buffer.array());
            for (int i = 0; i < 4096; i=i+16) {
                int from = buffer.getInt(i);
                int to = buffer.getInt(i+4);
                int middle = buffer.getInt(i+8);
                float weight = buffer.getFloat(i+12);
                if (from == -1) break;
                System.out.printf("(%d)-[%.2f]->(%2d), via: %d%n", from, weight, to, middle);
                ranks.add(from);
            }
        }
        int[] ranksArray = ranks.stream().mapToInt(i -> i).toArray();
        Assertions.assertEquals(10, ranksArray.length);
        Assertions.assertEquals(0, ranksArray[0]);
        Assertions.assertEquals(9, ranksArray[1]);
        Assertions.assertEquals(1, ranksArray[2]);
        Assertions.assertEquals(7, ranksArray[3]);
        Assertions.assertEquals(5, ranksArray[4]);
        Assertions.assertEquals(2, ranksArray[5]);
        Assertions.assertEquals(4, ranksArray[6]);
        Assertions.assertEquals(3, ranksArray[7]);
        Assertions.assertEquals(8, ranksArray[8]);
        Assertions.assertEquals(6, ranksArray[9]);
    }

    private void fillDownwards() {
        Arc[] arcs = new Arc[] {
                new Arc(vertices[0], vertices[1], 1),
                new Arc(vertices[0], vertices[6], 2),
                new Arc(vertices[0], vertices[7], 4),

                new Arc(vertices[1], vertices[2], 2),
                new Arc(vertices[1], vertices[4], 1),

                new Arc(vertices[3], vertices[2], 2),
                new Arc(vertices[3], vertices[5], 2),
                new Arc(vertices[3], vertices[8], 4, vertices[5], 2),

                new Arc(vertices[4], vertices[2], 2),
                new Arc(vertices[4], vertices[3], 1),
                new Arc(vertices[4], vertices[5], 3),

                new Arc(vertices[5], vertices[7], 4),

                new Arc(vertices[6], vertices[7], 2),
                new Arc(vertices[6], vertices[9], 1),

                new Arc(vertices[8], vertices[5], 2),

                new Arc(vertices[9], vertices[7], 1),
                new Arc(vertices[9], vertices[8], 2),

                new Arc(vertices[10], vertices[0], 1),
                new Arc(vertices[10], vertices[2], 2),
        };
        for (Arc arc : arcs) { arc.start.addArc(arc); arc.end.addArc(arc); }

    }

    private void fillUpwards() {
        Arc[] arcs = new Arc[] {
                new Arc(vertices[0], vertices[10], 1),

                new Arc(vertices[1], vertices[0], 1),

                new Arc(vertices[2], vertices[1], 2),
                new Arc(vertices[2], vertices[3], 2),
                new Arc(vertices[2], vertices[4], 2),
                new Arc(vertices[2], vertices[10], 2),

                new Arc(vertices[3], vertices[4], 1),

                new Arc(vertices[4], vertices[1], 1),

                new Arc(vertices[5], vertices[3], 2),
                new Arc(vertices[5], vertices[4], 3),
                new Arc(vertices[5], vertices[8], 2),

                new Arc(vertices[6], vertices[0], 2),

                new Arc(vertices[7], vertices[0], 4),
                new Arc(vertices[7], vertices[5], 4),
                new Arc(vertices[7], vertices[6], 2),
                new Arc(vertices[7], vertices[9], 1),

                new Arc(vertices[8], vertices[3], 4, vertices[5], 2),
                new Arc(vertices[8], vertices[9], 2),

                new Arc(vertices[9], vertices[6], 1),
        };
        for (Arc arc : arcs) { arc.start.addArc(arc); arc.end.addArc(arc); }
    }

    private void fillVertices() {
        vertices[0] = new Vertex(null, "V0");
        vertices[0].rank = 9;
        vertices[1] = new Vertex(null, "V1");
        vertices[1].rank = 7;
        vertices[2] = new Vertex(null, "V2");
        vertices[2].rank = 0;
        vertices[3] = new Vertex(null, "V3");
        vertices[3].rank = 4;
        vertices[4] = new Vertex(null, "V4");
        vertices[4].rank = 5;
        vertices[5] = new Vertex(null, "V5");
        vertices[5].rank = 2;
        vertices[6] = new Vertex(null, "V6");
        vertices[6].rank = 8;
        vertices[7] = new Vertex(null, "V7");
        vertices[7].rank = 1;
        vertices[8] = new Vertex(null, "V8");
        vertices[8].rank = 3;
        vertices[9] = new Vertex(null, "V9");
        vertices[9].rank = 6;
        vertices[10] = new Vertex(null, "V10");
        vertices[10].rank = 10;
    }
}
