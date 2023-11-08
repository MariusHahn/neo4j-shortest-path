package wft.hahn.neo4j.cch.search;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import wft.hahn.neo4j.cch.storage.Buffer;
import wft.hahn.neo4j.cch.storage.FifoBuffer;

@RequiredArgsConstructor
public class DiskDijkstra {

    private final FifoBuffer fifoBuffer;

    public Map<Integer, SearchPath> find(int startRank) {
        return new Query(startRank, Set.of(), fifoBuffer).getResult();
    }

    public SearchPath find(int startRank, int goalRank) {
        return new Query(startRank, Set.of(goalRank), fifoBuffer).getResult().get(goalRank);
    }

    public Map<Integer, SearchPath> find(int startRank, Collection<Integer> goals) {
        return new Query(startRank, new HashSet<>(goals), fifoBuffer).getResult();
    }

    public static class Query {
        private final Set<Integer> goals;
        private final PriorityQueue<DiskDijkstraState> queue = new PriorityQueue<>();
        private final Map<Integer, DiskDijkstraState> seen = new HashMap<>();
        private final Map<Integer, SearchPath> shortestPaths;
        private DiskDijkstraState latestExpand;
        private final VertexManager vertexManager;

        public Query(int start, Set<Integer> goals, Buffer fifoBuffer) {
            this.goals = goals;
            this.vertexManager = new VertexManager(fifoBuffer);
            DiskDijkstraState init = new DiskDijkstraState(vertexManager.getVertex(start), vertexManager);
            queue.offer(init);
            seen.put(start, init);
            shortestPaths = new HashMap<>(goals.size() * 4 / 3 );
        }

        private Map<Integer, SearchPath> getResult() {
            while (!isComplete()) {expandNext();}
            return shortestPaths;
        }

        public void expandNext() {
            final DiskDijkstraState state = queue.poll();
            latestExpand = state;
            if (goals.contains(state.getEndVertex().rank) || goals.isEmpty()) {
                shortestPaths.put(state.getEndVertex().rank, state.getPath());
            }
            final Iterable<SearchArc> arcs = state.getEndVertex().arcs;
            state.settle();
            for (SearchArc arc : arcs) {
                final SearchVertex neighbor = arc.otherVertex(state.getEndVertex());
                final int cost = arc.weight;
                if (mustUpdateNeighborState(state, neighbor, cost)) {
                    final SearchPath newPath = ExtendedSearchPath.extend(state.getPath(), arc);
                    final DiskDijkstraState newState = new DiskDijkstraState(neighbor, newPath, vertexManager);
                    queue.remove(newState);
                    queue.offer(newState);
                    seen.put(neighbor.rank, newState);
                }
            }
        }

        public boolean isComplete() {
            return queue.isEmpty() || (goals.size() == shortestPaths.size() && goals.size() != 0);
        }

        private boolean mustUpdateNeighborState(DiskDijkstraState state, SearchVertex neighbor, int cost) {
            return !seen.containsKey(neighbor.rank) ||
                    !(seen.get(neighbor.rank).settled || seen.get(neighbor.rank).weight() < state.weight() + cost);
        }

        public Map<Integer, SearchPath> resultMap() {
            return shortestPaths;
        }

        public SearchVertex latestExpand() {
            return latestExpand.getEndVertex();
        }

        public int latestWeight() {
            return shortestPaths.get(latestExpand.getEndVertex().rank).weight();
        }
    }
}
