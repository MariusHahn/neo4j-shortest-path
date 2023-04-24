package wtf.hahn.neo4j.contractionHierarchies.index;

import static java.util.Arrays.asList;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedInNodes;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedNeighbors;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedOutNodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.expander.NotContractedWithShortcutsExpander;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.model.inmemory.GraphLoader;
import wtf.hahn.neo4j.util.LastInsertWinsPriorityQueue;
import wtf.hahn.neo4j.util.PathUtils;

public final class ContractionHierarchiesIndexerByEdgeDifference implements ContractionHierarchiesIndexer {
    private final RelationshipType type;
    private final String costProperty;
    private final Dijkstra dijkstra;
    private final String rankPropertyName;
    private final GraphLoader graphLoader;
    private final NotContractedWithShortcutsExpander notYetContractedExpander;

    public ContractionHierarchiesIndexerByEdgeDifference(String type, String costProperty, Transaction transaction, GraphDatabaseService graphDatabaseService) {
        this.type = RelationshipType.withName(type);
        this.costProperty = costProperty;
        rankPropertyName = Shortcuts.rankPropertyName(this.type);
        graphLoader = new GraphLoader(transaction);
        dijkstra = new Dijkstra(this.type, costProperty);
        notYetContractedExpander = new NotContractedWithShortcutsExpander(this.type, rankPropertyName);
    }

    void updateNeighborsInQueue(LastInsertWinsPriorityQueue<EdgeDifferenceAndShortCuts> queue, Node nodeToContract) {
        Stream.concat(getNotContractedNeighbors(type, nodeToContract, Direction.INCOMING)
                        , getNotContractedNeighbors(type, nodeToContract, Direction.OUTGOING))
                .distinct()
                .map(this::shortCutsToInsert)
                .forEach(queue::offer);
    }

    record XShortcut(Relationship in, Relationship out , Double weight) {}
    EdgeDifferenceAndShortCuts shortCutsToInsert(Node nodeToContract) {
        final Node[] inNodes = getNotContractedInNodes(type, nodeToContract);
        final Node[] outNodes = getNotContractedOutNodes(type, nodeToContract);
        final Collection<XShortcut> shortcuts = new ConcurrentLinkedDeque<>();
        Arrays.stream(inNodes).parallel().forEach(inNode -> {
            final Map<Node, ShortestPathResult> shortestPaths = dijkstra.find(inNode, asList(outNodes), notYetContractedExpander);
            for (int j = 0, outNodesLength = outNodes.length; j < outNodesLength; j++) {
                final Node outNode = outNodes[j];
                if (inNode == outNode) {continue;}
                ShortestPathResult shortestPath = shortestPaths.get(outNode);
                if (!(shortestPath.length() != 2 || !PathUtils.contains(shortestPath, nodeToContract))) {
                    final Iterator<Relationship> rIter = shortestPath.relationships().iterator();
                    shortcuts.add(new XShortcut(rIter.next(), rIter.next(), shortestPath.weight()));
                }
            }
        });
        final int edgeDifference = shortcuts.size() - inNodes.length - outNodes.length;
        return new EdgeDifferenceAndShortCuts(nodeToContract, edgeDifference, shortcuts);
    }

    public int insertShortcuts() {
        int insertionCounter = 0;
        Set<Node> nodes = graphLoader.loadAllNodes(type);
        LastInsertWinsPriorityQueue<EdgeDifferenceAndShortCuts>
                queue = new LastInsertWinsPriorityQueue<>(nodes.stream().map(this::shortCutsToInsert));
        int rank = 0;
        while (!queue.isEmpty()) {
            EdgeDifferenceAndShortCuts poll = queue.poll();
            Node nodeToContract = poll.nodeToContract;
            for (XShortcut shortcut : poll.shortcuts) {
                Shortcuts.create(shortcut.in, shortcut.out, costProperty, shortcut.weight);
                insertionCounter++;
            }
            nodeToContract.setProperty(rankPropertyName, rank++);
            updateNeighborsInQueue(queue, nodeToContract);
        }
        graphLoader.saveAllNode(nodes);
        return insertionCounter;
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode(of = "nodeToContract")
    private static class EdgeDifferenceAndShortCuts implements Comparable<EdgeDifferenceAndShortCuts> {
        public final Node nodeToContract;
        public final Integer edgeDifference;
        public final Collection<XShortcut> shortcuts;

        @Override
        public int compareTo(EdgeDifferenceAndShortCuts o) {
            Comparator<EdgeDifferenceAndShortCuts> c = Comparator
                    .<EdgeDifferenceAndShortCuts>comparingInt(x -> x.edgeDifference)
                    .thenComparingInt(value -> value.shortcuts.size())
                    .thenComparing(Comparator.<EdgeDifferenceAndShortCuts>comparingInt(value -> value.nodeToContract.getDegree()).reversed());
            return c.compare(this, o);
        }
    }
}
