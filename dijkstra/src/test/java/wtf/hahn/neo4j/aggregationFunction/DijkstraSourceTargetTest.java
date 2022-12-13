package wtf.hahn.neo4j.aggregationFunction;

import static java.util.List.of;
import static org.neo4j.graphdb.Direction.OUTGOING;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.IntegrationTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DijkstraSourceTargetTest extends IntegrationTest {


    public DijkstraSourceTargetTest() {
        super(of(), of(DijkstraSourceTarget.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test void pathLengthTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = getCypher("A", "F", "pathCost");
            System.out.println(cypher);
            Result result = transaction.execute(cypher);
            Long pathCost = (Long) result.next().values().stream().findFirst().orElseThrow();
            Assertions.assertEquals(160L, pathCost);
            System.out.printf("Result: %s%n", pathCost);
        }
    }

    @Test void pathNodeTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = getCypher("A", "F", "[n in nodes(path) | n.name] as name");
            System.out.println(cypher);
            Result result = transaction.execute(cypher);
            String[] values = ((List<String>) result.next().get("name")).toArray(String[]::new);
            Assertions.assertArrayEquals(values, new String[]{"A", "C", "D", "E", "F"});
            System.out.printf("Result: [%s]%n", Arrays.stream(values).collect(Collectors.joining(", ")));
        }
    }

    @Test void pathLengthTestInternal() {
        try (Transaction transaction1 = database().beginTx()) {
            Node start = transaction1.findNode(() -> "Location", "name", "A");
            Node end = transaction1.findNode(() -> "Location", "name", "F");
            RelationshipType type = RelationshipType.withName("ROAD");
            PathExpander<Double> expander = PathExpanders.forTypeAndDirection(type, OUTGOING);
            PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(expander, "cost", 1);
            WeightedPath singlePath = pathFinder.findSinglePath(start, end);
            System.out.println(singlePath.weight());
            transaction1.rollback();
        }
    }

    static String getCypher(final String sourceNode, final String targetNode, final String returnClause) {
        return """
                MATCH (a:Location {name: '%s'}), (b:Location {name: '%s'})
                CALL wtf.hahn.neo4j.aggregationFunction.sourceTarget(a, b, 'ROAD', 'cost')
                YIELD pathCost, path
                RETURN %s
                """.formatted(sourceNode, targetNode, returnClause).stripIndent();
    }
}
