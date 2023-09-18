package wtf.hahn.neo4j.cch.search;

import static java.util.List.of;
import static wtf.hahn.neo4j.cch.search.DiskChDijkstraTest.setupPaperGraphTest;
import static wtf.hahn.neo4j.util.EntityHelper.getLongProperty;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.update.Updater;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.PathUtils;

public class UpperTriangleTest extends IntegrationTest {

    @TempDir
    private static Path path;

    public UpperTriangleTest() {
        super(of(), of(), of(), TestDataset.UPPER_TRIANGLE);

    }
    @BeforeAll void setUp() {
        Vertex[] vertices = fillVertices();
        fillUpwards(vertices);
        fillDownwards(vertices);
        setupPaperGraphTest(vertices[2], path);
        try (Transaction transaction = database().beginTx()) {
            transaction.findRelationships(() -> "ROAD", "x", "x")
                    .forEachRemaining(relationship -> relationship.setProperty("cost", 1));
            Updater updater = new Updater(transaction, path);
            setupPaperGraphTest(updater.update(), path);
            transaction.commit();
        }
    }

    private static Stream<Arguments> ranksToTest() {
        return Stream.of(Arguments.of(1, 2), Arguments.of(2,1));
    }

    @ParameterizedTest
    @MethodSource({"ranksToTest"})
    void upperTriangleTest(Integer i, Integer j) {
        Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty());
        try (Transaction transaction = database().beginTx(); DiskChDijkstra diskChDijkstra = new DiskChDijkstra(path)) {
            Node start = transaction.findNode(() -> "Location", "id", i);
            Node end = transaction.findNode(() -> "Location", "id", j);
            WeightedPath expectPath = dijkstra.find(start, end);
            int startRank = (int) getLongProperty(start, "ROAD_rank");
            int endRank = (int) getLongProperty(end, "ROAD_rank");
            SearchPath cchPath = diskChDijkstra.find(startRank, endRank);
            System.out.println(SearchVertexPaths.toString(cchPath));
            System.out.println(PathUtils.toRankString(expectPath));
            Assertions.assertEquals(expectPath.weight(), cchPath.weight(), i + " -> " + j);
        }
    }

    private static void fillUpwards(Vertex[] vertices) {
        vertices[0].addArc(vertices[1], null, 2, 1);
        vertices[0].addArc(vertices[2], null, 2, 1);
        vertices[1].addArc(vertices[2], vertices[0], 4, 2);
    }
    
    private static void fillDownwards(Vertex[] vertices) {
        vertices[1].addArc(vertices[0], null, 2, 1);
        vertices[2].addArc(vertices[0], null, 2, 1);
        vertices[2].addArc(vertices[1], vertices[0], 4, 2);
    }

    private static Vertex[] fillVertices() {
        Vertex[] vertices = new Vertex[11];
        vertices[0] = new Vertex(null, "V0");
        vertices[0].rank = 0;
        vertices[1] = new Vertex(null, "V1");
        vertices[1].rank = 1;
        vertices[2] = new Vertex(null, "V2");
        vertices[2].rank = 2;
        return vertices;
    }
}
