package wtf.hahn.neo4j.cch.search;

import java.nio.file.Path;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.val;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import wft.hahn.neo4j.cch.model.BidirectionalSearchPath;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wtf.hahn.neo4j.cch.storage.IndexStoreFunctionTest;

public class DiskChDijkstraTest {

    public static void setupPaperGraphTest(Vertex topNode, Path path) {
        try (val x = new StoreFunction(topNode, Mode.OUT, path);
             val y = new StoreFunction(topNode, Mode.IN, path)) {
            x.go();
            y.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Stream<Arguments> x() {
        return IntStream.range(0, 11)
                .mapToObj(i -> IntStream.range(0, 11).mapToObj(j -> Arguments.of(i, j)))
                .flatMap(x -> x);
    }

    @ParameterizedTest
    @MethodSource("x")
    void a(Integer start, Integer goal, @TempDir Path tempPath) {
        Vertex[] vertices = IndexStoreFunctionTest.fillVertices();
        IndexStoreFunctionTest.fillUpwards(vertices);
        IndexStoreFunctionTest.fillDownwards(vertices);
        setupPaperGraphTest(vertices[10], tempPath);
        DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
        BidirectionalSearchPath searchPath = (BidirectionalSearchPath) diskChDijkstra.find(start, goal);
        System.out.println(SearchVertexPaths.toString(searchPath));
    }
}
