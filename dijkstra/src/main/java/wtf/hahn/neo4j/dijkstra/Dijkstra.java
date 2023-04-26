package wtf.hahn.neo4j.dijkstra;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.impl.ExtendedPath;
import org.neo4j.graphdb.traversal.BranchState;
import wtf.hahn.neo4j.model.ShortestPathResult;

import static wtf.hahn.neo4j.util.EntityHelper.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    private final PathExpander<Double> baseExpander;
    private final CostEvaluator<Double> weightFunction;

    public Dijkstra(RelationshipType relationshipType, CostEvaluator<Double> weightFunction) {
        baseExpander = PathExpanderBuilder.empty().add(relationshipType, Direction.OUTGOING).build();
        this.weightFunction = weightFunction;
    }

    public Dijkstra(RelationshipType relationshipType, String costProperty) {
        this(relationshipType, ((relationship, direction) -> getDoubleProperty(relationship, costProperty)));
    }

    public ShortestPathResult find(Node start, Node goal, PathExpander<Double> expander) {
        return new Query(start, Set.of(goal), expander, weightFunction).getResult().get(goal);
    }

    public ShortestPathResult find(Node start, Node goal) {
        return new Query(start, Set.of(goal), baseExpander, weightFunction).getResult().get(goal);
    }
    public Map<Node, ShortestPathResult> find(Node start, Collection<Node> goals) {
        return new Query(start, new HashSet<>(goals), baseExpander, weightFunction).getResult();
    }

    public Map<Node, ShortestPathResult> find(Node start, Collection<Node> goals, PathExpander<Double> expander) {
        return new Query(start, new HashSet<>(goals), expander, weightFunction).getResult();
    }

    public static class Query {
        private final Set<Node> goals;
        private final PriorityQueue<DijkstraState> queue = new PriorityQueue<>();
        private final Map<Node, DijkstraState> seen = new HashMap<>();
        private final PathExpander<Double> expander;
        private final Map<Node, ShortestPathResult> shortestPaths;
        private final CostEvaluator<Double> weightFunction;
        private DijkstraState latestExpand;

        public Query(Node start, Set<Node> goals, PathExpander<Double> expander,
                     CostEvaluator<Double> weightFunction) {
            this.goals = goals;
            this.expander = expander;
            this.weightFunction = weightFunction;
            DijkstraState init = new DijkstraState(start);
            queue.offer(init);
            seen.put(start, init);
            shortestPaths = new HashMap<>(goals.size() * 4 / 3 );
        }

        private Map<Node, ShortestPathResult> getResult() {
            while (!isComplete()) {expandNext();}
            return shortestPaths;
        }

        public void expandNext() {
            final DijkstraState state = queue.poll();
            latestExpand = state;
            if (goals.contains(state.getEndNode()) || goals.isEmpty()) {
                shortestPaths.put(state.getEndNode(), new ShortestPathResult(state.getPath(), state.getCost(), 0, queue.size()));
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

        public boolean isComplete() {
            return queue.isEmpty() || (goals.size() == shortestPaths.size() && goals.size() != 0);
        }

        private boolean mustUpdateNeighborState(DijkstraState state, Node neighbor, Double cost) {
            return !seen.containsKey(neighbor) ||
                    !(seen.get(neighbor).settled || seen.get(neighbor).getCost() < state.getCost() + cost);
        }

        public Map<Node, ShortestPathResult> resultMap() {
            return shortestPaths;
        }

        public Node latestExpand() {
            return latestExpand.getEndNode();
        }

        public double latestWeight() {
            return shortestPaths.get(latestExpand.getEndNode()).weight();
        }
    }
}
