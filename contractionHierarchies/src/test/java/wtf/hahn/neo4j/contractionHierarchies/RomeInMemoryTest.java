package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.testUtil.IntegrationTest;


public class RomeInMemoryTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;

    public RomeInMemoryTest() {
        super(of(), of(), of(), TestDataset.ROME);
        try (Transaction transaction = database().beginTx()) {
            Comparator<Node> comparator = Comparator.comparingInt(Node::getDegree);
            System.err.println("Start contracting");
            logTime(() -> {
                new ContractionHierarchiesIndexerInMem(edgeLabel, costProperty, transaction,
                        comparator).insertShortcuts();
                return Void.TYPE;
            });
            System.err.println("End contracting");
            transaction.commit();
        }
    }

    private static Stream<Arguments> someArguments() throws IOException {
        return Files.lines(Paths.get("src", "test", "resources", "testRomeEdgeCases.csv"))
                .skip(1)
                .map(line -> line.split(","))
                .map(line -> Arguments.of(Integer.valueOf(line[0].trim()), Integer.valueOf(line[1].trim())));
    }

    @ParameterizedTest
    @MethodSource("someArguments")
    void newBidirectionalDijkstra(Integer startNodeId, Integer endNodeId) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchies chFinder = new ContractionHierarchies(database(), transaction);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = logTime(() -> new NativeDijkstra().shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty()));
            if (dijkstraPath != null) {
                WeightedPath chPath =
                        logTime(() -> (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty).findFirst().get().path);
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight());
            }
        }
    }


    //@Test
    void sourceTargetToCsv() throws IOException {
        Path file = Files.createFile(Paths.get(".", "failing.txt"));
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
            bufferedWriter.append("From,To,Dijkstra,CH,length,chLength\n");
            IntStream.rangeClosed(1, 3353)
                    .mapToObj(i -> IntStream.rangeClosed(i, 3353).mapToObj(j -> new int[] {i, j}))
                    .flatMap(Function.identity())
                    .parallel()
                    .forEach(x -> sourceTargetToFile(x[0], x[1], bufferedWriter));
        }
    }

    void sourceTargetToFile(Integer startNodeId, Integer endNodeId, BufferedWriter writer) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchies chFinder = new ContractionHierarchies(database(), transaction);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra()
                    .shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath = (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty).findFirst().get().path;
                double chWeight = chPath == null ? -1.0 : chPath.weight();
                long chLength = chPath == null ? Long.MAX_VALUE : chPath.length();
                if (chWeight != dijkstraPath.weight()) {
                    writer.append("%d,%d,%f,%f,%d,%d\n".formatted(startNodeId, endNodeId, dijkstraPath.weight(), chWeight,
                            dijkstraPath.length(), chLength));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static <T> T logTime(Supplier<T> s ) {
        long start = System.currentTimeMillis();
        T r = s.get();
        System.out.printf("%s%n", System.currentTimeMillis() - start);
        return r;
    }
}
