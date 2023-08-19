package wtf.hahn.neo4j.cch.search;

import static java.util.List.of;
import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
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

public class OldenburgGraphTest extends IntegrationTest {
    public static final Label LABEL = () -> "Location";
    private final String costProperty = dataset.costProperty;
    @TempDir private static Path tempPath;

    public OldenburgGraphTest() {
        super(of(), of(), of(), TestDataset.OLDENBURG);
    }

    @BeforeAll
    void setup() {
        try (Transaction transaction = database().beginTx()) {
            long start = System.currentTimeMillis();
            Vertex topVertex =
                    new IndexerByImportanceWithSearchGraph(dataset.relationshipTypeName, costProperty, transaction
                    ).insertShortcuts();
            System.out.println(System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            try (StoreFunction storeOutF = new StoreFunction(topVertex, Mode.OUT, tempPath);
                 StoreFunction storeInF = new StoreFunction(topVertex, Mode.IN, tempPath)
            ) {
                storeInF.go();
                storeOutF.go();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    @ParameterizedTest
    @MethodSource("allSourceTarget")
    void a(Integer start, Integer goal) {
        try (Transaction transaction = database().beginTx()) {
            Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty);
            Node s = transaction.findNode(LABEL, "ROAD_rank", start);
            Node t = transaction.findNode(LABEL, "ROAD_rank", goal);
            System.out.printf("from: (%s) to: (%s)\n", getProperty(s, "name"), getProperty(t, "name"));
            WeightedPath weightedPath = dijkstra.find(s, t);
            if (weightedPath != null) {
                System.out.printf("(%s)-[%.2f]->(%s)", getProperty(s, "name"), weightedPath.weight(), getProperty(t, "name"));
                DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
                SearchPath chPath = diskChDijkstra.find(start, goal);
                System.out.println(SearchVertexPaths.toString(chPath));
                Assertions.assertEquals(weightedPath.weight(), chPath.weight());
            }
        }
    }

    private static Stream<Arguments> allSourceTarget() {
        return IntStream.range(0, 2)
                .mapToObj(i -> IntStream.range(0, 2).filter(j -> i != j).mapToObj(j -> Arguments.of(i, j)))
                .flatMap(Function.identity());
    }


}
