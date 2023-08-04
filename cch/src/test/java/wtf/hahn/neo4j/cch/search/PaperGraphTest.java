package wtf.hahn.neo4j.cch.search;

import static java.util.List.of;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.BidirectionalSearchPath;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.IndexStoreFunction;
import wft.hahn.neo4j.cch.storage.Mode;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class PaperGraphTest extends IntegrationTest {
    private final String costProperty = dataset.costProperty;
    private final RelationshipType type = RelationshipType.withName(dataset.relationshipTypeName);
    @TempDir private static Path tempPath;

    public PaperGraphTest() {
        super(of(), of(), of(), TestDataset.SEMINAR_PAPER);
    }

    @BeforeAll
    void setup() throws IOException {
        try (Transaction transaction = database().beginTx()) {
            Vertex topVertex = new IndexerByImportanceWithSearchGraph(
                    dataset.relationshipTypeName, costProperty, transaction
            ).insertShortcuts();
            try (IndexStoreFunction storeOutF = new IndexStoreFunction(topVertex, Mode.OUT, tempPath);
                 IndexStoreFunction storeInF = new IndexStoreFunction(topVertex, Mode.IN, tempPath)
            ) {
                storeInF.go();
                storeOutF.go();
            }
        }
    }

    @ParameterizedTest @MethodSource("allSourceTarget")
    void a(Integer start, Integer goal) {
        DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
        BidirectionalSearchPath searchPath = (BidirectionalSearchPath) diskChDijkstra.find(start, goal);
        System.out.println(SearchVertexPaths.toString(searchPath));
    }

    private static Stream<Arguments> allSourceTarget() {
        return IntStream.rangeClosed(0, 10)
                .mapToObj(i -> IntStream.rangeClosed(0, 10).mapToObj(j -> Arguments.of(i, j)))
                .flatMap(Function.identity());
    }
}
