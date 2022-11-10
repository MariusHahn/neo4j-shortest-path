package wtf.hahn.neo4j.aggregationFunction;

import static java.util.List.of;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import wtf.hahn.neo4j.util.IntegrationTest;

public class DijkstraSourceTargetTest extends IntegrationTest {


    public DijkstraSourceTargetTest() {
        super(of(), of(), of(DijkstraSourceTarget.class), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test void pathLengthTest() {
        try (Driver driver = driver()) {
            Session session = driver.session();
            String cypher = getCypher("A", "F", "length");
            Result result = session.run(cypher);
            Assertions.assertEquals(160, result.single().get(0).asInt());
        }
    }

    //@Test
    void pathRelationshipsTest() {
        try (Driver driver = driver()) {
            Session session = driver.session();
            String cypher = getCypher("A", "F", "relationships");
            Result result = session.run(cypher);
            result.single().fields().forEach(System.out::println);
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
