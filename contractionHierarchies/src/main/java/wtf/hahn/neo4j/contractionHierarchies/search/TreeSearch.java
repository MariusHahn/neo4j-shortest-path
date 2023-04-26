package wtf.hahn.neo4j.contractionHierarchies.search;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.contractionHierarchies.expander.ContractionHierarchiesExpander;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.util.PathUtils;

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

    public ShortestPathResult find(Node start, Node goal) {
        Dijkstra dijkstra = new Dijkstra(type, weightFunction);
        Map<Node, ShortestPathResult> forwardTree = dijkstra.find(start, Set.of(), forwardExpander);
        Map<Node, ShortestPathResult> backwardTree = dijkstra.find(goal, Set.of(), backwardExpander);
        record Tree(ShortestPathResult forwardPath, ShortestPathResult backwardPath) implements Comparable<Tree> {
            @Override
            public int compareTo(Tree o) {
                double o1Weight = forwardPath.weight() + backwardPath.weight();
                double o2Weight = o.forwardPath.weight() + o.backwardPath.weight();
                return Double.compare(o1Weight, o2Weight);
            }
        }
        PriorityQueue<Tree> trees = new PriorityQueue<>();
        forwardTree.forEach((endNode, path) -> {
            if (backwardTree.containsKey(endNode)) trees.add(new Tree(path, backwardTree.get(endNode)));
        });
        if (trees.peek() == null) return null;
        Tree poll = trees.poll();
        Path completePath = PathUtils.bidirectional(poll.forwardPath, poll.backwardPath);
        return new ShortestPathResult(completePath, poll.forwardPath.weight() + poll.backwardPath.weight(), 0, 0);
    }
}
