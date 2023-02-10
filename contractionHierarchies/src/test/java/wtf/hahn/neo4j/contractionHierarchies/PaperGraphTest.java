package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.EntityHelper;

public class PaperGraphTest extends IntegrationTest {
    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;

    public PaperGraphTest() {
        super(of(), of(), of(), TestDataset.SEMINAR_PAPER);
        try (Transaction transaction = database().beginTx()) {
            Comparator<Node> comparator =
                    Comparator.comparingLong(node -> EntityHelper.<Long>getProperty(node, "paper_rank"));
            new ContractionHierarchiesIndexer(edgeLabel, costProperty, transaction, comparator).insertShortcuts();
            transaction.commit();
        }
    }

    @ParameterizedTest
    @MethodSource("someArguments")
    void sourceTargetSamples(Number startNodeId, Number endNodeId) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchiesFinder chFinder =
                    new ContractionHierarchiesFinder(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra()
                    .shortestPathWithShortcuts(start, end, RelationshipType.withName(edgeLabel), costProperty);
            if (dijkstraPath != null) {
                WeightedPath chPath = chFinder
                        .find(start, end, RelationshipType.withName(edgeLabel), costProperty);
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight());
            }
        }
    }

    private static Stream<Arguments> someArguments() {
        return IntStream.rangeClosed(0, 10)
                .mapToObj(i -> IntStream.rangeClosed(i, 10).mapToObj(j -> Arguments.of(i,j)))
                .flatMap(Function.identity());
    }
}
