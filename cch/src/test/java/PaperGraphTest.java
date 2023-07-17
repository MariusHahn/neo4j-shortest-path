import static java.util.List.of;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.BufferManager;
import wft.hahn.neo4j.cch.storage.IndexStoreFunction;
import wft.hahn.neo4j.cch.storage.Mode;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class PaperGraphTest extends IntegrationTest {
    private final String costProperty = dataset.costProperty;

    public PaperGraphTest() {
        super(of(), of(), of(), TestDataset.SEMINAR_PAPER);
        /*try (Transaction transaction = database().beginTx()) {
            Vertex topNode = new IndexerByImportanceWithSearchGraph(
                    dataset.relationshipTypeName, costProperty, transaction
            ).insertShortcuts();
            try (val x = new IndexStoreFunction(topNode, Mode.OUT);
                 val y = new IndexStoreFunction(topNode, Mode.IN)) {
                x.go();
                y.go();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

         */
    }

    @Test
    void a() {
        DiskDijkstra dijkstra = new DiskDijkstra(new BufferManager(Mode.OUT));
        long start = System.currentTimeMillis();
        Map<Integer, SearchPath> paths = dijkstra.find(1);
        paths.forEach((rank, path) -> System.out.println(SearchVertexPaths.toString(path)));
        System.out.println(System.currentTimeMillis() - start);
    }

    @ParameterizedTest
    @MethodSource("allSourceTarget")
    void testEachNodeToEveryOther(Number startNodeId, Number endNodeId) {
        /*try (Transaction transaction = database().beginTx()) {
            BasicEvaluationContext context = new BasicEvaluationContext(transaction, database());
            NativeTreeSearch searcher = new NativeTreeSearch(context, relationshipType(), Shortcuts.rankPropertyName(relationshipType()), costProperty);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra(new BasicEvaluationContext(transaction, database())).shortestPathWithShortcuts(start, end, type, costProperty);
            if (dijkstraPath != null) {
                WeightedPath chPath = searcher.find(start, end);
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight());

            }
        }*/
        System.out.println("hello");
    }

    private static Stream<Arguments> allSourceTarget() {
        return IntStream.rangeClosed(0, 10)
                .mapToObj(i -> IntStream.rangeClosed(0, 10).mapToObj(j -> Arguments.of(i, j)))
                .flatMap(Function.identity());
    }
}
