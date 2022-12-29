package wtf.hahn.neo4j.contractionHierarchies;

import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.contractionHierarchies.expander.ContractionHierarchiesExpander;

public record ContractionHierarchiesFinder(EvaluationContext evaluationContext) {

    public WeightedPath find(Node source, Node target, RelationshipType type, String costProperty) {
        ContractionHierarchiesExpander expander = new ContractionHierarchiesExpander(
                type
                , Shortcut.rankPropertyName(type)
                , ContractionHierarchiesExpander.Side.UPWARDS
        );
        return GraphAlgoFactory.dijkstra(evaluationContext, expander, costProperty).findSinglePath(source, target);
    }
}
