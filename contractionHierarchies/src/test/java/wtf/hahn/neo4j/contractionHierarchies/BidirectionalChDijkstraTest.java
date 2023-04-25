package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexerByEdgeDifference.Mode.INMEMORY;

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
import wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexerByEdgeDifference;
import wtf.hahn.neo4j.contractionHierarchies.search.BidirectionChDijkstra;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class BidirectionalChDijkstraTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;
    private final BidirectionChDijkstra
            bidirectionChDijkstra = new BidirectionChDijkstra(relationshipType(), costProperty());

    public BidirectionalChDijkstraTest() {
        super(of(), of(), of(), TestDataset.OLDENBURG);
        try (Transaction transaction = database().beginTx()) {
            new ContractionHierarchiesIndexerByEdgeDifference(
                    edgeLabel, costProperty, transaction, INMEMORY
            ).insertShortcuts();
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
                ShortestPathResult chPath = bidirectionChDijkstra.find(start, end);
                Assertions.assertNotNull(chPath);
                //System.err.printf("path length %s, searchSpace size %s%n", dijkstraPath.length(), chPath.searchSpaceSize());
                Assertions.assertEquals(
                        dijkstraPath.weight()
                        , chPath.weight()
                        , "%s%n%s".formatted(dijkstraPath.toString(), chPath.toString())
                );
            }
        }
    }
}
