package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.procedure.ContractionHierarchies;
import wtf.hahn.neo4j.util.IntegrationTest;

public class ContractionHierarchiesTest extends IntegrationTest {

    public ContractionHierarchiesTest() {
        super(of(), of(ContractionHierarchies.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void chStandardContractionOrderTest() {
        try (Transaction transaction = database().beginTx()) {
            String cypher = "CALL wtf.hahn.neo4j.procedure.createContractionHierarchiesIndex('ROAD', 'cost')";
            transaction.execute(cypher);
            Set<ShortcutTriple> shortcuts = transaction.getAllRelationships().stream()
                    .filter(Shortcut::isShortcut)
                    .map(ShortcutTriple::new)
                    .peek(System.out::println)
                    .collect(Collectors.toSet());
            Assertions.assertEquals(0, shortcuts.size());
        }
    }

    @Test
    void chReverseDegreeTest() {
        try (Transaction transaction = database().beginTx()) {
            new CH(
                    relationshipType().name()
                    , costProperty()
                    , transaction
                    , Comparator.<Node>comparingInt(Node::getDegree).reversed()
            ).insertShortcuts();

            List<ShortcutTriple> shortcuts = transaction.getAllRelationships().stream()
                    .filter(Shortcut::isShortcut)
                    .map(ShortcutTriple::new)
                    .peek(System.out::println)
                    .toList();
            Assertions.assertTrue(shortcuts.contains(new ShortcutTriple("B", "E", 70.0)));
            Assertions.assertTrue(shortcuts.contains(new ShortcutTriple("B", "F", 110.0)));
            Assertions.assertTrue(shortcuts.contains(new ShortcutTriple("C", "E", 70.0)));
            Assertions.assertTrue(shortcuts.contains(new ShortcutTriple("C", "F", 110.0)));
            Assertions.assertEquals(4, shortcuts.size());

        }
    }
    private record ShortcutTriple(String start, String end, Double weight) {
        ShortcutTriple(Relationship relationship) {
            this(
                    (String) relationship.getStartNode().getProperty("name")
                    , (String) relationship.getEndNode().getProperty("name")
                    , (Double) relationship.getProperty((String) relationship.getProperty(Shortcut.WEIGHT_PROPERTY_KEY))
            );
        }
    }
}
