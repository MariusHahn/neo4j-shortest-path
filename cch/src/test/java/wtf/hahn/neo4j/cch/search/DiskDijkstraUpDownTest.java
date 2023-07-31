package wtf.hahn.neo4j.cch.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.BufferManager;
import wft.hahn.neo4j.cch.storage.IndexStoreFunction;
import wft.hahn.neo4j.cch.storage.Mode;
import wtf.hahn.neo4j.cch.storage.IndexStoreFunctionTest;

public class DiskDijkstraUpDownTest {

    private static void setupPaperGraphTest(Vertex topNode, Path path) {
        try (/*val x = new IndexStoreFunction(topNode, Mode.OUT, path);*/
             val y = new IndexStoreFunction(topNode, Mode.IN, path)) {
            //x.go();
            y.go();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    void dijkstraOutTest(@TempDir Path tempPath) {
        Vertex[] vertices = IndexStoreFunctionTest.fillVertices();
        IndexStoreFunctionTest.fillUpwards(vertices);
        setupPaperGraphTest(vertices[10], tempPath);
        DiskDijkstra dijkstra = new DiskDijkstra(new BufferManager(Mode.OUT, tempPath));
        Map<Integer, SearchPath> paths = dijkstra.find(0);
        paths.forEach((rank, path) -> System.out.println(SearchVertexPaths.toString(path)));
        assertEquals(6, paths.size());
        SearchPath to0 = paths.get(0);
        assertEquals(0.0f, to0.weight()); assertEquals(0, to0.length());
        SearchPath to4 = paths.get(4);
        assertEquals(2.0f, to4.weight()); assertEquals(1, to4.length());
        SearchPath to5 = paths.get(5);
        assertEquals(2.0f, to5.weight()); assertEquals(1, to5.length());
        SearchPath to7 = paths.get(7);
        assertEquals(2.0f, to7.weight()); assertEquals(1, to7.length());
        SearchPath to9 = paths.get(9);
        assertEquals(3.0f, to9.weight()); assertEquals(2, to9.length());
        SearchPath to10 = paths.get(10);
        assertEquals(2.0f, to10.weight()); assertEquals(1, to10.length());
    }

    @Test
    void dijkstraInTest(@TempDir Path tempPath) {
        Vertex[] vertices = IndexStoreFunctionTest.fillVertices();
        IndexStoreFunctionTest.fillDownwards(vertices);
        setupPaperGraphTest(vertices[10], tempPath);
        DiskDijkstra dijkstra = new DiskDijkstra(new BufferManager(Mode.IN, tempPath));
        Map<Integer, SearchPath> paths = dijkstra.find(0);
        paths.forEach((rank, path) -> System.out.println(SearchVertexPaths.toString(path)));
        assertEquals(6, paths.size());
        SearchPath to0 = paths.get(0);
        assertEquals(0.0f, to0.weight()); assertEquals(0, to0.length());
        SearchPath to4 = paths.get(4);
        assertEquals(2.0f, to4.weight()); assertEquals(1, to4.length());
        SearchPath to5 = paths.get(5);
        assertEquals(2.0f, to5.weight()); assertEquals(1, to5.length());
        SearchPath to7 = paths.get(7);
        assertEquals(2.0f, to7.weight()); assertEquals(1, to7.length());
        SearchPath to10 = paths.get(10);
        assertEquals(2.0f, to10.weight()); assertEquals(1, to10.length());
        SearchPath to9 = paths.get(9);
        assertEquals(3.0f, to9.weight()); assertEquals(2, to9.length());
    }
}
