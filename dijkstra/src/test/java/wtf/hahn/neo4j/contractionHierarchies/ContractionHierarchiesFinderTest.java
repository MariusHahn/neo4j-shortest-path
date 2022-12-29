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
import wtf.hahn.neo4j.util.IntegrationTest;

public class ContractionHierarchiesFinderTest extends IntegrationTest {

    public ContractionHierarchiesFinderTest() {
        super(of(), of(Procedure.class), of(), Dataset.DIJKSTRA_SOURCE_TARGET_SAMPLE);
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
                    .map(n -> n.getProperty("name").toString())
                    .collect(Collectors.joining(","));
            System.out.println(names);
            System.out.printf("Result: %s%n", path.weight());
            transaction.rollback();
        }
    }
}
