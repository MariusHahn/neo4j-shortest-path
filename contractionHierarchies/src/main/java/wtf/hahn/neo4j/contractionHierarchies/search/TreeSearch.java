package wtf.hahn.neo4j.contractionHierarchies.search;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.contractionHierarchies.expander.ContractionHierarchiesExpander;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.WeightedPathImpl;

public class TreeSearch {

    private final PathExpander<Double> forwardExpander;
    private final PathExpander<Double> backwardExpander;
    private final CostEvaluator<Double> weightFunction;
    private final RelationshipType type;

    public TreeSearch(RelationshipType type, CostEvaluator<Double> weightFunction) {
        final String rankProperty = Shortcuts.rankPropertyName(type);
        this.type = type;
        forwardExpander = ContractionHierarchiesExpander.upwards(this.type, rankProperty);
        backwardExpander = forwardExpander.reverse();
        this.weightFunction = weightFunction;
    }

    public TreeSearch(RelationshipType type, String costProperty) {
        this(type, (relationship, direction) -> getDoubleProperty(relationship, costProperty));
    }

    public WeightedPath find(Node start, Node goal) {
        Dijkstra dijkstra = new Dijkstra(type, weightFunction);
        Map<Node, WeightedPath> forwardPaths = dijkstra.find(start, Set.of(), forwardExpander);
        Map<Node, WeightedPath> backwardPaths = dijkstra.find(goal, Set.of(), backwardExpander);
        PriorityQueue<WeightedPathImpl> candidates = new PriorityQueue<>();
        forwardPaths.forEach((endNode, path) -> {
            if (backwardPaths.containsKey(endNode)) candidates.add(new WeightedPathImpl(path, backwardPaths.get(endNode)));
        });
        return candidates.poll();
    }
}
