package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference.Mode.INMEMORY;

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
import wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference;
import wtf.hahn.neo4j.contractionHierarchies.search.TreeSearch;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class PaperGraphInMemoryTest extends IntegrationTest {
    private final String costProperty = dataset.costProperty;
    private final RelationshipType type = RelationshipType.withName(dataset.relationshipTypeName);

    public PaperGraphInMemoryTest() {
        super(of(), of(), of(), TestDataset.SEMINAR_PAPER);
        try (Transaction transaction = database().beginTx()) {
            int inse = new IndexerByEdgeDifference(dataset.relationshipTypeName, costProperty, transaction, INMEMORY).insertShortcuts();
            System.out.printf("%d shortCuts have been inserted%n", inse);
            transaction.commit();
        }
    }

    @ParameterizedTest
    @MethodSource("allSourceTarget")
    void testEachNodeToEveryOther(Number startNodeId, Number endNodeId) {
        try (Transaction transaction = database().beginTx()) {
            TreeSearch treeSearch = new TreeSearch(relationshipType(), costProperty);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra(new BasicEvaluationContext(transaction, database())).shortestPathWithShortcuts(start, end, type, costProperty);
            if (dijkstraPath != null) {
                WeightedPath chPath = treeSearch.find(start, end);
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight());

            }
        }
    }

    private static Stream<Arguments> allSourceTarget() {
        return IntStream.rangeClosed(0, 10)
                .mapToObj(i -> IntStream.rangeClosed(0, 10).mapToObj(j -> Arguments.of(i, j)))
                .flatMap(Function.identity());
    }
}
