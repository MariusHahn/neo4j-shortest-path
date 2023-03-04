package wtf.hahn.neo4j.model.inmemory;

import static wtf.hahn.neo4j.model.inmemory.Modification.CREATED;

import java.util.Objects;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class VRelationship extends VEntity implements Relationship {

    private final RelationshipType type;
    private final VNode startNode;
    private final VNode endNode;

    public VRelationship(RelationshipType type, VNode startNode, VNode endNode) {
        super();
        this.startNode = startNode;
        this.endNode = endNode;
        this.type = type;
        modification = CREATED;
    }
    public VRelationship(Relationship relationship) {
        super(relationship);
        type = relationship.getType();
        this.startNode = (VNode) relationship.getStartNode();
        this.endNode = (VNode) relationship.getEndNode();
    }

    @Override
    public Node getStartNode() {
        return startNode;
    }

    @Override
    public Node getEndNode() {
        return endNode;
    }

    @Override
    public Node getOtherNode(Node node) {
        return endNode.equals(node) ? startNode : endNode;
    }

    @Override
    public Node[] getNodes() {
        return new Node[]{startNode, endNode};
    }

    @Override
    public RelationshipType getType() {
        return type;
    }

    @Override
    public boolean isType(RelationshipType type) {
        return this.type.equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VRelationship that = (VRelationship) o;
        boolean sameElementId = !(getElementId() == null || that.getElementId() == null || !getElementId().equals(that.getElementId()));
        return sameElementId || type.equals(that.type) && startNode.equals(that.startNode) && endNode.equals(that.endNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, startNode, endNode);
    }
}
