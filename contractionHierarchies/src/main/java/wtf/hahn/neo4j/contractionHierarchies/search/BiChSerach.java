package wtf.hahn.neo4j.contractionHierarchies.search;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;
import static wtf.hahn.neo4j.util.PathUtils.bidirectional;

import java.util.Comparator;
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

public class BiChSerach {

    private final PathExpander<Double> forwardExpander;
    private final CostEvaluator<Double> weightFunction;

    public BiChSerach(RelationshipType type, CostEvaluator<Double> weightFunction) {
        final String rankProperty = Shortcuts.rankPropertyName(type);
        forwardExpander = ContractionHierarchiesExpander.upwards(type, rankProperty);
        this.weightFunction = weightFunction;
    }

    public BiChSerach(RelationshipType type, String costProperty) {
        this(type, (relationship, direction) -> getDoubleProperty(relationship, costProperty));
    }

    public ShortestPathResult find(Node start, Node goal) {
        boolean pickForward = true;
        final PriorityQueue<ShortestPathResult> candidates = new PriorityQueue<>(x);
        final Dijkstra.Query forwardQuery = new Dijkstra.Query(start, Set.of(), forwardExpander, weightFunction);
        final Dijkstra.Query backwardQuery = new Dijkstra.Query(goal, Set.of(), forwardExpander, weightFunction);
        while (!isComplete(forwardQuery, backwardQuery, candidates.peek())) {
            final Dijkstra.Query query = pickForward ? forwardQuery : backwardQuery;
            final Dijkstra.Query other = pickForward ? backwardQuery : forwardQuery;
            pickForward = !pickForward;
            if (!query.isComplete()) query.expandNext(); else continue;
            final Node latest = query.latestExpand();
            if (other.resultMap().containsKey(latest)) {
                final double forwardWeight = forwardQuery.resultMap().get(latest).weight();
                final double backwardWeight = backwardQuery.resultMap().get(latest).weight();
                final Path path = bidirectional(forwardQuery.resultMap().get(latest), backwardQuery.resultMap().get(latest));
                candidates.offer(new ShortestPathResult(path, forwardWeight + backwardWeight, 0,0));
            }
        }
        return candidates.poll();
    }

    private static boolean isComplete(Dijkstra.Query forwardQuery, Dijkstra.Query backwardQuery, ShortestPathResult currentBest) {
        return forwardQuery.isComplete() && backwardQuery.isComplete() || shortestFound(forwardQuery, backwardQuery, currentBest);
    }

    private static boolean shortestFound(Dijkstra.Query forwardQuery, Dijkstra.Query backwardQuery, ShortestPathResult currentBest) {
        return currentBest != null && currentBest.weight() <= forwardQuery.latestWeight() &&
                currentBest.weight() <= backwardQuery.latestWeight();
    }

    Comparator<ShortestPathResult> x = new Comparator<>() {
        @Override
        public int compare(ShortestPathResult o1, ShortestPathResult o2) {
            return Double.compare(o1.weight(), o2.weight());
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    };
}
