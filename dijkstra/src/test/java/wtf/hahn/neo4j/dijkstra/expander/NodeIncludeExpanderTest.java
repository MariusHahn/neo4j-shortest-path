package wtf.hahn.neo4j.dijkstra.expander;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static wtf.hahn.neo4j.util.IterationHelper.stream;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.dijkstra.Neo4jDijkstra;
import wtf.hahn.neo4j.util.IntegrationTest;

public class NodeIncludeExpanderTest extends IntegrationTest {

    private final Neo4jDijkstra neo4jDijkstra = new Neo4jDijkstra();

    public NodeIncludeExpanderTest() {
        super(of(), of(), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }


    @ParameterizedTest
    @MethodSource("dontFindViaPathArguments")
    void dontFindViaTest(String s, String viaNode, String t) {
        try (Transaction transaction = database().beginTx()) {
            Node start = transaction.findNode(() -> "Location", "name", s);
            Node target = transaction.findNode(() -> "Location", "name", t);
            Node via = transaction.findNode(() -> "Location", "name", viaNode);
            NodeIncludeExpander expander = new NodeIncludeExpander(via, relationshipType());
            WeightedPath path = neo4jDijkstra.shortestPath(start, target, expander, costProperty());
            Assertions.assertNull(path);
        }
    }

    private static Stream<Arguments> dontFindViaPathArguments() {
        return Stream.of(
                Arguments.of("A", "B", "F")
                , Arguments.of("A", "C", "F")
                , Arguments.of("A", "E", "F")
                , Arguments.of("B", "E", "F")
                , Arguments.of("A", "B", "E")
        );
    }

    @ParameterizedTest
    @MethodSource("findViaPathArguments")
    void findViaTest(String s, String viaNode, String t, double weight) {
        try (Transaction transaction = database().beginTx()) {
            Node start = transaction.findNode(() -> "Location", "name", s);
            Node target = transaction.findNode(() -> "Location", "name", t);
            Node via = transaction.findNode(() -> "Location", "name", viaNode);
            NodeIncludeExpander expander = new NodeIncludeExpander(via, relationshipType());
            WeightedPath path = neo4jDijkstra.shortestPath(start, target, expander, costProperty());
            String[] retrievedPathNames = stream(path.nodes()).map(n -> (String) n.getProperty("name")).toArray(String[]::new);
            System.out.printf("\"%s\"", String.join("\", \"", retrievedPathNames));
            assertArrayEquals(new String[]{s, viaNode, t}, retrievedPathNames);
            assertEquals(weight, path.weight());
        }
    }

    private static Stream<Arguments> findViaPathArguments() {
        return Stream.of(
                Arguments.of( "A", "B", "D", 90)
                , Arguments.of("A", "C", "D", 90)
                , Arguments.of("A", "D", "E", 130)
                , Arguments.of("A", "D", "F", 180)
                , Arguments.of("B", "D", "F", 120)
        );
    }
}
