package wtf.hahn.neo4j.dijkstra;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.impl.ExtendedPath;
import org.neo4j.graphdb.impl.StandardExpander;
import org.neo4j.graphdb.traversal.BranchState;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.util.EntityHelper;

import static wtf.hahn.neo4j.dijkstra.BidirectionalDijkstra.isSettled;
import static wtf.hahn.neo4j.util.PathUtils.bidirectional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public class Dijkstra {
    private final PathExpander<Double> baseExpander;
    private final CostEvaluator<Double> weightFunction;

    public Dijkstra(RelationshipType relationshipType, CostEvaluator<Double> weightFunction) {
        baseExpander = StandardExpander.create(relationshipType, Direction.OUTGOING);
        this.weightFunction = weightFunction;
    }

    public Dijkstra(RelationshipType relationshipType, String costProperty) {
        this(relationshipType,
                ((relationship, direction) -> EntityHelper.getDoubleProperty(relationship, costProperty)));
    }

    public ShortestPathResult findBi(Node start, Node goal) {
        final Query forwardQuery = new Query(start, goal, baseExpander);
        final Query backwardQuery = new Query(goal, start, baseExpander.reverse());
        ShortestPathResult candidate = new ShortestPathResult();
        while (!forwardQuery.queue.isEmpty() || !backwardQuery.queue.isEmpty()) {
            forwardQuery.relax();
            if (!(forwardQuery.shortestPath == null || forwardQuery.shortestPath.weight() >= candidate.weight())) {
                return forwardQuery.shortestPath;
            }
            DijkstraState forwardState = forwardQuery.queue.peek();
            if (isSettled(backwardQuery.seen, forwardState.getEndNode())) {
                final DijkstraState backwardState = backwardQuery.seen.get(forwardState.getEndNode());
                final double pathCost = forwardState.getCost() + backwardState.getCost();
                if (pathCost < candidate.weight()) {
                    final Path path = bidirectional(forwardState.getPath(), backwardState.getPath());
                    candidate = new ShortestPathResult(path, pathCost, 0, forwardQuery.seen.size() + backwardQuery.seen.size());
                }
            }
            backwardQuery.relax();
            if (!(backwardQuery.shortestPath == null || backwardQuery.shortestPath.weight() >= candidate.weight())) {
                return backwardQuery.shortestPath;
            }
            final DijkstraState backwardState = forwardQuery.queue.peek();
            if (isSettled(forwardQuery.seen, backwardState.getEndNode())) {
                DijkstraState forwardState2 = forwardQuery.seen.get(backwardState.getEndNode());
                final double pathCost = forwardState2.getCost() + backwardState.getCost();
                if (pathCost < candidate.weight()) {
                    final Path path = bidirectional(forwardState2.getPath(), backwardState.getPath());
                    candidate = new ShortestPathResult(path, pathCost, 0, forwardQuery.seen.size() + backwardQuery.seen.size());
                }
            }
            if (shouldStop(forwardQuery, backwardQuery, candidate, forwardState, backwardState)) {
                return candidate;
            }
        }
        return null;
    }

    boolean shouldStop(final Query forwardQuery, final Query backwardQuery,
                       ShortestPathResult candidate, DijkstraState forwardState,
                       DijkstraState backwardState) {
        PriorityQueue<DijkstraState> forwardQueue = forwardQuery.queue;
        PriorityQueue<DijkstraState> backwardQueue = backwardQuery.queue;
        Map<Node, DijkstraState> forwardStates = forwardQuery.seen;
        Map<Node, DijkstraState> backwardStates = backwardQuery.seen;
        DijkstraState forwardBest = forwardQueue.peek();
        DijkstraState backwardBest = backwardQueue.peek();
        if (backwardBest == null || forwardBest == null || candidate.path() == null) {
            return false;
        }
        if (!isSettled(forwardStates, backwardBest.getEndNode()) || !isSettled(backwardStates, forwardBest.getEndNode())) {
            return false;
        }
        double v = Optional.ofNullable(forwardStates.get(forwardState.getEndNode())).map(DijkstraState::getCost).orElse(0.0);
        double w = Optional.ofNullable(backwardStates.get(backwardState.getEndNode())).map(DijkstraState::getCost).orElse(0.0);
        return v + w >= candidate.weight();
    }

    public ShortestPathResult find(Node start, Node goal) {
        return new Query(start, goal, baseExpander).getResult();
    }

    class Query {
        private final Node goal;
        private final PriorityQueue<DijkstraState> queue = new PriorityQueue<>();
        private final Map<Node, DijkstraState> seen = new HashMap<>();
        private final PathExpander<Double> expander;
        private ShortestPathResult shortestPath = null;

        Query(Node start, Node goal, PathExpander<Double> expander) {
            this.goal = goal;
            this.expander = expander;
            DijkstraState init = new DijkstraState(start);
            queue.offer(init);
            seen.put(start, init);

        }

        public ShortestPathResult getResult() {
            while (!queue.isEmpty() && shortestPath == null) {
                relax();
            }
            return shortestPath;
        }

        private void relax() {
            final DijkstraState state = queue.poll();
            if (state.getEndNode().equals(goal)) {
                shortestPath = new ShortestPathResult(state.getPath(), state.getCost(), 0, queue.size());
            }
            final ResourceIterable<Relationship> relationships = expander.expand(state.getPath(), BranchState.NO_STATE);
            state.settled = true;
            for (Relationship relationship : relationships) {
                final Node neighbor = relationship.getOtherNode(state.getEndNode());
                final Double cost = weightFunction.getCost(relationship, Direction.BOTH);
                if (mustUpdateNeighborState(state, neighbor, cost)) {
                    final Path newPath = ExtendedPath.extend(state.getPath(), relationship);
                    final DijkstraState newState = new DijkstraState(neighbor, newPath, state.getCost() + cost);
                    queue.remove(newState);
                    queue.offer(newState);
                    seen.put(neighbor, newState);
                }
            }
        }

        private boolean mustUpdateNeighborState(DijkstraState state, Node neighbor, Double cost) {
            return !seen.containsKey(neighbor) ||
                    !(seen.get(neighbor).settled || seen.get(neighbor).getCost() < state.getCost() + cost);
        }
    }
}
