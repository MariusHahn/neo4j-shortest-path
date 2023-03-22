package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BasicEvaluationContext;
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
                new ContractionHierarchiesIndexerByEdgeDifference(edgeLabel, costProperty, transaction,
                        database()).insertShortcuts();
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
            WeightedPath dijkstraPath = new NativeDijkstra(new BasicEvaluationContext(
                    transaction, database()
            )).shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath =
                        (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty).findFirst().get().path;
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight());
            }
        }
    }


    ///@Test
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
            WeightedPath dijkstraPath = new NativeDijkstra(new BasicEvaluationContext(transaction, database()))
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

    @Test
    void randomPaths() throws IOException {
        Random random = new Random(1);
        Path file = Files.createFile(Paths.get(".", "measureQueryPerformance.csv"));
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
            bufferedWriter.append("From,To,Dijkstra,dijkstraLength,chLength,weight,dijkstraTime,cchTime\n");
            random.ints(200, 1, 3353)
                    .mapToObj(i -> random.ints(200, 1,3353).mapToObj(j -> new int[]{i, j}))
                    .flatMap(Function.identity())
                    .parallel()
                    .forEach(x -> searchPerformanceTest(x[0], x[1], bufferedWriter));
        }

    }


    void searchPerformanceTest(Integer startNodeId, Integer endNodeId, BufferedWriter writer) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchies chFinder = new ContractionHierarchies(database(), transaction);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            TimeResult<WeightedPath> dijkstraPath = stoppedResult(() -> new NativeDijkstra(new BasicEvaluationContext(transaction, database())).shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty()));
            if (dijkstraPath.result != null) {
                TimeResult<WeightedPath> chPath = stoppedResult(() -> (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty).findFirst().get().path);
                Assertions.assertEquals(dijkstraPath.result.weight(), chPath.result.weight());
                String log = "%d,%d,%d,%d,%f,%d,%d%n".formatted(
                                startNodeId
                                , endNodeId
                                , dijkstraPath.result.length()
                                , chPath.result.length()
                                , dijkstraPath.result.weight()
                                , dijkstraPath.executionTime
                                , chPath.executionTime
                );
                writer.append(log);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    record TimeResult<T>(Long executionTime, T result) {}

    private static <T> TimeResult<T> stoppedResult(Supplier<T> s) {
        long start = System.currentTimeMillis();
        T result = s.get();
        return new TimeResult<>(System.currentTimeMillis() - start, result);
    }
}
