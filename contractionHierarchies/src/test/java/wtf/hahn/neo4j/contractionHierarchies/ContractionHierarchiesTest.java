package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterables;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.EntityHelper;

public class ContractionHierarchiesTest extends IntegrationTest {

    public ContractionHierarchiesTest() {
        super(of(), of(ContractionHierarchies.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void chStandardContractionOrderTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = "CALL wtf.hahn.neo4j.contractionHierarchies.createContractionHierarchiesIndex('ROAD', 'cost')";
            transaction.execute(cypher);
            Set<ShortcutTriple> shortcuts = transaction.getAllRelationships().stream()
                    .filter(Shortcut::isShortcut)
                    .map(ShortcutTriple::new)
                    .peek(System.out::println)
                    .collect(Collectors.toSet());
            Assertions.assertEquals(0, shortcuts.size());
            transaction.rollback();
        }
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
                CALL wtf.hahn.neo4j.contractionHierarchies.sourceTargetCH(a, b, 'ROAD', 'cost')
                YIELD pathCost, path
                RETURN pathCost, path
                """;
    }

}
