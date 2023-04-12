package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;


public class RomeTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;

    public RomeTest() {
        super(of(), of(), of(), TestDataset.ROME);
        try (Transaction transaction = database().beginTx()) {
            Comparator<Node> comparator = Comparator.comparingInt(Node::getDegree);
            new ContractionHierarchiesIndexer(edgeLabel, costProperty, transaction, comparator, database()).insertShortcuts();
            transaction.commit();
        }
    }

    @Test
    void testRomeEdgeCases() throws IOException {
        Files.lines(Paths.get("src", "test", "resources", "testRomeEdgeCases.csv"))
                .skip(1)
                .parallel()
                .map(line -> line.split(","))
                .forEach(line -> sourceTargetProbe(Integer.valueOf(line[0].trim()), Integer.valueOf(line[1].trim())));
    }

    @Test
    void randomPaths() {
        Random random = new Random(73);
        random.ints(200, 1, 3353)
                .mapToObj(i -> random.ints(200, 1, 3353).mapToObj(j -> new int[] {i, j}))
                .flatMap(Function.identity())
                .parallel()
                .forEach(x -> sourceTargetProbe(x[0], x[1]));
    }

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
