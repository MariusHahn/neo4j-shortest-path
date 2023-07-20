package wtf.hahn.neo4j.cch.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.IndexStoreFunction;
import wft.hahn.neo4j.cch.storage.Mode;

public class IndexStoreFunctionTest {

    @Test
    void outTest(@TempDir Path path) throws IOException {
        Vertex[] vertices = fillVertices();
        fillUpwards(vertices);
        try (IndexStoreFunction indexStoreFunction = new IndexStoreFunction(vertices[10], Mode.OUT, path)) {
            indexStoreFunction.go();
        }
        List<Edge> edges = new LinkedList<>();
        try (RandomAccessFile outFile = new RandomAccessFile(path.resolve("OUT.storage").toFile(), "r")) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            outFile.read(buffer.array());
            for (int i = 0; i < 4096; i=i+16) {
                Edge edge = new Edge(buffer.getInt(i), buffer.getInt(i + 4), buffer.getInt(i + 8)
                        , buffer.getFloat(i + 12));
                if (edge.fromRank == -1) break;
                System.out.println(edge);
                edges.add(edge);
            }
        }
        Assertions.assertEquals(19, edges.size());
        Assertions.assertEquals(new Edge(9, 10, -1, 1.f), edges.get(0));
        Assertions.assertEquals(new Edge(0, 10, -1, 2.f), edges.get(1));
        Assertions.assertEquals(new Edge(7, 9, -1, 1.0f), edges.get(2));
        Assertions.assertEquals(new Edge(8, 9, -1, 2.0f), edges.get(3));
        Assertions.assertEquals(new Edge(1, 9, -1, 4.0f), edges.get(4));
        Assertions.assertEquals(new Edge(0, 7, -1, 2.0f), edges.get(5));
        Assertions.assertEquals(new Edge(5, 7, -1, 1.0f), edges.get(6));
        Assertions.assertEquals(new Edge(0, 5, -1, 2.0f), edges.get(7));
        Assertions.assertEquals(new Edge(4, 5, -1, 1.0f), edges.get(8));
        Assertions.assertEquals(new Edge(2, 5, -1, 3.0f), edges.get(9));
        Assertions.assertEquals(new Edge(1, 2, -1, 4.0f), edges.get(10));
        Assertions.assertEquals(new Edge(0, 4, -1, 2.0f), edges.get(11));
        Assertions.assertEquals(new Edge(2, 4, -1, 2.0f), edges.get(12));
        Assertions.assertEquals(new Edge(3, 4,  2, 4.0f), edges.get(13));
        Assertions.assertEquals(new Edge(2, 3, -1, 2.0f), edges.get(14));
        Assertions.assertEquals(new Edge(1, 8, -1, 2.0f), edges.get(15));
        Assertions.assertEquals(new Edge(6, 8, -1, 1.0f), edges.get(16));
        Assertions.assertEquals(new Edge(1, 6, -1, 1.0f), edges.get(17));
        Assertions.assertEquals(new Edge(3, 6, -1, 2.0f), edges.get(18));
    }

    record Edge(int fromRank, int toRank, int middle, float weight) {
        @Override public String toString() {
            return "(%02d)-[%.2f]->(%02d), via: %02d".formatted(fromRank, weight, toRank, middle); }
    }

    @Test
    void inTest(@TempDir Path path) throws IOException {
        Vertex[] vertices = fillVertices();
        fillDownwards(vertices);
        try (IndexStoreFunction indexStoreFunction = new IndexStoreFunction(vertices[10], Mode.IN, path)) {
            indexStoreFunction.go();
        }
        List<Edge> edges = new LinkedList<>();
        try (RandomAccessFile outFile = new RandomAccessFile(path.resolve("IN.storage").toFile(), "r")) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            outFile.read(buffer.array());
            for (int i = 0; i < 4096; i=i+16) {
                Edge edge = new Edge(buffer.getInt(i), buffer.getInt(i + 4), buffer.getInt(i + 8)
                        , buffer.getFloat(i + 12));
                if (edge.fromRank == -1) break;
                System.out.println(edge);
                edges.add(edge);
            }
        }
        Assertions.assertEquals(19, edges.size());
        Assertions.assertEquals(new Edge(10, 9, -1, 1.f), edges.get(0));
        Assertions.assertEquals(new Edge(10, 0, -1, 2.f), edges.get(1));
        Assertions.assertEquals(new Edge(9, 7, -1, 1.0f), edges.get(2));
        Assertions.assertEquals(new Edge(9, 8, -1, 2.0f), edges.get(3));
        Assertions.assertEquals(new Edge(9, 1, -1, 4.0f), edges.get(4));
        Assertions.assertEquals(new Edge(7, 0, -1, 2.0f), edges.get(5));
        Assertions.assertEquals(new Edge(7, 5, -1, 1.0f), edges.get(6));
        Assertions.assertEquals(new Edge(5, 0, -1, 2.0f), edges.get(7));
        Assertions.assertEquals(new Edge(5, 4, -1, 1.0f), edges.get(8));
        Assertions.assertEquals(new Edge(5, 2, -1, 3.0f), edges.get(9));
        Assertions.assertEquals(new Edge(2, 1, -1, 4.0f), edges.get(10));
        Assertions.assertEquals(new Edge(4, 0, -1, 2.0f), edges.get(11));
        Assertions.assertEquals(new Edge(4, 2, -1, 2.0f), edges.get(12));
        Assertions.assertEquals(new Edge(4, 3,  2, 4.0f), edges.get(13));
        Assertions.assertEquals(new Edge(3, 2, -1, 2.0f), edges.get(14));
        Assertions.assertEquals(new Edge(8, 1, -1, 2.0f), edges.get(15));
        Assertions.assertEquals(new Edge(8, 6, -1, 1.0f), edges.get(16));
        Assertions.assertEquals(new Edge(6, 1, -1, 1.0f), edges.get(17));
        Assertions.assertEquals(new Edge(6, 3, -1, 2.0f), edges.get(18));
    }

    public static void fillDownwards(Vertex[] vertices) {
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

    public static void fillUpwards(Vertex[] vertices) {
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

    public static Vertex[] fillVertices() {
        Vertex[] vertices = new Vertex[11];
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
        return vertices;
    }
}
