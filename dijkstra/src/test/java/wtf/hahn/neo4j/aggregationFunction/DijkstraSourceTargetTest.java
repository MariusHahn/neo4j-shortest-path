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

public class DijkstraSourceTargetTest extends IntegrationTest {


    public DijkstraSourceTargetTest() {
        super(of(), of(), of(DijkstraSourceTarget.class), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test void pathLengthTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = getCypher("A", "F", "length");
            Result result = transaction.execute(cypher);
            Assertions.assertEquals(160L, result.next().values().stream().findFirst().orElseThrow());
        }
    }
    @Test void pathLengthTest2() {
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

    @Test void printProcedures() {
        try (Transaction transaction = database().beginTx()) {
            Result result = transaction.execute("SHOW PROCEDURES  YIELD name, signature RETURN *");
            result.stream().flatMap(x -> x.entrySet().stream())
                    .forEach(stringObjectEntry -> System.out.printf("signature: %s\n",  stringObjectEntry.getValue()));
        }
    }


    //@Test
    void pathRelationshipsTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = getCypher("A", "F", "relationships");
            Result result = transaction.execute(cypher);
            result.next().values().stream().forEach(System.out::println);
        }
    }

    private static String getCypher(final String sourceNode, final String targetNode, final String aggFunction) {
        return """
                MATCH
                  (a:Location {name: '%s'}),
                  (b:Location {name: '%s'})
                WITH wtf.hahn.neo4j.aggregationFunction.sourceTarget(a, b, 'ROAD', 'cost') as path
                RETURN %s(path)
                """.formatted(sourceNode, targetNode, aggFunction);
    }
}
