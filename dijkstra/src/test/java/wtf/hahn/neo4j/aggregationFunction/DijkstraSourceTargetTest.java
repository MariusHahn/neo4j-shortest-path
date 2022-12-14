package wtf.hahn.neo4j.aggregationFunction;

import static java.util.List.of;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.IntegrationTest;

public class DijkstraSourceTargetTest extends IntegrationTest {


    public DijkstraSourceTargetTest() {
        super(of(), of(DijkstraSourceTarget.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }
    @ParameterizedTest
    @ValueSource(strings = {
            "wtf.hahn.neo4j.aggregationFunction.sourceTarget",
            "wtf.hahn.neo4j.aggregationFunction.sourceTargetNative",
    })
    void pathLengthTest(String functionName) {
        try (Transaction transaction = database().beginTx()) {
            String cypher = getCypher(functionName, "pathCost");
            System.out.println(cypher);
            Result result = transaction.execute(cypher);
            Double pathCost = (Double) result.next().values().stream().findFirst().orElseThrow();
            Assertions.assertEquals(160.0, pathCost);
            System.out.printf("Result: %s%n", pathCost);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "wtf.hahn.neo4j.aggregationFunction.sourceTarget",
            "wtf.hahn.neo4j.aggregationFunction.sourceTargetNative",
    })
    void pathNodeTest(String functionName ) {
        try (Transaction transaction = database().beginTx()) {
            String cypher = getCypher(functionName, "[n in nodes(path) | n.name] as name");
            System.out.println(cypher);
            Result result = transaction.execute(cypher);
            String[] values = ((List<String>) result.next().get("name")).toArray(String[]::new);
            Assertions.assertArrayEquals(values, new String[]{"A", "C", "D", "E", "F"});
            System.out.printf("Result: [%s]%n", String.join(", ", values));
        }
    }

    static String getCypher(final String functionName, final String returnClause) {
        return """
                MATCH (a:Location {name: 'A'}), (b:Location {name: 'F'})
                CALL %s(a, b, 'ROAD', 'cost')
                YIELD pathCost, path
                RETURN %s
                """.formatted(functionName, returnClause).stripIndent();
    }
}
