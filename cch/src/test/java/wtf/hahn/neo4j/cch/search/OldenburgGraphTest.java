package wtf.hahn.neo4j.cch.search;

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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.indexer.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.storage.FifoBuffer;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wft.hahn.neo4j.cch.update.Updater;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.PathUtils;
import wtf.hahn.neo4j.util.StoppedResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.List.of;
import static wtf.hahn.neo4j.cch.search.DiskChDijkstraTest.setupPaperGraphTest;
import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

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

    @Test
    void smallPerformanceTest() {
        Stream<Arguments> argumentsStream = randomSourceTarget();
        AtomicLong overAll = new AtomicLong(0);
        AtomicInteger denominator = new AtomicInteger(0);
        FifoBuffer outBuffer = new FifoBuffer(18000, Mode.OUT, tempPath);
        FifoBuffer inBuffer = new FifoBuffer(18000, Mode.IN, tempPath);
        try (Transaction transaction = database().beginTx();
             DiskChDijkstra diskChDijkstra = new DiskChDijkstra(outBuffer, inBuffer)) {
            argumentsStream.forEach(arg -> {
                int start = (int) arg.get()[0];
                int goal = (int) arg.get()[1];
                Node s = transaction.findNode(LABEL, "ROAD_rank", start);
                Node t = transaction.findNode(LABEL, "ROAD_rank", goal);
                WeightedPath weightedPath = dijkstra.find(s, t);
                if (weightedPath != null) {
                    StoppedResult<SearchPath> result = new StoppedResult<>(() -> diskChDijkstra.find(start, goal));
                    SearchPath cchPath = result.getResult();
                    System.out.println(result.getMicros());
                    overAll.addAndGet(result.getMicros());
                    denominator.incrementAndGet();
                    Assertions.assertEquals(weightedPath.weight(), cchPath.weight());
                }
            });
            double avg = overAll.doubleValue() / denominator.doubleValue();
            System.out.printf("avg : %s with %d load invocations%n", avg, diskChDijkstra.loadInvocations());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    void randomChangeTest() {
        Transaction transaction = database().beginTx();

        // When
        List<Relationship> relationships = transaction.findRelationships(() -> "ROAD").stream().collect(Collectors.toList());
        Collections.shuffle(relationships);
        for (int i = 0; i < relationships.size(); i++) {
            Relationship relationship = relationships.get(i);
            double current = getDoubleProperty(relationship, "cost");
            if (i % 2 == 0) relationship.setProperty("cost", 1);
            else relationship.setProperty("cost", 2);
            relationship.setProperty("changed", true);
        }

        //Then
        Updater updater = new Updater(transaction, tempPath);
        setupPaperGraphTest(updater.update(), tempPath);

        //Assert That
        AtomicLong overAll = new AtomicLong(0);
        AtomicInteger denominator = new AtomicInteger(0);
        FifoBuffer outBuffer = new FifoBuffer(18000, Mode.OUT, tempPath);
        FifoBuffer inBuffer = new FifoBuffer(18000, Mode.IN, tempPath);
        try (DiskChDijkstra diskChDijkstra = new DiskChDijkstra(outBuffer, inBuffer)) {
            randomSourceTarget().forEach(arg -> {
                int start = (int) arg.get()[0];
                int goal = (int) arg.get()[1];
                Node s = transaction.findNode(LABEL, "ROAD_rank", start);
                Node t = transaction.findNode(LABEL, "ROAD_rank", goal);
                WeightedPath weightedPath = dijkstra.find(s, t);
                if (weightedPath != null) {
                    StoppedResult<SearchPath> result = new StoppedResult<>(() -> diskChDijkstra.find(start, goal));
                    SearchPath cchPath = result.getResult();
                    System.out.println(result.getMicros());
                    overAll.addAndGet(result.getMicros());
                    denominator.incrementAndGet();
                    if (weightedPath.weight() != cchPath.weight()) {
                        System.out.println(PathUtils.toRankString(weightedPath));
                        System.out.println(SearchVertexPaths.toString(cchPath));
                    }
                    Assertions.assertEquals(weightedPath.weight(), cchPath.weight());
                }
            });
            double avg = overAll.doubleValue() / denominator.doubleValue();
            System.out.printf("avg : %s with %d load invocations%n", avg, diskChDijkstra.loadInvocations());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    void allSourceTargetTest() throws IOException {
        final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("fail.csv", true));
        final int maxId = 6104;
        bufferedWriter.append("from,to,dijkstraLength,cchLength\n");
        IntStream.rangeClosed(0, maxId).parallel().forEach(from -> {
            try (Transaction transaction = database().beginTx()) {
                for (int to = 0; to <= maxId; to++) if (from != to) {
                    Node s = transaction.findNode(LABEL, "ROAD_rank", from);
                    Node t = transaction.findNode(LABEL, "ROAD_rank", to);
                    WeightedPath dijkstraPath = dijkstra.find(s, t);
                    if (dijkstraPath != null) {
                        DiskChDijkstra diskChDijkstra = new DiskChDijkstra(tempPath);
                        SearchPath chPath = diskChDijkstra.find(from, to);
                        if (chPath == null || chPath.weight() != dijkstraPath.weight()) {
                            String chWeight = chPath == null ? "null" : Integer.toString(chPath.weight());
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
