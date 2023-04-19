package wtf.hahn.neo4j.contractionHierarchies.index;

import static java.lang.Math.max;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedInNodes;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedOutNodes;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.inmemory.GraphLoader;
import wtf.hahn.neo4j.util.Iterables;
import wtf.hahn.neo4j.util.LastInsertWinsPriorityQueue;
import wtf.hahn.neo4j.util.StoppedResult;

public final class ContractionHierarchiesIndexerByEdgeDifference implements ContractionHierarchiesIndexer {
    private final RelationshipType type;
    private final String costProperty;
    private final NativeDijkstra dijkstra;
    private final String rankPropertyName;
    private final GraphLoader graphLoader;

    public ContractionHierarchiesIndexerByEdgeDifference(String type, String costProperty, Transaction transaction, GraphDatabaseService graphDatabaseService) {
        this.type = RelationshipType.withName(type);
        this.costProperty = costProperty;
        dijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, graphDatabaseService));
        rankPropertyName = Shortcuts.rankPropertyName(this.type);
        graphLoader = new GraphLoader(transaction);
    }

    void updateNeighborsInQueue(LastInsertWinsPriorityQueue<EdgeDifferenceAndShortCuts> queue, Node nodeToContract) {
        Set<EdgeDifferenceAndShortCuts> collect = Stream.concat(
                        Arrays.stream(getNotContractedInNodes(type, nodeToContract))
                        , Arrays.stream(getNotContractedOutNodes(type, nodeToContract)))
                .distinct()
                .map(nodeToContract1 -> new StoppedResult<>(() -> shortCutsToInsert(nodeToContract1)))
                .peek(stoppedResult -> System.err.printf(", calc: %d", stoppedResult.getMillis()))
                .map(StoppedResult::getResult)
                .collect(Collectors.toSet());
        System.out.println();
        StoppedResult<Class<Void>> classStoppedResult = new StoppedResult<>(() -> {
            collect.forEach(queue::offer);
            return Void.TYPE;
        });
        System.err.printf("Updating queue took %d micro seconds%n", classStoppedResult.getMillis());
    }

    record XShortcut(Relationship in, Relationship out , Double weight) {}
    EdgeDifferenceAndShortCuts shortCutsToInsert(Node nodeToContract) {
        Node[] notContractedInNodes = getNotContractedInNodes(type, nodeToContract);
        Node[] notContractedOutNodes = getNotContractedOutNodes(type, nodeToContract);
        Collection<XShortcut> shortcutsToInsert = new ArrayBlockingQueue<>(max(1,notContractedInNodes.length*notContractedOutNodes.length));
        record InOutNode(Node inNode, Node outNode) {}
        Arrays.stream(notContractedInNodes)
                .flatMap(inNode -> Arrays.stream(notContractedOutNodes).filter(outNode -> !inNode.equals(outNode)).map(outNode -> new InOutNode(inNode, outNode)))
                .parallel()
                .forEach(inOutNode -> {
                    Iterable<WeightedPath> shortestPaths = dijkstra.shortestPathsWithShortcuts(inOutNode.inNode, inOutNode.outNode, type, costProperty, rankPropertyName);
                    NodeIncludeExpander includeExpander = new NodeIncludeExpander(nodeToContract, type, rankPropertyName);
                    WeightedPath includePath = dijkstra.shortestPath(inOutNode.inNode, inOutNode.outNode, includeExpander, costProperty);
                    if (Iterables.stream(shortestPaths).anyMatch(path -> samePath(path, includePath))) {
                        Iterator<Relationship> relationshipIterator = includePath.relationships().iterator();
                        shortcutsToInsert.add(new XShortcut(relationshipIterator.next(), relationshipIterator.next(), includePath.weight()));
                    }
                });
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
            StoppedResult<EdgeDifferenceAndShortCuts> poll = new StoppedResult<>(() -> queue.poll());
            System.err.printf("Poll took %d micro seconds%n", poll.getMillis());
            Node nodeToContract = poll.getResult().nodeToContract;
            for (XShortcut shortcut : poll.getResult().shortcuts) {
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
