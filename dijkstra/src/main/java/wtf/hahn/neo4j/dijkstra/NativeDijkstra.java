package wtf.hahn.neo4j.dijkstra;

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
import org.neo4j.graphdb.impl.StandardExpander;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.util.EntityHelper;

@RequiredArgsConstructor
public class NativeDijkstra {

    private final EvaluationContext context;

    public WeightedPath shortestPath(Node startNode, Node endNode, PathExpander<Double> expander, String costProperty) {
        PathFinder<WeightedPath> dijkstraFinder = GraphAlgoFactory.dijkstra(context, expander, costProperty);
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
}
