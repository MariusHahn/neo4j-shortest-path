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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Dijkstra {
    private final PathExpander<Double> expander;
    private final CostEvaluator<Double> weightFunction;

    public Dijkstra(RelationshipType relationshipType, CostEvaluator<Double> weightFunction) {
        expander = StandardExpander.create(relationshipType, Direction.OUTGOING);
        this.weightFunction = weightFunction;
    }

    public Dijkstra(RelationshipType relationshipType, String costProperty) {
        this(relationshipType,
                ((relationship, direction) -> EntityHelper.getDoubleProperty(relationship, costProperty)));
    }

    public ShortestPathResult find(Node start, Node goal) {
        return new Query(start, goal).getResult();
    }

    class Query {
        private final Node goal;
        private final PriorityQueue<DijkstraState> queue = new PriorityQueue<>();
        private final Map<Node, DijkstraState> seen = new HashMap<>();

        Query(Node start, Node goal) {
            this.goal = goal;
            DijkstraState init = new DijkstraState(start);
            queue.offer(init);
            seen.put(start, init);

        }

        public ShortestPathResult getResult() {
            while (!queue.isEmpty()) {
                final DijkstraState state = queue.poll();
                if (state.getEndNode().equals(goal)) {
                    return new ShortestPathResult(state.getPath(), state.getCost(), 0, queue.size());
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
            return null;
        }

        private boolean mustUpdateNeighborState(DijkstraState state, Node neighbor, Double cost) {
            return !seen.containsKey(neighbor) ||
                    !(seen.get(neighbor).settled || seen.get(neighbor).getCost() < state.getCost() + cost);
        }
    }
}
