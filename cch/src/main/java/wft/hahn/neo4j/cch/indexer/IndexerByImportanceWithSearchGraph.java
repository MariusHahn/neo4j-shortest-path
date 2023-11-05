package wft.hahn.neo4j.cch.indexer;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexLoader;
import wtf.hahn.neo4j.util.LastInsertWinsPriorityQueue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.lang.Math.max;

public final class IndexerByImportanceWithSearchGraph {
    private final RelationshipType type;
    private final VertexLoader vertexLoader;
    private final LastInsertWinsPriorityQueue<QueueVertex> queue;
    private final int size;
    private int insertionCounter = 0;
    private int rank = 0;
    private int maxDegree = 0;
    private Vertex vertexToContract = null;

    public IndexerByImportanceWithSearchGraph(String type, String costProperty, Transaction transaction) {
        this.type = RelationshipType.withName(type);
        vertexLoader = new VertexLoader(transaction, costProperty, this.type);
        Set<Vertex> vertices = vertexLoader.loadAllVertices();
        size = vertices.size();
        queue = new LastInsertWinsPriorityQueue<>(vertices.stream().map(v -> new QueueVertex(shortcutsToInsert(v))));
    }

    public Vertex insertShortcuts() {
        long counter = 0;
        while (!queue.isEmpty()) {
            if (counter++ % 1000 == 0) System.out.println(counter + " vertices contracted");
            final Contraction poll = shortcutsToInsert(queue.poll().vertex());
            vertexToContract = poll.vertexToContract;
            vertexToContract.rank = rank;
            vertexLoader.setRankProperty(vertexToContract, rank++, type.name()+"_rank");
            for (Shortcut shortcut : poll.shortcuts) {
                insertionCounter += createOrUpdateEdge(vertexToContract, shortcut);
            }
            updateNeighborsInQueue(queue, vertexToContract);
            maxDegree = max(maxDegree, vertexToContract.getDegree());
        }
        vertexLoader.commit();
        System.out.println("Max Degree: " + maxDegree);
        System.out.println(insertionCounter + " shortcuts inserted!");
        return vertexToContract;
    }

    private static void updateNeighborsInQueue(LastInsertWinsPriorityQueue<QueueVertex> queue, Vertex nodeToContract) {
        final Set<Vertex> seen = new HashSet<>();
        for (final Arc inArc : nodeToContract.inArcs()) {
            final Vertex neighbor = inArc.start;
            if (neighbor.rank == Vertex.UNSET && !seen.contains(neighbor)) {
                neighbor.contractedLevel = max(neighbor.contractedLevel, nodeToContract.contractedLevel);
                seen.add(neighbor);
                queue.offer(new QueueVertex(shortcutsToInsert(neighbor)));
            }

        }
        for (final Arc inArc : nodeToContract.outArcs()) {
            final Vertex neighbor = inArc.end;
            if (neighbor.rank == Vertex.UNSET && !seen.contains(neighbor)) {
                neighbor.contractedLevel = max(neighbor.contractedLevel, nodeToContract.contractedLevel);
                seen.add(neighbor);
                queue.offer(new QueueVertex(shortcutsToInsert(neighbor)));
            }
        }
    }

    private static Contraction shortcutsToInsert(final Vertex nodeToContract) {
        int outerCount = 0, innerCountTimesOuter = 0;
        final Collection<Shortcut> shortcuts = new ConcurrentLinkedDeque<>();
        for (final Arc inArc : nodeToContract.inArcs()) if (inArc.start.rank == Vertex.UNSET) {
            outerCount++;
            final Vertex inNode = inArc.start;
            for (final Arc outArc : nodeToContract.outArcs()) if (outArc.end.rank == Vertex.UNSET) {
                innerCountTimesOuter++;
                final Vertex outNode = outArc.end;
                if (inNode != outNode) shortcuts.add(new Shortcut(inArc, outArc));
            }
        }
        final int innerCount = outerCount == 0 ? 0 : innerCountTimesOuter / outerCount;
        final int edgeDifference = shortcuts.size() - outerCount - innerCount;
        return new Contraction(nodeToContract, edgeDifference, shortcuts);
    }

    private static int createOrUpdateEdge(Vertex vertexToContract, Shortcut shortcut) {
        final Vertex from = shortcut.in().start;
        final Vertex to = shortcut.out().end;
        if (from.addArc(to, vertexToContract, shortcut.weight(), shortcut.hopLength())) return 1;
        return 0;
    }
}
