package wtf.hahn.dijkstra;

import static java.util.List.of;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.neo4j.graphdb.Direction.OUTGOING;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class DijkstraTest extends IntegrationTest {

    private final Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty());

    public DijkstraTest() {
        super(of(), of(), of(), TestDataset.OLDENBURG);
    }

    private static Stream<Arguments> fromTheoPaths() throws IOException {
        return Files.lines(IntegrationTest.resourcePath().resolve("oldenburg-st.csv"))
                .map(line -> Arrays.stream(line.split(",")).map(Integer::valueOf).toArray(Integer[]::new))
                .map(ids -> Arguments.of(ids[0], ids[1]));
    }

    @ParameterizedTest
    @MethodSource({"fromTheoPaths",})
    void testNodeIdToNodeIdFind(Integer s, Integer t) {
        try (Transaction transaction = database().beginTx()) {
            NativeDijkstra nativeDijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, database()));
            Node start = transaction.findNode(() -> "Location", "id", s);
            Node end = transaction.findNode(() -> "Location", "id", t);
            PathExpander<Double> standardExpander = PathExpanders.forTypeAndDirection(relationshipType(), OUTGOING);
            WeightedPath dijkstraPath = nativeDijkstra.shortestPath(start, end, standardExpander, costProperty());
            if (dijkstraPath != null) {
                ShortestPathResult chPath = dijkstra.find(start, end);
                Assertions.assertNotNull(chPath);
                Assertions.assertEquals(
                        dijkstraPath.weight()
                        , chPath.weight()
                        , "%s%n%s".formatted(dijkstraPath.toString(), chPath.toString())
                );
            }
        }
    }
    @Test
    void testOneToManyDijkstra() throws IOException {
        Set<Integer> ids = Files.lines(IntegrationTest.resourcePath().resolve("oldenburg-st.csv"))
                .map(line -> Arrays.stream(line.split(",")).map(Integer::valueOf).toArray(Integer[]::new))
                .flatMap(Arrays::stream).collect(toSet());
        try (Transaction transaction = database().beginTx()) {
            Node start = transaction.findNode(() -> "Location", "id", 1);
            Set<Node> goals = ids.stream().map(id -> transaction.findNode(() -> "Location", "id", id)).collect(toSet());
            PathExpander<Double> standardExpander = PathExpanders.forTypeAndDirection(relationshipType(), OUTGOING);
            Map<Node, ShortestPathResult> shortestPaths = dijkstra.find(start, goals, standardExpander);
            NativeDijkstra nativeDijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, database()));
            for (Node goal : goals) {
                WeightedPath dijkstraPath = nativeDijkstra.shortestPath(start, goal, standardExpander, costProperty());
                if (dijkstraPath != null) {
                    ShortestPathResult shortestPath = shortestPaths.get(goal);
                    Assertions.assertNotNull(shortestPath);
                    Assertions.assertEquals(
                            dijkstraPath.weight()
                            , shortestPath.weight()
                            , "%s%n%s".formatted(dijkstraPath.toString(), shortestPath.toString())
                    );
                }

            }
        }
    }
}
