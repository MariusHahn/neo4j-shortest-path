package wtf.hahn.neo4j.procedure;

import static java.util.List.of;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.IntegrationTest;

public class ContractionHierarchiesTest extends IntegrationTest {

    public ContractionHierarchiesTest() {
        super(of(), of(ContractionHierarchies.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void a() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = "CALL wtf.hahn.neo4j.procedure.createContractionHierarchiesIndex('ROAD', 'cost')";
            transaction.execute(cypher);
            transaction.getAllNodes().stream().sorted(Comparator.comparingInt(o -> (Integer) o.getProperty("ROAD_rank"))).forEach(node -> {
                String name = (String) node.getProperty("name");
                Integer roadRank = (Integer) node.getProperty("ROAD_rank");
                System.out.printf("Node: %s | rank: %s%n", name, roadRank);
            });
            transaction.getAllRelationships().forEach( relationship -> {
                String start = (String) relationship.getStartNode().getProperty("name");
                String end = (String) relationship.getEndNode().getProperty("name");
                String name = relationship.getType().name();
                Number cost = (Number) relationship.getProperty("cost");
                System.out.printf("(%s)-[%s]->(%s) | cost: %.2f \n", start, name, end, cost.doubleValue());
            });
        }
    }
}
