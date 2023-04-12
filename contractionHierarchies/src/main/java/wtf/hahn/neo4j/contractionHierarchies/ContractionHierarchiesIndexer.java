package wtf.hahn.neo4j.contractionHierarchies;

import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;
import wtf.hahn.neo4j.dijkstra.NativeDijkstra;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.util.Iterables;

public record ContractionHierarchiesIndexer(RelationshipType type, String costProperty, Transaction transaction, List<Node> nodes,
                                            RelationshipType shortcutType, Comparator<Node> contractionOrderComparator, NativeDijkstra dijkstra, String rankPropertyName) {

    public ContractionHierarchiesIndexer(String type, String costProperty, Transaction transaction, Comparator<Node> contractionOrderComparator, GraphDatabaseService graphDatabaseService) {
        this(
                RelationshipType.withName(type)
                , costProperty
                , transaction
                , loadAllNodes(RelationshipType.withName(type), transaction)
                , Shortcuts.shortcutRelationshipType(RelationshipType.withName(type))
                , contractionOrderComparator
                , new NativeDijkstra(new BasicEvaluationContext(transaction, graphDatabaseService))
                , Shortcuts.rankPropertyName(RelationshipType.withName(type))
        );
    }

    public int insertShortcuts() {
        int insertionCounter = 0;
        PriorityQueue<Node> queue = new PriorityQueue<>(nodes.size(), contractionOrderComparator);
        queue.addAll(nodes);
        int rank = 0;
        while (!queue.isEmpty()) {
            Node nodeToContract = queue.poll();
            Node[] inNodes = getNotContractedInNodes(type, nodeToContract);
            Node[] outNodes = getNotContractedOutNodes(type, nodeToContract);
            for (Node inNode : inNodes) {
                for (Node outNode : outNodes) {
                    if (inNode.equals(outNode)) continue;
                    Iterable<WeightedPath> shortestPaths = dijkstra.shortestPathsWithShortcuts(inNode, outNode, type, costProperty, rankPropertyName);
                    NodeIncludeExpander includeExpander = new NodeIncludeExpander(nodeToContract, type, rankPropertyName);
                    WeightedPath includePath = dijkstra.shortestPath(inNode, outNode, includeExpander, costProperty);
                    if (Iterables.stream(shortestPaths).anyMatch(path -> samePath(path, includePath))) {
                        insertionCounter++;
                        Iterator<Relationship> relationshipIterator = includePath.relationships().iterator();
                        Shortcuts.create(relationshipIterator.next(), relationshipIterator.next(), costProperty,
                                includePath.weight());
                    }
                }
            }
            nodeToContract.setProperty(rankPropertyName, rank++);
        }
        return insertionCounter;
    }

    private static Node[] getNotContractedNeighbors(RelationshipType relationshipType, Node nodeToContract,
                                                    Direction direction) {
        Function<Relationship, Node> getEndNode =
                direction == OUTGOING ? Relationship::getEndNode : Relationship::getStartNode;
        return nodeToContract.getRelationships(direction, relationshipType,
                        Shortcuts.shortcutRelationshipType(relationshipType))
                .stream()
                .map(getEndNode)
                .filter(n -> !n.hasProperty(Shortcuts.rankPropertyName(relationshipType)))
                .distinct()
                .toArray(Node[]::new);
    }

    static Node[] getNotContractedOutNodes(RelationshipType relationshipType, Node nodeToContract) {
        return getNotContractedNeighbors(relationshipType, nodeToContract, OUTGOING);
    }

    static Node[] getNotContractedInNodes(RelationshipType relationshipType, Node nodeToContract) {
        return getNotContractedNeighbors(relationshipType, nodeToContract, INCOMING);
    }

    static List<Node> loadAllNodes(RelationshipType relationshipType, Transaction transaction) {
        return transaction.findRelationships(relationshipType).stream()
                .map(Relationship::getNodes)
                .flatMap(Arrays::stream).distinct()
                .toList();
    }
}
