package wtf.hahn.neo4j.contractionHierarchies.index;

import static java.util.List.of;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.TestDataset;
import wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexerOld;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.testUtil.ShortcutTriple;

public class ContractionHierarchiesIndexerOldTest extends IntegrationTest {

    public ContractionHierarchiesIndexerOldTest() {
        super(of(), of(), of(), TestDataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void chReverseDegreeTest() {
        try (Transaction transaction = database().beginTx()) {
            new ContractionHierarchiesIndexerOld(
                    relationshipType().name()
                    , costProperty()
                    , transaction
                    , Comparator.<Node>comparingInt(Node::getDegree).reversed()
                    , database()
            ).insertShortcuts();

            List<ShortcutTriple> shortcuts = transaction.getAllRelationships().stream()
                    .filter(Shortcuts::isShortcut)
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
