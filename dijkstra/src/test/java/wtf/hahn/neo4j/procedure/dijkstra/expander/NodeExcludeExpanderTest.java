package wtf.hahn.neo4j.procedure.dijkstra.expander;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static wtf.hahn.neo4j.util.IterationHelper.stream;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.dijkstra.Neo4jDijkstra;
import wtf.hahn.neo4j.dijkstra.expander.NodeExcludeExpander;
import wtf.hahn.neo4j.util.IntegrationTest;

public class NodeExcludeExpanderTest extends IntegrationTest {

    private final Neo4jDijkstra neo4jDijkstra = new Neo4jDijkstra();

    public NodeExcludeExpanderTest() {
        super(of(), of(), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @ParameterizedTest
    @MethodSource("provideStringsForIsBlank")
    void restrictNodeTest(String restrictedNode, double weight, String[] pathNames) {
        try (Transaction transaction = database().beginTx()) {
            Node nodeA = transaction.findNode(() -> "Location", "name", "A");
            Node nodeF = transaction.findNode(() -> "Location", "name", "F");
            Node restricted = transaction.findNode(() -> "Location", "name", restrictedNode);
            NodeExcludeExpander expander = new NodeExcludeExpander(restricted, relationshipType());
            WeightedPath path = neo4jDijkstra.shortestPath(nodeA, nodeF, expander, costProperty());
            String[] retrievedPathNames =
                    stream(path.nodes()).map(n -> (String) n.getProperty("name")).toArray(String[]::new);
            System.out.printf("\"%s\"", String.join("\", \"", retrievedPathNames));
            assertArrayEquals(pathNames, retrievedPathNames);
            assertEquals(weight, path.weight());
        }
    }

    private static Stream<Arguments> provideStringsForIsBlank() {
        return Stream.of(
                Arguments.of("C", 160.0, new String[] {"A", "B", "D", "E", "F"})
                , Arguments.of("D", 170.0, new String[] {"A", "C", "E", "F"})
                , Arguments.of("E", 170.0, new String[] {"A", "C", "D", "F"})
        );
    }
}
