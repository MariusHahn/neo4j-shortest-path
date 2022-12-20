package wtf.hahn.neo4j.dijkstra;

import static org.neo4j.graphdb.Direction.OUTGOING;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.contractionHierarchies.Shortcut;

public class Neo4jDijkstra {

    public WeightedPath shortestPath(Node startNode, Node endNode, PathExpander<Double> expander, String costProperty) {
        PathFinder<WeightedPath> dijkstraFinder = GraphAlgoFactory.dijkstra(expander, costProperty, 1);
        return dijkstraFinder.findSinglePath(startNode, endNode);
    }

    public WeightedPath shortestPathWithShortcuts(Node startNode, Node endNode, RelationshipType type, String costProperty) {
        PathExpander<Double> standardExpander = PathExpanders.forTypesAndDirections(
                type
                , OUTGOING
                , Shortcut.shortcutRelationshipType(type)
                , OUTGOING
        );
        return shortestPath(startNode, endNode, standardExpander, costProperty);
    }

}
