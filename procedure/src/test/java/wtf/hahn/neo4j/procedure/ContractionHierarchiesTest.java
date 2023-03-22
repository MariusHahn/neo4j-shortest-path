package wtf.hahn.neo4j.procedure;

import static java.util.List.of;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.testUtil.ShortcutTriple;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.EntityHelper;
import wtf.hahn.neo4j.util.Iterables;

public class ContractionHierarchiesTest extends IntegrationTest {

    public ContractionHierarchiesTest() {
        super(of(), of(CHProcedures.class), of(), TestDataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void chStandardContractionOrderTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = "CALL wtf.hahn.neo4j.procedure.createContractionHierarchiesIndex('ROAD', 'cost')";
            transaction.execute(cypher);
            Set<ShortcutTriple> shortcuts = transaction.getAllRelationships().stream()
                    .filter(Shortcuts::isShortcut)
                    .map(ShortcutTriple::new)
                    .peek(System.out::println)
                    .collect(Collectors.toSet());
            Assertions.assertTrue(2 > shortcuts.size());
            transaction.rollback();
        }
    }

    @Test
    void sourceTargetCypher() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = "CALL wtf.hahn.neo4j.procedure.createContractionHierarchiesIndex('ROAD', 'cost')";
            transaction.execute(cypher);
            Map<String, Object> result = transaction.execute(sourceTargetAtoFQuery()).next();
            Double pathCost = ((Double) result.get("pathCost"));
            WeightedPath path = ((WeightedPath) result.get("path"));
            Assertions.assertEquals(160.0, pathCost);
            String names = Iterables.stream(path.nodes())
                    .map(EntityHelper::getNameProperty)
                    .collect(Collectors.joining(","));
            System.out.println(names);
            System.out.printf("Result: %s%n", pathCost);
            transaction.rollback();
        }
    }

    static String sourceTargetAtoFQuery() {
        return """
                MATCH (a:Location {name: 'A'}), (b:Location {name: 'F'})
                CALL wtf.hahn.neo4j.procedure.sourceTargetCH(a, b, 'ROAD', 'cost')
                YIELD pathCost, path
                RETURN pathCost, path
                """;
    }

}
