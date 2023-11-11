package wft.hahn.neo4j.cch.search;

import java.nio.file.Path;
import java.util.PriorityQueue;
import java.util.Set;

import wft.hahn.neo4j.cch.model.BidirectionalSearchPath;
import wft.hahn.neo4j.cch.search.DiskDijkstra.Query;
import wft.hahn.neo4j.cch.storage.Buffer;
import wft.hahn.neo4j.cch.storage.FifoBuffer;
import wft.hahn.neo4j.cch.storage.Mode;
import static wft.hahn.neo4j.cch.storage.Writer.DISK_BLOCK_SIZE;

public class DiskChDijkstra implements AutoCloseable {

    private final Buffer outBuffer;
    private final Buffer inBuffer;
    public int expandedNodes;

    private int loadInvocationsLatestQuery;

    public DiskChDijkstra(Buffer outBuffer, Buffer inBuffer) {
        this.outBuffer = outBuffer;
        this.inBuffer = inBuffer;
    }

    public int getLoadInvocationsLatestQuery() {
        return loadInvocationsLatestQuery;
    }

    public DiskChDijkstra(Path basePath) {
        outBuffer = new FifoBuffer(DISK_BLOCK_SIZE, Mode.OUT, basePath);
        inBuffer = new FifoBuffer(DISK_BLOCK_SIZE, Mode.IN, basePath);
    }

    public SearchPath find(int start, int goal) {
        final int startLoadInvocations = totalLoadInvocations();
        expandedNodes = 0;
        boolean pickForward = true;
        final PriorityQueue<SearchPath> candidates = new PriorityQueue<>();
        final Query forwardQuery = new Query(start, Set.of(), outBuffer);
        final Query backwardQuery = new Query(goal, Set.of(), inBuffer);
        while (!isComplete(forwardQuery, backwardQuery, candidates.peek())) {
            final Query query = pickForward ? forwardQuery : backwardQuery;
            final Query other = pickForward ? backwardQuery : forwardQuery;
            pickForward = !pickForward;
            if (!query.isComplete()) query.expandNext(); else continue;
            expandedNodes++;
            final SearchVertex latest = query.latestExpand();
            if (other.resultMap().containsKey(latest.rank)) {
                final SearchPath forwardPath = forwardQuery.resultMap().get(latest.rank);
                final SearchPath backwardPath = backwardQuery.resultMap().get(latest.rank);
                candidates.offer(new BidirectionalSearchPath(forwardPath, backwardPath));
            }
        }
        loadInvocationsLatestQuery = totalLoadInvocations() - startLoadInvocations;
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
    public void close() {
        try {
            outBuffer.close();
            inBuffer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int totalLoadInvocations() {
        return inBuffer.getLoadInvocations() + outBuffer.getLoadInvocations();
    }
}
