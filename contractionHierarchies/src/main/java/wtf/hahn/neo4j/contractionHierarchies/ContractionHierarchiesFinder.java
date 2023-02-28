package wtf.hahn.neo4j.contractionHierarchies;

import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.contractionHierarchies.search.TreeBasedCHSearch;
import wtf.hahn.neo4j.model.Shortcut;

@RequiredArgsConstructor
public class ContractionHierarchiesFinder {
    private final EvaluationContext evaluationContext;
    private final RelationshipType type;
    private final String costProperty;

    public WeightedPath find(Node source, Node target) {
        return find(source, target, Shortcut.rankPropertyName(type));
    }

    public WeightedPath find(Node source, Node target, String rankProperty) {
        TreeBasedCHSearch search = new TreeBasedCHSearch(evaluationContext, type, rankProperty, costProperty);
        return search.find(source, target);
    }
}
