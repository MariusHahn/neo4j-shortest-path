package wtf.hahn.neo4j.dijkstra;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.impl.ExtendedPath;
import org.neo4j.graphdb.impl.StandardExpander;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Paths;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.util.EntityHelper;
import wtf.hahn.neo4j.util.Iterables;
import wtf.hahn.neo4j.util.PathUtils;
import wtf.hahn.neo4j.util.ZipIterator;

import static wtf.hahn.neo4j.util.PathUtils.bidirectional;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Stream;

public class BidirectionalDijkstra {

    private final PathExpander<Double> forwardExpander;
    private final PathExpander<Double> backwardExpander;
    private final CostEvaluator<Double> weightFunction;

    public BidirectionalDijkstra(RelationshipType relationshipType, CostEvaluator<Double> weightFunction) {
        forwardExpander = StandardExpander.create(relationshipType, Direction.OUTGOING);
        backwardExpander = forwardExpander.reverse();
        this.weightFunction = weightFunction;
    }

    public BidirectionalDijkstra(RelationshipType relationshipType, String costProperty) {
        this(relationshipType,
                ((relationship, direction) -> EntityHelper.getDoubleProperty(relationship, costProperty)));
    }

    public ShortestPathResult find(Node start, Node goal) {
        return new Query(start, goal).getResult();
    }

    class Query {
        private final Node start, goal;
        private final PriorityQueue<DijkstraState> forwardQueue = new PriorityQueue<>();
        private final PriorityQueue<DijkstraState> backwardQueue = new PriorityQueue<>();
        private final Map<Node, DijkstraState> forwardStates = new HashMap<>();
        private final Map<Node, DijkstraState> backwardStates = new HashMap<>();
        private ShortestPathResult shortestPathCandidate = new ShortestPathResult();
        long rank = 0L;
        boolean continueBackward = true;
        boolean continueForward = true;

        Query(Node start, Node goal) {
            this.start = start;
            this.goal = goal;
            DijkstraState initialF = new DijkstraState(start);
            forwardQueue.offer(initialF);
            DijkstraState initialB = new DijkstraState(goal);
            backwardQueue.offer(initialB);
            forwardStates.put(initialF.getEndNode(), initialF);
            backwardStates.put(initialB.getEndNode(), initialB);
        }

        public ShortestPathResult getResult() {
            if (start.equals(goal)) {
                return new ShortestPathResult(start);
            }
            while (continueForward || continueBackward) {
                ++rank;
                if (continueForward) {
                    if (forwardQueue.isEmpty()) {
                        continueForward = false;
                        continue;
                    }
                    final DijkstraState forwardState = forwardQueue.poll();
                    forwardState.settled = true;
                    if (shouldStop()) {
                        continueForward = false;
                        continue;
                    }
                    final ResourceIterable<Relationship>
                            relationships = forwardExpander.expand(forwardState.getPath(), BranchState.NO_STATE);
                    addStatesToQueue(relationships, forwardStates, forwardQueue, forwardState);
                    if (isSettled(backwardStates, forwardState.getEndNode())) {
                        final DijkstraState backwardState = backwardStates.get(forwardState.getEndNode());
                        final double newPathWeight = backwardState.getCost() + forwardState.getCost();
                        if (newPathWeight < shortestPathCandidate.weight()) {
                            final Path mergedPath = bidirectional(forwardState.getPath(), backwardState.getPath());
                            shortestPathCandidate = new ShortestPathResult(mergedPath, newPathWeight, rank,
                                    settledCount(forwardStates) + settledCount(backwardStates));
                        }
                    }
                }
                if (continueBackward) {
                    if (backwardQueue.isEmpty()) {
                        continueBackward = false;
                        continue;
                    }
                    final DijkstraState backwardState = backwardQueue.poll();
                    backwardState.settled = true;
                    if (shouldStop()) {
                        continueBackward = false;
                        continue;
                    }
                    final ResourceIterable<Relationship> relationships = backwardExpander.expand(backwardState.getPath(), BranchState.NO_STATE);
                    addStatesToQueue(relationships, backwardStates, backwardQueue, backwardState);
                    if (isSettled(forwardStates, backwardState.getEndNode())) {
                        final DijkstraState forwardState = forwardStates.get(backwardState.getEndNode());
                        final double pathWeight = forwardState.getCost() + backwardState.getCost();
                        if (pathWeight < shortestPathCandidate.weight()) {
                            final Path path = bidirectional(forwardState.getPath(), backwardState.getPath());
                            shortestPathCandidate = new ShortestPathResult(path, pathWeight, rank,
                                    settledCount(forwardStates) + settledCount(backwardStates));
                        }
                    }
                }

            }
            return shortestPathCandidate;
        }
        boolean shouldStop() {
            DijkstraState forwardBest = forwardQueue.peek();
            DijkstraState backwardBest = backwardQueue.peek();
            if (backwardBest == null || forwardBest == null) {
                return false;
            }
            if (!isSettled(forwardStates, backwardBest.getEndNode()) || !isSettled(backwardStates, forwardBest.getEndNode())) {
                return false;
            }
            double v = Optional.ofNullable(forwardStates.get(backwardBest.getEndNode())).map(DijkstraState::getCost).orElse(0.0) + forwardBest.getCost();
            double w = Optional.ofNullable(backwardStates.get(forwardBest.getEndNode())).map(DijkstraState::getCost).orElse(0.0) + backwardBest.getCost();
            return shortestPathCandidate.weight() <= v && shortestPathCandidate.weight() <= w;
        }
    }

    private void addStatesToQueue(Iterable<Relationship> relationships, Map<Node, DijkstraState> queueStates, Queue<DijkstraState> queue, DijkstraState state) {
        final Node node = state.getEndNode();
        for (final Relationship relationship : relationships) {
            final Node neighbor = relationship.getOtherNode(node);
            if (isSettled(queueStates, neighbor)) {
                continue;
            }
            final DijkstraState newState = new DijkstraState(
                    neighbor,
                    ExtendedPath.extend(state.getPath(), relationship)
                    , state.getCost() + cost(relationship)
            );
            if (!alreadySeen(queueStates, neighbor)) {
                queue.offer(newState);
                queueStates.put(neighbor, newState);
            } else if (newStateHasLowerCost(queueStates, newState)) {
                queueStates.put(neighbor, newState);
                queue.remove(newState);
                queue.offer(newState);
            }
        }
    }


    private static boolean newStateHasLowerCost(Map<Node, DijkstraState> backwardQueueStates, DijkstraState possibleUpdate) {
        return backwardQueueStates.get(possibleUpdate.getEndNode()).getCost() > possibleUpdate.getCost();
    }

    private static boolean alreadySeen(Map<Node, DijkstraState> queueStateForward, Node possibleUpdateForwardSearch) {
        return queueStateForward.containsKey(possibleUpdateForwardSearch);
    }

    private Double cost(Relationship relationship) {
        return weightFunction.getCost(relationship, Direction.BOTH);
    }

    private static boolean isSettled(Map<Node, DijkstraState> queueState, Node neighbor) {
        return !(!alreadySeen(queueState, neighbor) || !queueState.get(neighbor).isSettled());
    }

    private static long settledCount(Map<Node, DijkstraState> queueState) {
        return queueState.values().stream().filter(DijkstraState::isSettled).count();
    }
}