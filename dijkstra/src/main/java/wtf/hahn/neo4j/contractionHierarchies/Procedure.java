package wtf.hahn.neo4j.contractionHierarchies;

import java.util.Comparator;
import java.util.stream.Stream;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import wtf.hahn.neo4j.model.PathResult;

public class Procedure {

    @Context
    public GraphDatabaseService graphDatabaseService;

    @SuppressWarnings("unused")
    @org.neo4j.procedure.Procedure(mode = Mode.WRITE)
    public void createContractionHierarchiesIndex(@Name("type") String type,
                                                  @Name("costProperty") String costProperty) {
        try (Transaction transaction = graphDatabaseService.beginTx()) {
            new ContractionHierarchiesIndexer(type, costProperty, transaction, Comparator.comparingInt(Node::getDegree)).insertShortcuts();
            transaction.commit();
        }
    }
    @SuppressWarnings("unused")
    @org.neo4j.procedure.Procedure
    public Stream<PathResult> sourceTargetCH(@Name("startNode") Node startNode,
                                             @Name("endNode") Node endNode,
                                             @Name("type") String type,
                                             @Name("costProperty") String costProperty
    ){
        final RelationshipType relationshipType = RelationshipType.withName(type);
        try (Transaction transaction = graphDatabaseService.beginTx()) {
            BasicEvaluationContext evaluationContext =
                    new BasicEvaluationContext(transaction, graphDatabaseService);
            WeightedPath path = new ContractionHierarchiesFinder(evaluationContext)
                    .find(startNode, endNode, relationshipType, costProperty);
            PathResult result = new PathResult(path);
            return Stream.of(result);
        }
    }
}
