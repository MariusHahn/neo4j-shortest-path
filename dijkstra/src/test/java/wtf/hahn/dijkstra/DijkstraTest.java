package wtf.hahn.dijkstra;

import static java.util.List.of;
import static org.neo4j.graphdb.Direction.OUTGOING;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
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
    private static Stream<Arguments> failing() {
        return Stream.of(
                Arguments.of(3086, 2880)
                , Arguments.of(4162,500)
        );
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
}
