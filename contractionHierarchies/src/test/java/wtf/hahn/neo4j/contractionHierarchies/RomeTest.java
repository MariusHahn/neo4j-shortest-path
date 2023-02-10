package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.function.Function;
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
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.testUtil.IntegrationTest;


public class RomeTest extends IntegrationTest {

    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;

    public RomeTest() {
        super(of(), of(), of(), TestDataset.ROME);
        try (Transaction transaction = database().beginTx()) {
            Comparator<Node> comparator = Comparator.comparingInt(Node::getDegree);
            new ContractionHierarchiesIndexer(edgeLabel, costProperty, transaction, comparator).insertShortcuts();
            transaction.commit();
        }
    }

    private static Stream<Arguments> someArguments() throws IOException {
        return Files.lines(Paths.get("src", "test", "resources", "ForRomeToTest.csv"))
                .skip(1)
                .map(line -> line.split(","))
                .map(line -> Arguments.of(Integer.valueOf(line[0].trim()), Integer.valueOf(line[1].trim())));
    }

    @ParameterizedTest
    @MethodSource("someArguments")
    void sourceTargetSamples(Integer startNodeId, Integer endNodeId) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchiesFinder chFinder =
                    new ContractionHierarchiesFinder(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra().shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath = chFinder.find(start, end, RelationshipType.withName(edgeLabel), costProperty);
                Assertions.assertEquals(dijkstraPath.weight(), chPath.weight());
            }
        }
    }


    @Test
    void sourceTargetToCsv() throws IOException {
        Path file = Files.createFile(Paths.get(".", "failing.txt"));
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
            bufferedWriter.append("From,To,Dijkstra,CH,length\n");
            IntStream.rangeClosed(1, 3353)
                    .mapToObj(i -> IntStream.rangeClosed(i, 3353).mapToObj(j -> new int[] {i, j}))
                    .flatMap(Function.identity())
                    .parallel()
                    .forEach(x -> sourceTargetToFile(x[0], x[1], bufferedWriter));
        }
    }

    void sourceTargetToFile(Integer startNodeId, Integer endNodeId, BufferedWriter writer) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchiesFinder chFinder =
                    new ContractionHierarchiesFinder(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            WeightedPath dijkstraPath = new NativeDijkstra()
                    .shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty());
            if (dijkstraPath != null) {
                WeightedPath chPath = chFinder.find(start, end, RelationshipType.withName(edgeLabel), costProperty);
                double chWeight = chPath == null ? -1.0 : chPath.weight();
                if (chWeight != dijkstraPath.weight()) {
                    writer.append("%d,%d,%f,%f,%d\n".formatted(startNodeId, endNodeId, dijkstraPath.weight(), chWeight,
                            dijkstraPath.length()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}