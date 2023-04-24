package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexerOld;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;


public class RomeTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;

    public RomeTest() {
        super(of(), of(), of(), TestDataset.ROME);
        try (Transaction transaction = database().beginTx()) {
            Comparator<Node> comparator = Comparator.comparingInt(Node::getDegree);
            new ContractionHierarchiesIndexerOld(edgeLabel, costProperty, transaction, comparator, database()).insertShortcuts();
            transaction.commit();
        }
    }

    private static Stream<Arguments> testRomeEdgeCases() throws IOException {
        return Files.lines(Paths.get("src", "test", "resources", "testRomeEdgeCases.csv"))
                .skip(1)
                .parallel()
                .map(line -> line.split(","))
                .limit(1000)
                .map(line -> Arguments.of(Integer.valueOf(line[0].trim()), Integer.valueOf(line[1].trim())));
    }

    private static Stream<Arguments>  randomPaths() {
        return new Random(73).ints(31, 1, 3353)
                .mapToObj(i -> new Random(37).ints(31, 1, 3353).mapToObj(j -> new int[] {i, j}))
                .flatMap(Function.identity())
                .parallel()
                .map(x -> Arguments.of(x[0], x[1]));
    }

    @MethodSource({"randomPaths", "testRomeEdgeCases"})
    @ParameterizedTest
    void sourceTargetProbe(Integer startNodeId, Integer endNodeId) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchies chFinder = new ContractionHierarchies(database(), transaction);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra(new BasicEvaluationContext(
                    transaction, database()
            )).shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath =
                        (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty).findFirst().get().path;
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight(), "There should be a path from %d to %d".formatted(startNodeId, endNodeId));
            }
        }
    }
}
