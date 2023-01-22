package wtf.hahn.neo4j.contractionHierarchies;

import java.util.Comparator;
import java.util.stream.Stream;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Name;
import wtf.hahn.neo4j.model.PathResult;

public record ContractionHierarchies(GraphDatabaseService graphDatabaseService,
                                     Transaction transaction) {

    public void createContractionHierarchiesIndex(@Name("type") String type,
                                                  @Name("costProperty") String costProperty) {
        new ContractionHierarchiesIndexer(type, costProperty, transaction,
                Comparator.comparingInt(Node::getDegree)).insertShortcuts();
    }

    public Stream<PathResult> sourceTargetCH(Node startNode,
                                             Node endNode,
                                             String type,
                                             String costProperty
    ) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        BasicEvaluationContext evaluationContext = new BasicEvaluationContext(transaction, graphDatabaseService);
        WeightedPath path = new ContractionHierarchiesFinder(evaluationContext)
                .find(startNode, endNode, relationshipType, costProperty);
        PathResult result = new PathResult(new WeightedCHPath(path, transaction));
        return Stream.of(result);

    }
}
