package wtf.hahn.neo4j.cch.search;

import static java.util.List.of;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class OldenburgGraphTest extends IntegrationTest {
    public static final Label LABEL = () -> "Location";
    private final String costProperty = dataset.costProperty;
    @TempDir
    private static Path tempPath;
    private final Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty);

    public OldenburgGraphTest() {
        super(of(), of(), of(), TestDataset.OLDENBURG);
    }

    @BeforeAll
    void setup() {
        try (Transaction transaction = database().beginTx()) {
            Vertex topVertex =
                    new IndexerByImportanceWithSearchGraph(dataset.relationshipTypeName, costProperty, transaction
                    ).insertShortcuts();
            try (StoreFunction storeOutF = new StoreFunction(topVertex, Mode.OUT, tempPath);
                 StoreFunction storeInF = new StoreFunction(topVertex, Mode.IN, tempPath)
            ) {
                storeInF.go();
                storeOutF.go();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("randomSourceTarget")
    void randomSourceTargetTest(Integer start, Integer goal) {
        try (Transaction transaction = database().beginTx()) {
            Node s = transaction.findNode(LABEL, "ROAD_rank", start);
            Node t = transaction.findNode(LABEL, "ROAD_rank", goal);
            WeightedPath weightedPath = dijkstra.find(s, t);
            if (weightedPath != null) {
                DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
                SearchPath cchPath = diskChDijkstra.find(start, goal);
                Assertions.assertEquals(weightedPath.weight(), cchPath.weight());
            }
        }
    }

    private static Stream<Arguments> randomSourceTarget() {
        final Random outerRandom = new Random(73);
        final int maxId = 6104;
        final int sourceTargetTestsSquared = 100;
        return IntStream.rangeClosed(0, sourceTargetTestsSquared)
                .map(i -> outerRandom.nextInt(maxId + 1))
                .mapToObj(i -> {
                            final Random innerRandom = new Random(37);
                            return IntStream.rangeClosed(0, sourceTargetTestsSquared)
                                    .map(j -> innerRandom.nextInt(maxId + 1))
                                    .filter(j -> i != j)
                                    .mapToObj(j -> Arguments.of(i, j));
                        }
                )
                .flatMap(Function.identity());
    }

    //@Test
    void allSourceTargetTest() throws IOException {
        final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("fail.csv", true));
        final int maxId = 6104;
        bufferedWriter.append("from,to,dijkstraLength,cchLength\n");
        IntStream.rangeClosed(0, maxId).parallel().forEach(from -> {
            try (Transaction transaction = database().beginTx()) {
                for (int to = 0; to < maxId; to++) if (from != to) {
                    Node s = transaction.findNode(LABEL, "ROAD_rank", from);
                    Node t = transaction.findNode(LABEL, "ROAD_rank", to);
                    WeightedPath dijkstraPath = dijkstra.find(s, t);
                    if (dijkstraPath != null) {
                        DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
                        SearchPath chPath = diskChDijkstra.find(from, to);
                        if (chPath == null || chPath.weight() != dijkstraPath.weight()) {
                            String chWeight = chPath == null ? "null" : Float.toString(chPath.weight());
                            append(bufferedWriter, "%d,%d,%.2f,%s%n".formatted(from, to, dijkstraPath.weight(), chWeight));
                        }
                    }
                }
            }
        });
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    private void append(BufferedWriter writer, String s) {
        try {
            writer.append(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
