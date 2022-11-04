package wtf.hahn.neo4j.aggregationFunction;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import wtf.hahn.neo4j.util.IntegrationTest;

import static java.util.List.of;

public class DijkstraSourceTargetTest extends IntegrationTest {


    public DijkstraSourceTargetTest() {
        super(of(), of(), of(DijkstraSourceTarget.class), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test void pathLengthTest() {
        try (Driver driver = GraphDatabase.driver(uri, build)) {
            var session = driver.session();
            var cypher =
                    "MATCH \n" +
                    "  (a:Location {name: 'A'}),\n" +
                    "  (b:Location {name: 'F'}) \n" +
                    " WITH wtf.hahn.neo4j.aggregationFunction.sourceTarget(a, b, 'ROAD', 'cost') as path \n" +
                    " RETURN length(path) //, relationships(path) \n";

            var result = session.run(cypher);
            result.single().fields().forEach(System.out::println);
        }
    }

    //@Test
    void pathRelationshipsTest() {
        try (Driver driver = GraphDatabase.driver(uri, build)) {
            var session = driver.session();
            var cypher =
                    "MATCH \n" +
                    "  (a:Location {name: 'A'}),\n" +
                    "  (b:Location {name: 'F'}) \n" +
                    " WITH wtf.hahn.neo4j.aggregationFunction.sourceTarget(a, b, 'ROAD', 'cost') as path \n" +
                    " RETURN relationships(path) \n";

            var result = session.run(cypher);
            result.single().fields().forEach(System.out::println);
        }
    }
}
