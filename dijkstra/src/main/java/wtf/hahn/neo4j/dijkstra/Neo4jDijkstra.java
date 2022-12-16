package wtf.hahn.neo4j.dijkstra;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;

public class Neo4jDijkstra {

    public WeightedPath shortestPath(Node startNode, Node endNode, PathExpander<Double> expander, String costProperty) {
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(expander, costProperty, 1);
        return pathFinder.findSinglePath(startNode, endNode);
    }

}
