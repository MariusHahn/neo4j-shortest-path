package wft.hahn.neo4j.cch.search;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.BufferManager;

@RequiredArgsConstructor
public class DiskDijkstra {

    enum Mode {UPWARD, DOWNWARD, CLASSIC}
    private final BufferManager bufferManager;

    public SearchPath find(int startRank, int goalRank, Mode mode) {
        return new Query(startRank, Set.of(goalRank), bufferManager, mode).getResult().get(goalRank);
    }

    public Map<Integer, SearchPath> find(int startRank, Collection<Integer> goals, Mode mode) {
        return new Query(startRank, new HashSet<>(goals), bufferManager, mode).getResult();
    }

    public static class Query {
        private final Set<Integer> goals;
        private final PriorityQueue<DiskDijkstraState> queue = new PriorityQueue<>();
        private final Map<Integer, DiskDijkstraState> seen = new HashMap<>();
        private final Map<Integer, SearchPath> shortestPaths;
        private DiskDijkstraState latestExpand;
        private final VertexManager vertexManager;

        public Query(int start, Set<Integer> goals, BufferManager bufferManager,
                     Mode mode) {
            this.goals = goals;
            this.vertexManager = new VertexManager(bufferManager, mode);
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
                final float cost = arc.weight;
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

        private boolean mustUpdateNeighborState(DiskDijkstraState state, SearchVertex neighbor, float cost) {
            return !seen.containsKey(neighbor.rank) ||
                    !(seen.get(neighbor.rank).settled || seen.get(neighbor.rank).weight() < state.weight() + cost);
        }

        public Map<Integer, SearchPath> resultMap() {
            return shortestPaths;
        }

        public SearchVertex latestExpand() {
            return latestExpand.getEndVertex();
        }

        public float latestWeight() {
            return shortestPaths.get(latestExpand.getEndVertex().rank).weight();
        }
    }

    @RequiredArgsConstructor
    public static class VertexManager {
        private final BufferManager bufferManager;
        private final Mode mode;
        private final Map<Integer, SearchVertex> vertices = new HashMap<>();

        public SearchVertex getVertex(int rank) {
            if (!vertices.containsKey(rank)) {
                Collection<BufferManager.BufferArc> arcs = bufferManager.arcs(rank);
                for (BufferManager.BufferArc arc : arcs) {
                    SearchVertex start = vertices.computeIfAbsent(arc.s(), SearchVertex::new);
                    SearchVertex end = vertices.computeIfAbsent(arc.t(), SearchVertex::new);
                    if (arc.m() != Vertex.UNSET) {
                        vertices.computeIfAbsent(arc.s(), SearchVertex::new);
                    }
                    start.addArc(new SearchArc(start, end, vertices.get(arc.m()), arc.weight()));
                }
            }
            return vertices.get(rank);
        }

        void addArcs(SearchVertex vertex) {
            final int rank = vertex.rank;
            for (BufferManager.BufferArc arc : bufferManager.arcs(rank)) {
                if (Mode.UPWARD.equals(mode) && rank < arc.t()) {
                    vertex.addArc(new SearchArc(vertex, getVertex(arc.t()), getVertex(arc.m()), arc.weight()));
                }
                if (Mode.DOWNWARD.equals(mode) && rank > arc.t()) {
                    vertex.addArc(new SearchArc(getVertex(arc.t()), vertex, getVertex(arc.m()), arc.weight()));
                }
                vertex.addArc(new SearchArc(vertex, getVertex(arc.t()), getVertex(arc.m()), arc.weight()));
            }
        }
    }
}
