package wft.hahn.neo4j.cch;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.dijkstra.VertexDijkstra;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexLoader;
import wft.hahn.neo4j.cch.model.VertexPath;
import wft.hahn.neo4j.cch.model.VertexPaths;
import wtf.hahn.neo4j.util.LastInsertWinsPriorityQueue;

public final class IndexerByImportanceWithSearchGraph {
    private final RelationshipType type;
    private final String costProperty;
    private final VertexDijkstra dijkstra;
    private final VertexLoader vertexLoader;

    public IndexerByImportanceWithSearchGraph(String type, String costProperty, Transaction transaction) {
        this.type = RelationshipType.withName(type);
        this.costProperty = costProperty;
        vertexLoader = new VertexLoader(transaction);
        dijkstra = new VertexDijkstra();
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
        for (int i = 0, inNodesLength = inNodes.length; i < inNodesLength; i++) {
            final Vertex inNode = inNodes[i];
            final Map<Vertex, VertexPath> shortestPaths = dijkstra.find(inNode, asList(outNodes));
            for (int j = 0, outNodesLength = outNodes.length; j < outNodesLength; j++) {
                final Vertex outNode = outNodes[j];
                if (inNode == outNode) {
                    continue;
                }
                final VertexPath shortestPath = shortestPaths.get(outNode);
                if (!(shortestPath.length() != 2 || !VertexPaths.contains(shortestPath, nodeToContract))) {
                    final Iterator<Arc> rIter = shortestPath.arcs().iterator();
                    final Arc from = rIter.next();
                    final Arc to = rIter.next();
                    shortcuts.add(new Shortcut(from, to, shortestPath.weight(), from.hopLength + to.hopLength));
                }
            }
        }
        final int edgeDifference = shortcuts.size() - inNodes.length - outNodes.length;
        return new EdgeDifferenceAndShortcuts(nodeToContract, edgeDifference, shortcuts);
    }

    public Vertex insertShortcuts() {
        int insertionCounter = 0;
        Set<Vertex> vertices = vertexLoader.loadAllVertices(type, costProperty);
        LastInsertWinsPriorityQueue<EdgeDifferenceAndShortcuts> queue =
                new LastInsertWinsPriorityQueue<>(vertices.stream().map(this::shortcutsToInsert))
                ;
        int rank = 0;
        Vertex vertexToContract = null;
        while (!queue.isEmpty()) {
            EdgeDifferenceAndShortcuts poll = queue.poll();
            vertexToContract = poll.vertexToContract;
            vertexToContract.rank = rank;
            vertexLoader.setRankProperty(vertexToContract, rank++, type.name()+"_rank");
            for (Shortcut shortcut : poll.shortcuts) {
                Arc arc = new Arc(shortcut.in.start, shortcut.out.end, shortcut.weight, vertexToContract, shortcut.hopLength);
                shortcut.in.start.addArc(arc);
                shortcut.out.end.addArc(arc);
                insertionCounter++;
            }
            updateNeighborsInQueue(queue, vertexToContract);
        }
        vertexLoader.commit();
        System.out.println(insertionCounter + " shortcuts inserted!");
        return vertexToContract;
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
                    + (shortcuts.size()*1.0 / vertexToContract.getDegree()) // |A(x)| / |D(x)|
                    + (shortcuts.stream().mapToInt(sc -> sc.hopLength).sum() * 1.0
                               /
                       vertexToContract.arcs().mapToInt(arc -> arc.hopLength).sum())
                    ;
        }

    }
}
