package wtf.hahn.neo4j.procedure;

import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.util.IterationHelper.stream;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.dijkstra.Neo4jDijkstra;
import wtf.hahn.neo4j.dijkstra.expander.NodeRestrictedExpander;

public class ContractionHierarchies {

    @Context
    public GraphDatabaseService graphDatabaseService;

    @SuppressWarnings("unused")
    @Procedure(mode = Mode.WRITE)
    public void createContractionHierarchiesIndex(@Name("type") String type,
                                                  @Name("costProperty") String costProperty) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        try (Transaction transaction = graphDatabaseService.beginTx()) {
            final List<Node> nodes = loadAllNodes(relationshipType, transaction);
            insertShortcuts(relationshipType, costProperty, nodes);
            transaction.commit();
        }
    }

    private void insertShortcuts(RelationshipType relationshipType, String costProperty, List<Node> nodes) {
        Comparator<Node> comparator = Comparator.<Node>comparingInt(Node::getDegree).reversed();
        PriorityQueue<Node> queue = new PriorityQueue<>(nodes.size(), comparator);
        queue.addAll(nodes);
        int rank = 0;
        while (!queue.isEmpty()) {
            Node nodeToContract = queue.poll();
            nodeToContract.setProperty(rankPropertyName(relationshipType), rank++);
            Node[] inNodes = getNotContractedInNodes(relationshipType, nodeToContract);
            Node[] outNodes = getNotContractedOutNodes(relationshipType, nodeToContract);
            for (Node inNode : inNodes) {
                for (Node outNode : outNodes) {
                    WeightedPath restrictedPath = new Neo4jDijkstra().shortestPath(inNode, outNode,
                            new NodeRestrictedExpander(nodeToContract, relationshipType), costProperty);
                    WeightedPath nonRestricted = new Neo4jDijkstra().shortestPath(inNode, outNode,
                            PathExpanders.forTypesAndDirections(relationshipType, OUTGOING, shortcutRelationshipType(relationshipType), OUTGOING), costProperty);
                    if (restrictedPath == null || restrictedPath.weight() > nonRestricted.weight()) {
                        if (isShortestLocalPath(nonRestricted, nodeToContract)) {
                            Relationship shortcut = inNode.createRelationshipTo(outNode, shortcutRelationshipType(relationshipType));
                            shortcut.setProperty(costProperty, nonRestricted.weight());
                        }
                    }
                }
            }
        }
    }

    private static boolean isShortestLocalPath(Path nonRestricted, Node nodeToContract) {
        return stream(nonRestricted.nodes()).anyMatch(nodeToContract::equals);
    }

    private static Node[] getNotContractedNeighbors(RelationshipType relationshipType, Node nodeToContract, Direction direction) {
        Function<Relationship, Node> getEndNode = direction == OUTGOING ? Relationship::getEndNode : Relationship::getStartNode;
        return nodeToContract.getRelationships(direction, relationshipType, shortcutRelationshipType(relationshipType))
                .stream()
                .map(getEndNode)
                .filter(n -> !n.hasProperty(rankPropertyName(relationshipType)))
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
                .collect(Collectors.toList());
    }

    private static RelationshipType shortcutRelationshipType(RelationshipType relationshipType) {
        return RelationshipType.withName("sc_" + relationshipType.name());
    }

    private static String rankPropertyName(RelationshipType relationshipType) {
        return relationshipType.name() + "_rank";
    }
}
