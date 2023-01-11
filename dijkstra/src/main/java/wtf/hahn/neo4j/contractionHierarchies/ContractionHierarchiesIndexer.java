package wtf.hahn.neo4j.contractionHierarchies;

import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;

public record ContractionHierarchiesIndexer(RelationshipType type, String costProperty, Transaction transaction, List<Node> nodes,
                                            RelationshipType shortcutType, Comparator<Node> contractionOrderComparator) {

    public ContractionHierarchiesIndexer(String type, String costProperty, Transaction transaction, Comparator<Node> contractionOrderComparator) {
        this(
                RelationshipType.withName(type)
                , costProperty
                , transaction
                , loadAllNodes(RelationshipType.withName(type), transaction)
                , Shortcut.shortcutRelationshipType(RelationshipType.withName(type))
                , contractionOrderComparator
        );
    }

    public void insertShortcuts() {
        PriorityQueue<Node> queue = new PriorityQueue<>(nodes.size(), contractionOrderComparator);
        queue.addAll(nodes);
        int rank = 0;
        while (!queue.isEmpty()) {
            Node nodeToContract = queue.poll();
            nodeToContract.setProperty(Shortcut.rankPropertyName(type), rank++);
            Node[] inNodes = getNotContractedInNodes(type, nodeToContract);
            Node[] outNodes = getNotContractedOutNodes(type, nodeToContract);
            for (Node inNode : inNodes) {
                for (Node outNode : outNodes) {
                    WeightedPath shortestPath = shortestPathWithShortcuts(inNode, outNode, type, costProperty);
                    NodeIncludeExpander includeExpander = new NodeIncludeExpander(nodeToContract, type);
                    WeightedPath includePath = shortestPath(inNode, outNode, includeExpander, costProperty);
                    if (samePath(shortestPath, includePath)) {
                        new Shortcut(type, inNode, outNode, costProperty, includePath).create();
                    }
                }
            }
        }
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

    private static List<Node> loadAllNodes(RelationshipType relationshipType, Transaction transaction) {
        return transaction.findRelationships(relationshipType).stream()
                .map(Relationship::getNodes)
                .flatMap(Arrays::stream).distinct()
                .toList();
    }

    private static WeightedPath shortestPathWithShortcuts(Node startNode, Node endNode, RelationshipType type, String costProperty) {
        final PathExpander<Double> standardExpander = PathExpanders.forTypesAndDirections(
                type
                , OUTGOING
                , Shortcut.shortcutRelationshipType(type)
                , OUTGOING
        );
        return GraphAlgoFactory
                .dijkstra(standardExpander, costProperty, 1)
                .findSinglePath(startNode, endNode);
    }

    private static WeightedPath shortestPath(Node startNode, Node endNode, PathExpander<Double> expander, String costProperty) {
        PathFinder<WeightedPath> dijkstraFinder = GraphAlgoFactory.dijkstra(expander, costProperty, 1);
        return dijkstraFinder.findSinglePath(startNode, endNode);
    }
}
