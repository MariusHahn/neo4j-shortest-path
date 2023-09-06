package wtf.hahn.neo4j.cch.search;

import static java.util.List.of;
import static wtf.hahn.neo4j.util.EntityHelper.getLongProperty;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class PaperGraphTest extends IntegrationTest {
    private final String costProperty = dataset.costProperty;
    private final RelationshipType type = RelationshipType.withName(dataset.relationshipTypeName);
    @TempDir private static Path tempPath;

    public PaperGraphTest() {
        super(of(), of(), of(), TestDataset.SEMINAR_PAPER);
    }

    @BeforeAll
    void setup() throws Exception {
        try (Transaction transaction = database().beginTx()) {
            Vertex topVertex = new IndexerByImportanceWithSearchGraph(
                    dataset.relationshipTypeName, costProperty, transaction
            ).insertShortcuts();
            try (StoreFunction storeOutF = new StoreFunction(topVertex, Mode.OUT, tempPath);
                 StoreFunction storeInF = new StoreFunction(topVertex, Mode.IN, tempPath)
            ) {
                storeInF.go();
                storeOutF.go();
            }
        }
    }

    @ParameterizedTest @MethodSource("allSourceTarget")
    void a(Integer start, Integer goal) {
        try (Transaction transaction = database().beginTx()) {
            Dijkstra dijkstra = new Dijkstra(relationshipType(), "cost");
            Node s = transaction.findNode(() -> "Location", "id", start);
            Node t = transaction.findNode(() -> "Location", "id", goal);
            WeightedPath weightedPath = dijkstra.find(s, t);
            if (weightedPath != null) {
                System.out.println(weightedPath.weight());
                DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
                SearchPath chPath = diskChDijkstra.find(
                        (int) getLongProperty(s, "ROAD_rank")
                        , (int) getLongProperty(t, "ROAD_rank")
                );
                System.out.println(SearchVertexPaths.toString(chPath));
                Assertions.assertEquals(weightedPath.weight(), chPath.weight());
            }
        }
    }

    private static Stream<Arguments> allSourceTarget() {
        return IntStream.rangeClosed(0, 10)
                .mapToObj(i -> IntStream.rangeClosed(0, 10).mapToObj(j -> Arguments.of(i, j)))
                .flatMap(Function.identity());
    }
}
