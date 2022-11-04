package wtf.hahn.neo4j.aggregationFunction;

import static java.util.List.of;
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
            String cypher =
                    "MATCH \n" +
                    "  (a:Location {name: 'A'}),\n" +
                    "  (b:Location {name: 'F'}) \n" +
                    " WITH wtf.hahn.neo4j.aggregationFunction.sourceTarget(a, b, 'ROAD', 'cost') as path \n" +
                    " RETURN length(path) //, relationships(path) \n";

            Result result = session.run(cypher);
            result.single().fields().forEach(System.out::println);
        }
    }

    //@Test
    void pathRelationshipsTest() {
        try (Driver driver = driver()) {
            Session session = driver.session();
            String cypher =
                    "MATCH \n" +
                    "  (a:Location {name: 'A'}),\n" +
                    "  (b:Location {name: 'F'}) \n" +
                    " WITH wtf.hahn.neo4j.aggregationFunction.sourceTarget(a, b, 'ROAD', 'cost') as path \n" +
                    " RETURN relationships(path) \n";

            Result result = session.run(cypher);
            result.single().fields().forEach(System.out::println);
        }
    }
}
