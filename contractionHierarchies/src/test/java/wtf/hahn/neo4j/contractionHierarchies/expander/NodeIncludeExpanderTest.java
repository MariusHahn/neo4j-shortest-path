package wtf.hahn.neo4j.contractionHierarchies.expander;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.util.Iterables;
import wtf.hahn.neo4j.contractionHierarchies.search.NativeDijkstra;
import wtf.hahn.neo4j.contractionHierarchies.TestDataset;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.EntityHelper;

public class NodeIncludeExpanderTest extends IntegrationTest {


    public NodeIncludeExpanderTest() {
        super(of(), of(), of(), TestDataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }


    @ParameterizedTest
    @MethodSource("dontFindViaPathArguments")
    void dontFindViaTest(String s, String viaNode, String t) {
        try (Transaction transaction = database().beginTx()) {
            NativeDijkstra nativeDijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "name", s);
            Node target = transaction.findNode(() -> "Location", "name", t);
            Node via = transaction.findNode(() -> "Location", "name", viaNode);
            NodeIncludeExpander expander = new NodeIncludeExpander(via, relationshipType(), Shortcuts.rankPropertyName(relationshipType()));
            WeightedPath path = nativeDijkstra.shortestPath(start, target, expander, costProperty());
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
            NativeDijkstra nativeDijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "name", s);
            Node target = transaction.findNode(() -> "Location", "name", t);
            Node via = transaction.findNode(() -> "Location", "name", viaNode);
            NodeIncludeExpander expander = new NodeIncludeExpander(via, relationshipType(), Shortcuts.rankPropertyName(relationshipType()));
            WeightedPath path = nativeDijkstra.shortestPath(start, target, expander, costProperty());
            String[] retrievedPathNames = Iterables.stream(path.nodes()).map(EntityHelper::getNameProperty).toArray(String[]::new);
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
