package wtf.hahn.neo4j.model.inmemory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import static wtf.hahn.neo4j.model.inmemory.Modification.CREATED;
import static wtf.hahn.neo4j.model.inmemory.Modification.DELETED;
import static wtf.hahn.neo4j.model.inmemory.Modification.MODIFIED;

public class GraphLoader {

    private final Transaction transaction;

    public GraphLoader(Transaction transaction) {
        this.transaction = transaction;
    }

    public Set<Node> loadAllNodes(RelationshipType type) {
        final Map<VNode, VNode> nodes = new HashMap<>();
        for (final Relationship relationship : (Iterable<Relationship>) () -> transaction.findRelationships(type)) {
            final VNode startNode = nodes.computeIfAbsent(new VNode(relationship.getStartNode()), Function.identity());
            final VNode endNode = nodes.computeIfAbsent(new VNode(relationship.getEndNode()), Function.identity());
            final VRelationship vRelationship = new VRelationship(relationship);
            nodes.get(startNode).addOutRelationship(vRelationship);
            nodes.get(endNode).addInRelationship(vRelationship);
        }
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public void saveAllNode(Collection<Node> nodes) {
        final Iterable<VNode> vNodes = nodes.stream().map(VNode.class::cast)::iterator;
        for (final VNode vNode : vNodes) {
            final Node pNode = transaction.getNodeByElementId(vNode.getElementId());
            vNode.getProperties(MODIFIED).forEach(entry -> pNode.setProperty(entry.getKey(), entry.getValue()));
            vNode.getProperties(DELETED).map(Map.Entry::getKey).forEach(pNode::removeProperty);
            vNode.getLabels(CREATED).forEach(entry -> pNode.addLabel(entry.getKey()));
            vNode.getLabels(DELETED).forEach(entry -> pNode.removeLabel(entry.getKey()));
            vNode.getRelationships(MODIFIED).forEach(vRelationship -> {
                final Relationship pRelationship = getOrCreateRelationship(pNode, vRelationship);
                getOrCreateRelationship(pNode, vRelationship);
                vRelationship.getProperties(MODIFIED).forEach(entry -> pRelationship.setProperty(entry.getKey(), entry.getValue().property()));
                vRelationship.getProperties(DELETED).map(Map.Entry::getKey).forEach(pRelationship::removeProperty);
            });
        }
    }

    private Relationship getOrCreateRelationship(Node pNode, VRelationship vRelationship) {
        Relationship pRelationship;
        if (vRelationship.getElementId() == null) {
            Node pEndNode = transaction.getNodeByElementId(vRelationship.getEndNode().getElementId());
            pRelationship = pNode.createRelationshipTo(pEndNode, vRelationship.getType());
        } else {
            pRelationship = transaction.getRelationshipByElementId(vRelationship.getElementId());
        }
        return pRelationship;
    }
}