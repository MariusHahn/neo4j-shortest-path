package wft.hahn.neo4j.cch;

import static java.lang.Math.max;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexLoader;
import wtf.hahn.neo4j.util.LastInsertWinsPriorityQueue;

public final class IndexerByImportanceWithSearchGraph {
    private final RelationshipType type;
    private final String costProperty;
    private final VertexLoader vertexLoader;
    private int insertionCounter = 0;
    private int maxDegree = 0;

    public IndexerByImportanceWithSearchGraph(String type, String costProperty, Transaction transaction) {
        this.type = RelationshipType.withName(type);
        this.costProperty = costProperty;
        vertexLoader = new VertexLoader(transaction);
    }

    static void updateNeighborsInQueue(LastInsertWinsPriorityQueue<QueueVertex> queue, Vertex nodeToContract) {
        final Set<Vertex> seen = new HashSet<>();
        for (final Arc inArc : nodeToContract.inArcs()) {
            Vertex neighbor = inArc.start;
            if (neighbor.rank == Vertex.UNSET && !seen.contains(neighbor)) {
                neighbor.contractedLevel = max(neighbor.contractedLevel, nodeToContract.contractedLevel);
                seen.add(neighbor);
                queue.offer(new QueueVertex(shortcutsToInsert(neighbor)));
            }

        }
        for (final Arc inArc : nodeToContract.outArcs()) {
            Vertex neighbor = inArc.end;
            if (neighbor.rank == Vertex.UNSET && !seen.contains(neighbor)) {
                neighbor.contractedLevel = max(neighbor.contractedLevel, nodeToContract.contractedLevel);
                seen.add(neighbor);
                queue.offer(new QueueVertex(shortcutsToInsert(neighbor)));
            }
        }
    }

    record Shortcut(Arc in, Arc out , float weight, int hopLength) {
        Shortcut(Arc in, Arc out) {
            this(in, out, in.weight + out.weight, in.hopLength + out.hopLength);
        }
    }
    static EdgeDifferenceAndShortcuts shortcutsToInsert(final Vertex nodeToContract) {
        int outerCount = 0;
        int innerCountTimesOuter = 0;
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
        return new EdgeDifferenceAndShortcuts(nodeToContract, edgeDifference, shortcuts);
    }

    public Vertex insertShortcuts() {
        Set<Vertex> vertices = vertexLoader.loadAllVertices(costProperty, type);
        LastInsertWinsPriorityQueue<QueueVertex> queue =
                new LastInsertWinsPriorityQueue<>(vertices.stream().map(
                        nodeToContract -> new QueueVertex(shortcutsToInsert(nodeToContract))));
        int rank = 0;
        Vertex vertexToContract = null;
        while (!queue.isEmpty()) {
            EdgeDifferenceAndShortcuts poll = shortcutsToInsert(queue.poll().vertex);
            vertexToContract = poll.vertexToContract;
            vertexToContract.rank = rank;
            vertexLoader.setRankProperty(vertexToContract, rank++, type.name()+"_rank");
            for (Shortcut shortcut : poll.shortcuts) {
                createOrUpdateEdge(vertexToContract, shortcut);
            }
            updateNeighborsInQueue(queue, vertexToContract);
            maxDegree = max(maxDegree, vertexToContract.getDegree());
        }
        vertexLoader.commit();
        System.out.println("Max Degree: " + maxDegree);
        System.out.println(insertionCounter + " shortcuts inserted!");
        return vertexToContract;
    }

    private void createOrUpdateEdge(Vertex vertexToContract, Shortcut shortcut) {
        final Vertex from = shortcut.in.start;
        final Vertex to = shortcut.out.end;
        if (from.addArc(to, vertexToContract, shortcut.weight, shortcut.hopLength)) insertionCounter++;
    }

    record QueueVertex(double importance, Vertex vertex) implements Comparable<QueueVertex> {
        public QueueVertex(EdgeDifferenceAndShortcuts e) {
            this(e.importance(), e.vertexToContract);
        }

        @Override
        public int compareTo(QueueVertex o) {
            return Double.compare(importance(), o.importance());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            QueueVertex that = (QueueVertex) o;
            return vertex.equals(that.vertex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertex);
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode(of = "vertexToContract")
    private static class EdgeDifferenceAndShortcuts implements Comparable<EdgeDifferenceAndShortcuts> {
        public final Vertex vertexToContract;
        public final Integer edgeDifference;
        public final Collection<Shortcut> shortcuts;

        @Override
        public int compareTo(EdgeDifferenceAndShortcuts o) {
            return Double.compare(importance(), o.importance());
        }

        double importance() {
            return vertexToContract.contractedLevel // L(x)
                    + (shortcuts.size() * 1.0 / vertexToContract.getDegree()) // |A(x)| / |D(x)|
                    + (shortcutsHopLength()
                               /
                    vertexToContract.sumOfAtoDxHa())
                    ;
        }

        private double shortcutsHopLength() {
            int c = 0; for (Shortcut shortcut : shortcuts) c+= shortcut.hopLength; return c;
        }
    }
}
