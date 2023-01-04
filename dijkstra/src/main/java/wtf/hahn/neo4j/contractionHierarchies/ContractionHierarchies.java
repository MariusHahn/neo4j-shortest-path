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
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.model.PathResult;

public class ContractionHierarchies {

    @Context public GraphDatabaseService graphDatabaseService;
    @Context public Transaction transaction;

    @SuppressWarnings("unused")
    @Procedure(mode = Mode.WRITE)
    public void createContractionHierarchiesIndex(@Name("type") String type,
                                                  @Name("costProperty") String costProperty) {
            new ContractionHierarchiesIndexer(type, costProperty, transaction, Comparator.comparingInt(Node::getDegree)).insertShortcuts();
    }
    @SuppressWarnings("unused")
    @Procedure
    public Stream<PathResult> sourceTargetCH(@Name("startNode") Node startNode,
                                             @Name("endNode") Node endNode,
                                             @Name("type") String type,
                                             @Name("costProperty") String costProperty
    ){
        final RelationshipType relationshipType = RelationshipType.withName(type);
            BasicEvaluationContext evaluationContext = new BasicEvaluationContext(transaction, graphDatabaseService);
            WeightedPath path = new ContractionHierarchiesFinder(evaluationContext)
                    .find(startNode, endNode, relationshipType, costProperty);
            PathResult result = new PathResult(path);
            return Stream.of(result);

    }
}
