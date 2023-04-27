package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;
import static wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexerByEdgeDifference.*;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexerByEdgeDifference;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.EntityHelper;
import wtf.hahn.neo4j.util.Iterables;

public class ContractionHierarchiesFinderTest extends IntegrationTest {

    public ContractionHierarchiesFinderTest() {
        super(of(), of(), of(), TestDataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
        try (Transaction transaction = database().beginTx()) {
            new ContractionHierarchiesIndexerByEdgeDifference(
                    relationshipType().name()
                    , costProperty()
                    , transaction
                    , Mode.DISK
            ).insertShortcuts();
            transaction.commit();
        }
    }

    @Test
    void sourceTargetCypher() {
        try (Transaction transaction = database().beginTx()) {
            Node start = transaction.findNode(() -> "Location", "name", "A");
            Node goal = transaction.findNode(() -> "Location", "name", "F");
            ContractionHierarchiesFinder finder = new ContractionHierarchiesFinder(
                    new BasicEvaluationContext(transaction, database())
                    , relationshipType()
                    , costProperty()
            );
            WeightedPath chPath = finder.find(start, goal);
            Assertions.assertEquals(160.0, chPath.weight());
            WeightedPath resolvedPath = Shortcuts.resolve(chPath, transaction);
            String namesResolved = pathToString(resolvedPath.nodes());
            System.out.println(namesResolved);
            Assertions.assertEquals("A, B, D, E, F", namesResolved);
            System.out.printf("Result: %s%n", chPath.weight());
        }
    }

    private String pathToString(Iterable<Node> path) {
        return Iterables.stream(path)
                .map(EntityHelper::getNameProperty)
                .collect(Collectors.joining(", "));
    }
}
