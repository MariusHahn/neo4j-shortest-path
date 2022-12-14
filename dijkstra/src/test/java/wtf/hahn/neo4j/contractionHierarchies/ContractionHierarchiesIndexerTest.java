package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.IntegrationTest;

public class ContractionHierarchiesIndexerTest extends IntegrationTest {

    public ContractionHierarchiesIndexerTest() {
        super(of(), of(ContractionHierarchies.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void chReverseDegreeTest() {
        try (Transaction transaction = database().beginTx()) {
            new ContractionHierarchiesIndexer(
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
            transaction.rollback();
        }
    }
}
