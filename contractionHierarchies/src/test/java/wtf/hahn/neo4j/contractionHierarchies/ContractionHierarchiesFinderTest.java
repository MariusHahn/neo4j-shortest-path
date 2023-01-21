package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.util.Comparator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterables;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.EntityHelper;

public class ContractionHierarchiesFinderTest extends IntegrationTest {

    public ContractionHierarchiesFinderTest() {
        super(of(), of(ContractionHierarchies.class), of(), TestDataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
    }

    @Test
    void sourceTargetCypher() {
        try (Transaction transaction = database().beginTx()) {
            new ContractionHierarchiesIndexer(
                    relationshipType().name()
                    , costProperty()
                    , transaction
                    , Comparator.<Node>comparingInt(Node::getDegree).reversed()
            ).insertShortcuts();
            WeightedPath path = new ContractionHierarchiesFinder(new BasicEvaluationContext(transaction, database())
            ).find(
                    transaction.findNode(() -> "Location", "name", "A")
                    , transaction.findNode(() -> "Location", "name", "F")
                    , relationshipType()
                    , costProperty()
            );
            Assertions.assertEquals(160.0, path.weight());
            String names = Iterables.stream(path.nodes())
                    .map(EntityHelper::getNameProperty)
                    .collect(Collectors.joining(", "));
            System.out.println(names);
            Assertions.assertEquals("A, B, F", names);
            String namesResolved = Iterables.stream(new WeightedCHPath(path, transaction).nodes())
                    .map(EntityHelper::getNameProperty)
                    .collect(Collectors.joining(", "));
            System.out.println(namesResolved);
            Assertions.assertEquals("A, B, D, E, F", namesResolved);
            System.out.printf("Result: %s%n", path.weight());
            transaction.rollback();
        }
    }
}
