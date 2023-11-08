package wft.hahn.neo4j.cch.dijkstra;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.ExtendedVertexPath;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexPath;

public class VertexDijkstra {

    public VertexPath find(Vertex start, Vertex goal) {
        return new Query(start, Set.of(goal)).getResult().get(goal);
    }
    public Map<Vertex, VertexPath> find(Vertex start, Collection<Vertex> goals) {
        return new Query(start, new HashSet<>(goals)).getResult();
    }

    public static class Query {
        private final Set<Vertex> goals;
        private final PriorityQueue<VertexDijkstraState> queue = new PriorityQueue<>();
        private final Map<Vertex, VertexDijkstraState> seen = new HashMap<>();
        private final Map<Vertex, VertexPath> shortestPaths;
        private VertexDijkstraState latestExpand;

        public Query(Vertex start, Set<Vertex> goals) {
            this.goals = goals;
            VertexDijkstraState init = new VertexDijkstraState(start);
            queue.offer(init);
            seen.put(start, init);
            shortestPaths = new HashMap<>(goals.size() * 4 / 3 );
        }

        private Map<Vertex, VertexPath> getResult() {
            while (!isComplete()) {expandNext();}
            return shortestPaths;
        }

        public void expandNext() {
            final VertexDijkstraState state = queue.poll();
            latestExpand = state;
            if (goals.contains(state.getEndVertex()) || goals.isEmpty()) {
                shortestPaths.put(state.getEndVertex(), state.getPath());
            }
            final Iterable<Arc> arcs = state.getEndVertex().outArcs();
            state.settled = true;
            for (Arc arc : arcs) if (arc.end.rank == Vertex.UNSET) {
                final Vertex neighbor = arc.otherVertex(state.getEndVertex());
                final int cost = arc.weight;
                if (mustUpdateNeighborState(state, neighbor, cost)) {
                    final VertexPath newPath = ExtendedVertexPath.extend(state.getPath(), arc);
                    final VertexDijkstraState newState = new VertexDijkstraState(neighbor, newPath);
                    queue.remove(newState);
                    queue.offer(newState);
                    seen.put(neighbor, newState);
                }
            }
        }

        public boolean isComplete() {
            return queue.isEmpty() || (goals.size() == shortestPaths.size() && goals.size() != 0);
        }

        private boolean mustUpdateNeighborState(VertexDijkstraState state, Vertex neighbor, int cost) {
            return !seen.containsKey(neighbor) ||
                    !(seen.get(neighbor).settled || seen.get(neighbor).weight() < state.weight() + cost);
        }

        public Map<Vertex, VertexPath> resultMap() {
            return shortestPaths;
        }

        public Vertex latestExpand() {
            return latestExpand.getEndVertex();
        }

        public int latestWeight() {
            return shortestPaths.get(latestExpand.getEndVertex()).weight();
        }
    }
}
