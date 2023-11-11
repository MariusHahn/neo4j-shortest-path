package wtf.hahn.neo4j.cch.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.indexer.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.PathUtils;
import wtf.hahn.neo4j.util.StoppedResult;
import wtf.hahn.neo4j.util.importer.FileImporter;
import wtf.hahn.neo4j.util.importer.GrFileLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.List.of;


public class NewYorkGraphTest extends IntegrationTest {
    public static final Label LABEL = () -> "Location";
    private final String costProperty = dataset.costProperty;
    @TempDir private static Path tempPath = Paths.get(".");
    private final Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty);

    public NewYorkGraphTest() {
        super(of(), of(), of(), TestDataset.NO_IMPORT_FILE);
    }

    @BeforeAll
    void setup() throws IOException {
        FileImporter fileImporter = new FileImporter(new GrFileLoader(resourcePath().resolve("USA-road-t.NY.gr")), database());
        System.out.println("Start Import");
        fileImporter.importAllNodes();
        System.out.println("Start Import");
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

    //@ParameterizedTest
    @MethodSource("randomSourceTarget")
    void randomSourceTargetTest(Integer start, Integer goal) {
        try (Transaction transaction = database().beginTx()) {
            Node s = transaction.findNode(LABEL, "ROAD_rank", start);
            Node t = transaction.findNode(LABEL, "ROAD_rank", goal);
            Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty);
            StoppedResult<WeightedPath> weightedPath = new StoppedResult<>(() -> dijkstra.find(s, t));
            if (weightedPath.getResult() != null) {
                DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
                StoppedResult<SearchPath> result = new StoppedResult<>(() -> diskChDijkstra.find(start, goal));
                SearchPath cchPath = result.getResult();
                System.out.println(result.getMicros());
                System.out.println(SearchVertexPaths.toString(cchPath));
                System.out.println(weightedPath.getMicros());
                System.out.println(PathUtils.toRankString(weightedPath.getResult()));
                Assertions.assertEquals(weightedPath.getResult().weight(), cchPath.weight());
            }
        }
    }

    private static Stream<Arguments> randomSourceTarget() {
        final Random outerRandom = new Random(73);
        final int maxId = 264346;
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

    @Test
    void sourceTargetTest() {
        Stream<Arguments> argumentsStream = randomSourceTarget();
        AtomicLong overAll = new AtomicLong(0);
        AtomicInteger denominator = new AtomicInteger(0);
        try (Transaction transaction = database().beginTx();
             DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath))
        {
            argumentsStream.forEach(arg -> {
                int start = (int) arg.get()[0];
                int goal = (int) arg.get()[1];
                Node s = transaction.findNode(LABEL, "ROAD_rank", start);
                Node t = transaction.findNode(LABEL, "ROAD_rank", goal);
                StoppedResult<WeightedPath> weightedPath = new StoppedResult<>(() -> dijkstra.find(s, t));
                if (weightedPath.getResult() != null) {
                    StoppedResult<SearchPath> result = new StoppedResult<>(() -> diskChDijkstra.find(start, goal));
                    SearchPath cchPath = result.getResult();
                    System.out.println(result.getMicros() + " vs " + weightedPath.getMicros()+ " -> correct: " + (weightedPath.getResult().weight() == cchPath.weight()));
                    overAll.addAndGet(result.getMicros());
                    denominator.incrementAndGet();
                    Assertions.assertEquals(weightedPath.getResult().weight(), cchPath.weight());
                }
            });
            double avg = overAll.doubleValue() / denominator.doubleValue();
            System.out.printf("avg : %s with %d load invocations%n", avg, diskChDijkstra.totalLoadInvocations());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
