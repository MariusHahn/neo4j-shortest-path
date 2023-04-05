package wtf.hahn.neo4j.contractionHierarchies.index;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.model.Shortcuts;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static wtf.hahn.neo4j.model.Shortcuts.shortcutRelationshipType;

import java.util.function.Function;

public class IndexUtil {

    private static Node[] getNotContractedNeighbors(RelationshipType relationshipType, Node nodeToContract,
                                                    Direction direction) {
        Function<Relationship, Node> getNeighbor = direction == OUTGOING
                ? Relationship::getEndNode
                : Relationship::getStartNode;
        return nodeToContract.getRelationships(direction, relationshipType,
                        shortcutRelationshipType(relationshipType))
                .stream()
                .parallel()
                .map(getNeighbor)
                .filter(n -> !n.hasProperty(Shortcuts.rankPropertyName(relationshipType)))
                .distinct()
                .toArray(Node[]::new);
    }

    public static Node[] getNotContractedOutNodes(RelationshipType relationshipType, Node nodeToContract) {
        return getNotContractedNeighbors(relationshipType, nodeToContract, OUTGOING);
    }

    public static Node[] getNotContractedInNodes(RelationshipType relationshipType, Node nodeToContract) {
        return getNotContractedNeighbors(relationshipType, nodeToContract, INCOMING);
    }
}
