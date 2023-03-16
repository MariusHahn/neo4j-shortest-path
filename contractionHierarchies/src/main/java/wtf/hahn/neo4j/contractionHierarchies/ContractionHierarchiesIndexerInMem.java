package wtf.hahn.neo4j.contractionHierarchies;

import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;
import wtf.hahn.neo4j.model.Shortcut;
import wtf.hahn.neo4j.model.inmemory.GraphLoader;
import wtf.hahn.neo4j.util.Iterables;

public record ContractionHierarchiesIndexerInMem(RelationshipType type, String costProperty, Transaction transaction,
                                                 RelationshipType shortcutType, Comparator<Node> contractionOrderComparator, NativeDijkstra dijkstra, String rankPropertyName,
                                                 GraphLoader graphLoader

) {

    public ContractionHierarchiesIndexerInMem(String type, String costProperty, Transaction transaction, Comparator<Node> contractionOrderComparator) {
        this(
                RelationshipType.withName(type)
                , costProperty
                , transaction
                , Shortcut.shortcutRelationshipType(RelationshipType.withName(type))
                , contractionOrderComparator
                , new NativeDijkstra()
                , Shortcut.rankPropertyName(RelationshipType.withName(type))
                , new GraphLoader(transaction)
        );
    }

    public void insertShortcuts() {
        Set<Node> nodes = graphLoader.loadAllNodes(type());
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
                        new Shortcut(type, inNode, outNode, costProperty, includePath).create();
                    }
                }
            }
            nodeToContract.setProperty(rankPropertyName, rank++);
        }
        graphLoader.saveAllNode(nodes);
    }

    private static Node[] getNotContractedNeighbors(RelationshipType relationshipType, Node nodeToContract,
                                                    Direction direction) {
        Function<Relationship, Node> getEndNode =
                direction == OUTGOING ? Relationship::getEndNode : Relationship::getStartNode;
        return nodeToContract.getRelationships(direction, relationshipType,
                        Shortcut.shortcutRelationshipType(relationshipType))
                .stream()
                .map(getEndNode)
                .filter(n -> !n.hasProperty(Shortcut.rankPropertyName(relationshipType)))
                .distinct()
                .toArray(Node[]::new);
    }

    private static Node[] getNotContractedOutNodes(RelationshipType relationshipType, Node nodeToContract) {
        return getNotContractedNeighbors(relationshipType, nodeToContract, OUTGOING);
    }

    private static Node[] getNotContractedInNodes(RelationshipType relationshipType, Node nodeToContract) {
        return getNotContractedNeighbors(relationshipType, nodeToContract, INCOMING);
    }
}
