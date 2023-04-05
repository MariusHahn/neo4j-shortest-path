package wtf.hahn.neo4j.contractionHierarchies.index;

import static java.lang.Math.max;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedInNodes;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedOutNodes;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.search.NativeDijkstra;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.inmemory.GraphLoader;
import wtf.hahn.neo4j.util.Iterables;

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

    void updateNeighborsInQueue(Map<Node, EdgeDifferenceAndShortCuts> queue, Node nodeToContract) {
        Stream.concat(
                Arrays.stream(getNotContractedInNodes(type, nodeToContract))
                , Arrays.stream(getNotContractedOutNodes(type, nodeToContract)))
                .distinct()
                .forEach(node -> queue.put(node, shortCutsToInsert(node)));
    }

    record XShortcut(Relationship in, Relationship out , Double weight) {}
    record EdgeDifferenceAndShortCuts(Integer edgeDifference, Collection<XShortcut> shortcuts) {}
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
                    if (/*Iterables.stream(shortestPaths).count() == 1 &&*/ Iterables.stream(shortestPaths).anyMatch(path -> samePath(path, includePath))) {
                        Iterator<Relationship> relationshipIterator = includePath.relationships().iterator();
                        shortcutsToInsert.add(new XShortcut(relationshipIterator.next(), relationshipIterator.next(), includePath.weight()));
                    }
                });
        int edgeDifference = shortcutsToInsert.size() - notContractedInNodes.length - notContractedOutNodes.length;
        return new EdgeDifferenceAndShortCuts(edgeDifference, shortcutsToInsert);
    }

    Node getBestNext(Map<Node, EdgeDifferenceAndShortCuts> x) {
        Map.Entry<Node, EdgeDifferenceAndShortCuts> currentBest = null;
        for (Map.Entry<Node, EdgeDifferenceAndShortCuts> entry : x.entrySet()) {
            if (currentBest == null || entry.getValue().edgeDifference() < currentBest.getValue().edgeDifference()) {
                currentBest = entry;
            } else if (entry.getValue().edgeDifference().equals(currentBest.getValue().edgeDifference()) && entry.getValue().shortcuts().size() < currentBest.getValue().shortcuts().size()) {
                currentBest = entry;
            } else if (entry.getValue().edgeDifference().equals(currentBest.getValue().edgeDifference()) && entry.getValue().shortcuts().size() == currentBest.getValue().shortcuts().size() && entry.getKey().getDegree() > currentBest.getKey().getDegree()) {
                currentBest = entry;
            }
        }
        return currentBest.getKey();
    }

    public int insertShortcuts() {
        int insertionCounter = 0;
        Set<Node> nodes = graphLoader.loadAllNodes(type);
        Map<Node, EdgeDifferenceAndShortCuts> queue = nodes.stream().collect(Collectors.toMap(x -> x, this::shortCutsToInsert));
        int rank = 0;
        while (!queue.isEmpty()) {
            Node nodeToContract = getBestNext(queue);
            for (XShortcut shortcut : queue.get(nodeToContract).shortcuts) {
                Shortcuts.create(shortcut.in, shortcut.out, costProperty, shortcut.weight);
                insertionCounter++;
            }
            queue.remove(nodeToContract);
            nodeToContract.setProperty(rankPropertyName, rank++);
            updateNeighborsInQueue(queue, nodeToContract);
        }
        graphLoader.saveAllNode(nodes);
        return insertionCounter;
    }
}
