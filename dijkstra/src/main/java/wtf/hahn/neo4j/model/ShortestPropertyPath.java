package wtf.hahn.neo4j.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.ToString;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.util.ReverseIterator;
import wtf.hahn.neo4j.util.ZipIterator;

@ToString
public class ShortestPropertyPath implements Path {


    public final RelationshipType relationshipType;
    public final String propertyKey;
    public final int length;
    public final List<Node> nodes;
    public final List<Relationship> relationships;

    public ShortestPropertyPath(Iterator<Relationship> relationships, RelationshipType relationshipType,
                                String propertyKey) {
        this.propertyKey = propertyKey;
        this.relationshipType = relationshipType;
        this.relationships = materializeRelationships(relationships);
        length = materializeLength(propertyKey);
        nodes = materializeNodes();
    }

    @Override
    public Node startNode() {
        return relationships.stream().findFirst().map(Relationship::getStartNode).orElseThrow();
    }

    @Override
    public Node endNode() {
        return relationships.get(relationships.size() - 1).getEndNode();
    }

    @Override
    public Relationship lastRelationship() {
        return relationships.get(relationships.size()-1);
    }

    @Override
    public Iterable<Relationship> relationships() {
        return relationships;
    }

    @Override
    public Iterable<Relationship> reverseRelationships() {
        return new ReverseIterator<>(relationships);
    }

    @Override
    public Iterable<Node> nodes() {
        return nodes;
    }

    @Override
    public Iterable<Node> reverseNodes() {
        return new ReverseIterator<>(nodes);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public Iterator<Entity> iterator() {
        return new ZipIterator<>(nodes, relationships);
    }

    private List<Relationship> materializeRelationships(Iterator<Relationship> relationships) {
        final List<Relationship> relationshipList = new ArrayList<>();
        while (relationships.hasNext()) {
            Relationship relationship = relationships.next();
            relationshipList.add(relationship);
        }
        return relationshipList;
    }

    private List<Node> materializeNodes() {
        if (relationships.isEmpty()) return List.of();
        final List<Node> nodes = new ArrayList<>();
        nodes.add(relationships.get(0).getStartNode());
        relationships.stream().map(Relationship::getEndNode).forEach(nodes::add);
        return nodes;
    }

    private int materializeLength(String propertyKey) {
        return (int) this.relationships.stream()
                .map(relationship -> relationship.getProperty(propertyKey))
                .map(cost -> (cost instanceof Long) ? String.valueOf(cost) : (String) cost)
                .mapToLong(Long::valueOf)
                .sum();
    }
}
