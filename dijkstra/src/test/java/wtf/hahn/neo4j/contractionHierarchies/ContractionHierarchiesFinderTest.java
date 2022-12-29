package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;
import static wtf.hahn.neo4j.contractionHierarchies.ProcedureTest.sourceTargetAtoFQuery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterables;
import wtf.hahn.neo4j.util.IntegrationTest;

import java.util.Map;
import java.util.stream.Collectors;

public class ContractionHierarchiesFinderTest extends IntegrationTest {

    public ContractionHierarchiesFinderTest() {
        super(of(), of(Procedure.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void sourceTargetCypher() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = "CALL wtf.hahn.neo4j.contractionHierarchies.createContractionHierarchiesIndex('ROAD', 'cost')";
            transaction.execute(cypher);
            Map<String, Object> result = transaction.execute(sourceTargetAtoFQuery()).next();
            Double pathCost = ((Double) result.get("pathCost"));
            WeightedPath path = ((WeightedPath) result.get("path"));
            Assertions.assertEquals(160.0, pathCost);
            String names = Iterables.stream(path.nodes())
                    .map(n -> n.getProperty("name").toString())
                    .collect(Collectors.joining(","));
            System.out.println(names);
            System.out.printf("Result: %s%n", pathCost);
            transaction.rollback();
        }
    }
}
