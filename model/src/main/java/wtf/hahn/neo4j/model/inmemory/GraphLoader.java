package wtf.hahn.neo4j.model.inmemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.model.Shortcuts;
import static wtf.hahn.neo4j.model.inmemory.Modification.CREATED;
import static wtf.hahn.neo4j.model.inmemory.Modification.DELETED;
import static wtf.hahn.neo4j.model.inmemory.Modification.MODIFIED;

public class GraphLoader {

    private final Transaction transaction;
    private final List<VRelationship> createdRelationships = new ArrayList<>();

    boolean addCreatedRelationship(VRelationship vRelationship) {
        return createdRelationships.add(vRelationship);
    }

    void createAllRelationships() {
        Map<String, String> elementIdMapping = new HashMap<>();
        for (VRelationship vRelationship : createdRelationships) {
            Node pStartNode = transaction.getNodeByElementId(vRelationship.getStartNode().getElementId());
            Node pEndNode = transaction.getNodeByElementId(vRelationship.getEndNode().getElementId());
            Relationship pRelationship = pStartNode.createRelationshipTo(pEndNode, vRelationship.getType());
            elementIdMapping.put(vRelationship.getElementId(), pRelationship.getElementId());
            vRelationship.getProperties(MODIFIED).forEach(entry -> {
                if (Shortcuts.IN_RELATION.equals(entry.getKey()) || Shortcuts.OUT_RELATION.equals(entry.getKey())) {
                    String mixedId = (String) entry.getValue().property();
                    String elementId = elementIdMapping.getOrDefault(mixedId, mixedId);
                    pRelationship.setProperty(entry.getKey(), elementId);
                } else {
                    pRelationship.setProperty(entry.getKey(), entry.getValue().property());
                }
            });
            vRelationship.getProperties(DELETED).map(Map.Entry::getKey).forEach(pRelationship::removeProperty);
        }
    }

    public GraphLoader(Transaction transaction) {
        this.transaction = transaction;
    }

    public Set<Node> loadAllNodes(RelationshipType type) {
        final Map<VNode, VNode> nodes = new HashMap<>();
        for (final Relationship relationship : (Iterable<Relationship>) () -> transaction.findRelationships(type)) {
            final VNode startNode = nodes.computeIfAbsent(new VNode(relationship.getStartNode(), this), Function.identity());
            final VNode endNode = nodes.computeIfAbsent(new VNode(relationship.getEndNode(), this), Function.identity());
            final VRelationship vRelationship = new VRelationship(relationship, startNode, endNode);
            nodes.get(startNode).addOutRelationship(vRelationship);
            nodes.get(endNode).addInRelationship(vRelationship);
        }
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public void saveAllNode(Collection<Node> nodes) {
        createAllRelationships();
        final Iterable<VNode> vNodes = nodes.stream().map(VNode.class::cast)::iterator;
        for (final VNode vNode : vNodes) {
            final Node pNode = transaction.getNodeByElementId(vNode.getElementId());
            vNode.getProperties(MODIFIED).forEach(entry -> pNode.setProperty(entry.getKey(), entry.getValue().property()));
            vNode.getProperties(DELETED).map(Map.Entry::getKey).forEach(pNode::removeProperty);
            vNode.getLabels(CREATED).forEach(entry -> pNode.addLabel(entry.getKey()));
            vNode.getLabels(DELETED).forEach(entry -> pNode.removeLabel(entry.getKey()));
        }
    }
}