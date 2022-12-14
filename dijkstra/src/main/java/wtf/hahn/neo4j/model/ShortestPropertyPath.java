package wtf.hahn.neo4j.model;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.ToString;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.util.IterationHelper;
import wtf.hahn.neo4j.util.ReverseIterator;
import wtf.hahn.neo4j.util.ZipIterator;

@ToString
public class ShortestPropertyPath implements WeightedPath {

    public final RelationshipType relationshipType;
    public final String propertyKey;
    public final List<Relationship> relationships;

    public ShortestPropertyPath(Iterator<Relationship> relationships, RelationshipType relationshipType,
                                String propertyKey) {
        this.propertyKey = propertyKey;
        this.relationshipType = relationshipType;
        this.relationships = materializeRelationships(relationships);
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
        return materializeNodes();
    }

    @Override
    public Iterable<Node> reverseNodes() {
        return new ReverseIterator<>(materializeNodes());
    }

    @Override
    public int length() {
        return relationships.size();
    }

    @Override
    public Iterator<Entity> iterator() {
        return new ZipIterator<>(nodes(), relationships);
    }

    private List<Relationship> materializeRelationships(Iterator<Relationship> relationships) {
        return IterationHelper.stream(relationships).collect(Collectors.toList());
    }

    private List<Node> materializeNodes() {
        return Stream.concat(Stream.of(startNode()), relationships.stream().map(Relationship::getEndNode))
                .collect(Collectors.toList());
    }

    @Override
    public double weight() {
        return relationships.stream()
                .map(relationship -> relationship.getProperty(propertyKey))
                .map(cost -> (cost instanceof Long) ? String.valueOf(cost) : (String) cost)
                .mapToLong(Long::valueOf)
                .sum();
    }
}
