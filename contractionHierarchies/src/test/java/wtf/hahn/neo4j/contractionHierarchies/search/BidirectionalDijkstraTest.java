package wtf.hahn.neo4j.contractionHierarchies.search;

import static java.util.List.of;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference.Mode.INMEMORY;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.TestDataset;
import wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class BidirectionalDijkstraTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;
    private final BidirectionalDijkstra chDijkstra = new BidirectionalDijkstra(relationshipType(), costProperty());

    public BidirectionalDijkstraTest() {
        super(of(), of(), of(), TestDataset.OLDENBURG);
        try (Transaction transaction = database().beginTx()) {
            new IndexerByEdgeDifference(edgeLabel, costProperty, transaction, INMEMORY).insertShortcuts();
            transaction.commit();
        }
    }

    private static Stream<Arguments> fromTheoPaths() throws IOException {
        return Files.lines(IntegrationTest.resourcePath().resolve("oldenburg-st.csv"))
                .map(line -> Arrays.stream(line.split(",")).map(Integer::valueOf).toArray(Integer[]::new))
                .map(ids -> Arguments.of(ids[0], ids[1]));
    }

    @ParameterizedTest
    @MethodSource({"fromTheoPaths",})
    void testNodeIdToNodeId(Integer s, Integer t) {
        try (Transaction transaction = database().beginTx()) {
            NativeDijkstra dijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "id", s);
            Node end = transaction.findNode(() -> "Location", "id", t);
            PathExpander<Double> standardExpander = PathExpanders.forTypeAndDirection(relationshipType(), OUTGOING);
            WeightedPath dijkstraPath = dijkstra.shortestPath(start, end, standardExpander, costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath = chDijkstra.find(start, end);
                Assertions.assertNotNull(chPath);
                Assertions.assertEquals(
                        dijkstraPath.weight()
                        , chPath.weight()
                        , "%s%n%s".formatted(dijkstraPath.toString(), chPath.toString())
                );
            }
        }
    }
}
