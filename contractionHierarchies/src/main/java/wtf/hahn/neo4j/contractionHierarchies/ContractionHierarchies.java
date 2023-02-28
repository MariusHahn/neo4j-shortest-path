package wtf.hahn.neo4j.contractionHierarchies;

import java.util.Comparator;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Name;
import wtf.hahn.neo4j.model.PathResult;

@RequiredArgsConstructor
public class ContractionHierarchies {
    private final GraphDatabaseService graphDatabaseService;
    private final Transaction transaction;

    public void createContractionHierarchiesIndex(String type, String costProperty) {
        new ContractionHierarchiesIndexer(type, costProperty, transaction,
                Comparator.comparingInt(Node::getDegree)).insertShortcuts();
    }

    public Stream<PathResult> sourceTargetCH(Node startNode, Node endNode, String type, String costProperty) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        BasicEvaluationContext evaluationContext = new BasicEvaluationContext(transaction, graphDatabaseService);
        WeightedPath path = new ContractionHierarchiesFinder(evaluationContext,  relationshipType, costProperty)
                .find(startNode, endNode);
        return path != null
                ? Stream.of(new PathResult(new WeightedCHPath(path, transaction)))
                : Stream.of(PathResult.noPath());

    }
}
