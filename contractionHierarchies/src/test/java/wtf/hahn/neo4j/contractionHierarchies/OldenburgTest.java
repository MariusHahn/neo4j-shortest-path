package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;
import static org.neo4j.graphdb.Direction.OUTGOING;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
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
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class OldenburgTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;

    public OldenburgTest() {
        super(of(), of(), of(), TestDataset.OLDENBURG);
        try (Transaction transaction = database().beginTx()) {
            new ContractionHierarchiesIndexerByEdgeDifference(
                    edgeLabel, costProperty, transaction, database()
            ).insertShortcuts();
            transaction.commit();
        }
    }

    private static Stream<Arguments> fromTheoPaths() throws IOException {
        return Files.lines(IntegrationTest.resourcePath().resolve("oldenburg-st.csv"))
                .map(line -> Arrays.stream(line.split(",")).map(Integer::valueOf).toArray(Integer[]::new))
                .map(ids -> Arguments.of(ids[0], ids[1]));
    }

    private static Stream<Arguments> randomPaths() {
        return new Random(73).ints(31, 0, 6104)
                .mapToObj(i -> new Random(37).ints(31, 0, 6104).filter(j -> j != i).mapToObj(j -> new int[] {i, j}))
                .flatMap(Function.identity())
                .map(x -> Arguments.of(x[0], x[1]));
    }

    @ParameterizedTest
    @MethodSource({"randomPaths", "fromTheoPaths",})
    void testNodeIdToNodeId(Integer s, Integer t) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchies chFinder = new ContractionHierarchies(database(), transaction);
            NativeDijkstra dijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "id", s);
            Node end = transaction.findNode(() -> "Location", "id", t);
            PathExpander<Double> standardExpander = PathExpanders.forTypeAndDirection(relationshipType(), OUTGOING);
            WeightedPath dijkstraPath = dijkstra.shortestPath(start, end, standardExpander, costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath =
                        (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty)
                                .findFirst()
                                .get().path;
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
