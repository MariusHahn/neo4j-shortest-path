package wft.hahn.neo4j.cch.search;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import wft.hahn.neo4j.cch.model.ExtendedVertexPath;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexPath;
import wft.hahn.neo4j.cch.storage.BufferManager;

@RequiredArgsConstructor
public class DiskDijkstra {
    private final BufferManager bufferManager;


    public VertexPath find(int startRank, int goalRank) {
        return new Query(startRank, Set.of(goalRank), bufferManager).getResult().get(goalRank);
    }

    public Map<Vertex, VertexPath> find(int startRank, Collection<Integer> goals) {
        return new Query(startRank, new HashSet<>(goals), bufferManager).getResult();
    }

    public static class Query {
        private final Set<Integer> goals;
        private final PriorityQueue<DiskDijkstraState> queue = new PriorityQueue<>();
        private final Map<Integer, DiskDijkstraState> seen = new HashMap<>();
        private final Map<Integer, VertexPath> shortestPaths;
        private DiskDijkstraState latestExpand;
        private final VertexManager vertexManager;

        public Query(int start, Set<Integer> goals, BufferManager bufferManager) {
            this.goals = goals;
            this.vertexManager = new VertexManager(bufferManager);
            DiskDijkstraState init = new DiskDijkstraState(start);
            queue.offer(init);
            seen.put(start, init);
            shortestPaths = new HashMap<>(goals.size() * 4 / 3 );
        }

        private Map<Vertex, VertexPath> getResult() {
            while (!isComplete()) {expandNext();}
            return shortestPaths;
        }

        public void expandNext() {
            final DiskDijkstraState state = queue.poll();
            latestExpand = state;
            if (goals.contains(state.getEndVertex()) || goals.isEmpty()) {
                shortestPaths.put(state.getEndVertex(), state.getPath());
            }
            final Iterable<SearchArc> arcs = state.getEndVertex().arcs;
            state.settle();
            for (SearchArc arc : arcs) {
                final Vertex neighbor = arc.otherVertex(state.getEndVertex());
                final float cost = arc.weight;
                if (mustUpdateNeighborState(state, neighbor, cost)) {
                    final VertexPath newPath = ExtendedVertexPath.extend(state.getPath(), arc);
                    final DiskDijkstraState newState = new DiskDijkstraState(neighbor, newPath);
                    queue.remove(newState);
                    queue.offer(newState);
                    seen.put(neighbor, newState);
                }
            }
        }

        public boolean isComplete() {
            return queue.isEmpty() || (goals.size() == shortestPaths.size() && goals.size() != 0);
        }

        private boolean mustUpdateNeighborState(DiskDijkstraState state, Vertex neighbor, float cost) {
            return !seen.containsKey(neighbor) ||
                    !(seen.get(neighbor).settled || seen.get(neighbor).weight() < state.weight() + cost);
        }

        public Map<Vertex, VertexPath> resultMap() {
            return shortestPaths;
        }

        public Vertex latestExpand() {
            return latestExpand.getEndVertex();
        }

        public float latestWeight() {
            return shortestPaths.get(latestExpand.getEndVertex()).weight();
        }
    }

    @RequiredArgsConstructor
    public static class VertexManager {
        private final BufferManager bufferManager;
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

        Collection<SearchArc> arcs(int rank) {
            final Collection<SearchArc> searchArcs = new LinkedList<>();
            for (BufferManager.BufferArc arc : bufferManager.arcs(rank)) {
                searchArcs.add(new SearchArc(getVertex(arc.s()), getVertex(arc.t()), getVertex(arc.m()), arc.weight()));
            }
            return searchArcs;
        }
    }
}
