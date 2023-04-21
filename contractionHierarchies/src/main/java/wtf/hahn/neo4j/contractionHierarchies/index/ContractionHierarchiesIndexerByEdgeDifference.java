package wtf.hahn.neo4j.contractionHierarchies.index;

import static java.lang.Math.max;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedInNodes;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedNeighbors;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedOutNodes;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;
import wtf.hahn.neo4j.contractionHierarchies.expander.NotContractedWithShortcutsExpander;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.ShortestPathResult;
import wtf.hahn.neo4j.model.inmemory.GraphLoader;
import wtf.hahn.neo4j.util.LastInsertWinsPriorityQueue;

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
        Node[] notContractedInNodes = getNotContractedInNodes(type, nodeToContract);
        Node[] notContractedOutNodes = getNotContractedOutNodes(type, nodeToContract);
        Collection<XShortcut> shortcutsToInsert = new ArrayList<>();
        for (Node inNode : notContractedInNodes) {
            Map<Node, ShortestPathResult> shortestPaths =
                    dijkstra.find(inNode, Arrays.stream(notContractedOutNodes).toList(), notYetContractedExpander);
            for (Node outNode : notContractedOutNodes) {
                if (inNode == outNode) continue;
                NodeIncludeExpander includeExpander = new NodeIncludeExpander(nodeToContract, type, rankPropertyName);
                WeightedPath includePath = dijkstra.find(inNode, outNode, includeExpander);
                if (samePath(shortestPaths.get(outNode), includePath)) {
                    Iterator<Relationship> relationshipIterator = includePath.relationships().iterator();
                    shortcutsToInsert.add(new XShortcut(relationshipIterator.next(), relationshipIterator.next(), includePath.weight()));
                }
            }
        }
        int edgeDifference = shortcutsToInsert.size() - notContractedInNodes.length - notContractedOutNodes.length;
        return new EdgeDifferenceAndShortCuts(nodeToContract, edgeDifference, shortcutsToInsert);
    }

    public int insertShortcuts() {
        int insertionCounter = 0;
        Set<Node> nodes = graphLoader.loadAllNodes(type);
        LastInsertWinsPriorityQueue<EdgeDifferenceAndShortCuts>
                queue  = new LastInsertWinsPriorityQueue<>(nodes.stream().map(this::shortCutsToInsert));
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
