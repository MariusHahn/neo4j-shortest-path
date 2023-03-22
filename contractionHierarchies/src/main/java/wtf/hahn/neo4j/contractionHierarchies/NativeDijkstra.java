package wtf.hahn.neo4j.contractionHierarchies;

import static org.neo4j.graphdb.Direction.OUTGOING;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.util.EntityHelper;

@RequiredArgsConstructor
public class NativeDijkstra {

    private final EvaluationContext context;

    public WeightedPath shortestPath(Node startNode, Node endNode, PathExpander<Double> expander, String costProperty) {
        PathFinder<WeightedPath> dijkstraFinder = GraphAlgoFactory.dijkstra(expander, costProperty, 1);
        return dijkstraFinder.findSinglePath(startNode, endNode);
    }

    public WeightedPath shortestPathWithShortcuts(Node startNode, Node endNode, RelationshipType type, String costProperty) {
        PathExpander<Double> standardExpander = PathExpanders.forTypesAndDirections(
                type
                , OUTGOING
                , Shortcuts.shortcutRelationshipType(type)
                , OUTGOING
        );
        return shortestPath(startNode, endNode, standardExpander, costProperty);
    }

    public Iterable<WeightedPath> shortestPathsWithShortcuts(Node startNode, Node endNode, RelationshipType type,
                                                             String costProperty, String rankPropertyName) {
        PathExpander<Double> standardExpander = PathExpanderBuilder
                .empty()
                .add(type, OUTGOING)
                .add(Shortcuts.shortcutRelationshipType(type), OUTGOING)
                .addNodeFilter(node -> !node.hasProperty(rankPropertyName))
                .build();
        PathFinder<WeightedPath> dijkstraFinder = GraphAlgoFactory.dijkstra(
                context,
                standardExpander,
                (relationship, direction) -> EntityHelper.getDoubleProperty(relationship, costProperty)
        );
        return dijkstraFinder.findAllPaths(startNode, endNode);
    }
}
