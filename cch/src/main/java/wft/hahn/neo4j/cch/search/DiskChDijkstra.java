package wft.hahn.neo4j.cch.search;

import java.nio.file.Path;
import java.util.PriorityQueue;
import java.util.Set;

import wft.hahn.neo4j.cch.model.BidirectionalSearchPath;
import wft.hahn.neo4j.cch.search.DiskDijkstra.Query;
import wft.hahn.neo4j.cch.storage.BufferManager;
import wft.hahn.neo4j.cch.storage.Mode;

public class DiskChDijkstra implements AutoCloseable {

    private final BufferManager outBuffer;
    private final BufferManager inBuffer;

    public DiskChDijkstra(Path basePath) {
        outBuffer = new BufferManager(Mode.OUT, basePath);
        inBuffer = new BufferManager(Mode.IN, basePath);
    }

    public SearchPath find(int start, int goal) {
        boolean pickForward = true;
        final PriorityQueue<SearchPath> candidates = new PriorityQueue<>();
        final Query forwardQuery = new Query(start, Set.of(), outBuffer);
        final Query backwardQuery = new Query(goal, Set.of(), inBuffer);
        while (!isComplete(forwardQuery, backwardQuery, candidates.peek())) {
            final Query query = pickForward ? forwardQuery : backwardQuery;
            final Query other = pickForward ? backwardQuery : forwardQuery;
            pickForward = !pickForward;
            if (!query.isComplete()) query.expandNext(); else continue;
            final SearchVertex latest = query.latestExpand();
            if (other.resultMap().containsKey(latest.rank)) {
                final SearchPath forwardPath = forwardQuery.resultMap().get(latest.rank);
                final SearchPath backwardPath = backwardQuery.resultMap().get(latest.rank);
                candidates.offer(new BidirectionalSearchPath(forwardPath, backwardPath));
            }
        }
        return candidates.poll();
    }

    private static boolean isComplete(Query forwardQuery, Query backwardQuery, SearchPath currentBest) {
        return forwardQuery.isComplete() && backwardQuery.isComplete()
                || shortestFound(forwardQuery, backwardQuery, currentBest);
    }

    private static boolean shortestFound(Query forwardQuery, Query backwardQuery, SearchPath currentBest) {
        return currentBest != null && currentBest.weight() <= forwardQuery.latestWeight() &&
                currentBest.weight() <= backwardQuery.latestWeight();
    }

    @Override
    public void close() throws Exception {
        outBuffer.close();
        inBuffer.close();
    }
}
