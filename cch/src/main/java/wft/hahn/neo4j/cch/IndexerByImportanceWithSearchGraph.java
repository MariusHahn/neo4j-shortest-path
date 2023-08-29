package wft.hahn.neo4j.cch;

import static java.lang.Math.max;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

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

    public IndexerByImportanceWithSearchGraph(String type, String costProperty, Transaction transaction) {
        this.type = RelationshipType.withName(type);
        this.costProperty = costProperty;
        vertexLoader = new VertexLoader(transaction);
    }

    void updateNeighborsInQueue(LastInsertWinsPriorityQueue<EdgeDifferenceAndShortcuts> queue, Vertex nodeToContract) {
        Stream.concat(nodeToContract.inNeighbors(), nodeToContract.outNeighbors()).distinct()
                .filter(neighbor -> neighbor.rank == Vertex.UNSET)
                .peek(neighbor -> neighbor.contractedLevel = max(neighbor.contractedLevel, nodeToContract.contractedLevel) +1)
                .map(this::shortcutsToInsert)
                .forEach(queue::offer);
    }

    record Shortcut(Arc in, Arc out , float weight, int hopLength) {}
    EdgeDifferenceAndShortcuts shortcutsToInsert(Vertex nodeToContract) {
        final Vertex[] inNodes = nodeToContract.inNeighbors().filter(n -> n.rank == Vertex.UNSET).toArray(Vertex[]::new);
        final Vertex[] outNodes = nodeToContract.outNeighbors().filter(n -> n.rank == Vertex.UNSET).toArray(Vertex[]::new);
        final Collection<Shortcut> shortcuts = new ConcurrentLinkedDeque<>();
        for (final Vertex inNode : inNodes) for (final Vertex outNode : outNodes) if (inNode != outNode) {
                final Arc from = inNode.outArcs().stream().filter(arc -> nodeToContract.equals(arc.end)).findFirst().orElseThrow();
                final Arc to = outNode.inArcs().stream().filter(arc -> nodeToContract.equals(arc.start)).findFirst().orElseThrow();
                shortcuts.add(new Shortcut(from, to, from.weight + to.weight,from.hopLength + to.hopLength));
            }
        final int edgeDifference = shortcuts.size() - inNodes.length - outNodes.length;
        return new EdgeDifferenceAndShortcuts(nodeToContract, edgeDifference, shortcuts);
    }

    public Vertex insertShortcuts() {
        Set<Vertex> vertices = vertexLoader.loadAllVertices(costProperty, type);
        LastInsertWinsPriorityQueue<EdgeDifferenceAndShortcuts> queue =
                new LastInsertWinsPriorityQueue<>(vertices.stream().map(this::shortcutsToInsert));
        int rank = 0;
        Vertex vertexToContract = null;
        while (!queue.isEmpty()) {
            EdgeDifferenceAndShortcuts poll = queue.poll();
            vertexToContract = poll.vertexToContract;
            vertexToContract.rank = rank;
            vertexLoader.setRankProperty(vertexToContract, rank++, type.name()+"_rank");
            for (Shortcut shortcut : poll.shortcuts) {
                createOrUpdateEdge(vertexToContract, shortcut);
            }
            updateNeighborsInQueue(queue, vertexToContract);
        }
        vertexLoader.commit();
        System.out.println(insertionCounter + " shortcuts inserted!");
        return vertexToContract;
    }

    private void createOrUpdateEdge(Vertex vertexToContract, Shortcut shortcut) {
        final Vertex from = shortcut.in.start;
        final Vertex to = shortcut.out.end;
        final Arc existing = from.outArcs().stream().filter(arc -> arc.end.equals(to)).findFirst().orElse(null);
        if (existing != null) {
            if (shortcut.weight < existing.weight) {
                existing.weight = shortcut.weight;
                existing.hopLength = shortcut.hopLength;
                existing.middle = vertexToContract;
            }
        } else {
            Arc arc = new Arc(from, to, shortcut.weight, vertexToContract, shortcut.hopLength);
            shortcut.in.start.addArc(arc);
            shortcut.out.end.addArc(arc);
            insertionCounter++;
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
                    + (shortcuts.stream().mapToInt(sc -> sc.hopLength).sum() * 1.0
                               /
                       vertexToContract.arcs().mapToInt(arc -> arc.hopLength).sum())
                    ;
        }
    }
}
