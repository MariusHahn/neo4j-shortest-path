package wtf.hahn.neo4j.contractionHierarchies.search;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

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

public class BiChSearch {

    private final PathExpander<Double> forwardExpander;
    private final CostEvaluator<Double> weightFunction;

    public BiChSearch(RelationshipType type, CostEvaluator<Double> weightFunction) {
        final String rankProperty = Shortcuts.rankPropertyName(type);
        forwardExpander = ContractionHierarchiesExpander.upwards(type, rankProperty);
        this.weightFunction = weightFunction;
    }

    public BiChSearch(RelationshipType type, String costProperty) {
        this(type, (relationship, direction) -> getDoubleProperty(relationship, costProperty));
    }

    public WeightedPath find(Node start, Node goal) {
        boolean pickForward = true;
        final PriorityQueue<WeightedPathImpl> candidates = new PriorityQueue<>();
        final Dijkstra.Query forwardQuery = new Dijkstra.Query(start, Set.of(), forwardExpander, weightFunction);
        final Dijkstra.Query backwardQuery = new Dijkstra.Query(goal, Set.of(), forwardExpander, weightFunction);
        while (!isComplete(forwardQuery, backwardQuery, candidates.peek())) {
            final Dijkstra.Query query = pickForward ? forwardQuery : backwardQuery;
            final Dijkstra.Query other = pickForward ? backwardQuery : forwardQuery;
            pickForward = !pickForward;
            if (!query.isComplete()) query.expandNext(); else continue;
            final Node latest = query.latestExpand();
            if (other.resultMap().containsKey(latest)) {
                WeightedPath forwardPath = forwardQuery.resultMap().get(latest);
                WeightedPath backwardPath = backwardQuery.resultMap().get(latest);
                candidates.offer(new WeightedPathImpl(forwardPath, backwardPath));
            }
        }
        return candidates.poll();
    }

    private static boolean isComplete(Dijkstra.Query forwardQuery, Dijkstra.Query backwardQuery, WeightedPath currentBest) {
        return forwardQuery.isComplete() && backwardQuery.isComplete() || shortestFound(forwardQuery, backwardQuery, currentBest);
    }

    private static boolean shortestFound(Dijkstra.Query forwardQuery, Dijkstra.Query backwardQuery, WeightedPath currentBest) {
        return currentBest != null && currentBest.weight() <= forwardQuery.latestWeight() &&
                currentBest.weight() <= backwardQuery.latestWeight();
    }
}
