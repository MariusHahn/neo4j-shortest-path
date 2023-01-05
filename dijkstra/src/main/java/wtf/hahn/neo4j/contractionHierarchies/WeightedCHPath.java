package wtf.hahn.neo4j.contractionHierarchies;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterables;
import wtf.hahn.neo4j.util.ReverseIterator;
import wtf.hahn.neo4j.util.ZipIterator;

public final class WeightedCHPath implements WeightedPath {
    private final WeightedPath path;
    private final Transaction transaction;
    private final List<Relationship> relationships;
    private final List<Node> nodes;

    public WeightedCHPath(WeightedPath path, Transaction transaction) {
        this.path = path;
        this.transaction = transaction;
        relationships = Iterables.stream(this.path.relationships())
                .map(relationship -> Shortcut.resolveRelationships(relationship, this.transaction))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        nodes = Stream.concat(Stream.of(startNode()), relationships.stream().map(Relationship::getEndNode)).toList();
    }

    @Override
    public Node startNode() {
        return path.startNode();
    }

    @Override
    public Node endNode() {
        return path.endNode();
    }

    @Override
    public Relationship lastRelationship() {
        return relationships.get(relationships.size() - 1);
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
        return relationships.size();
    }

    @Override
    public Iterator<Entity> iterator() {
        return new ZipIterator<>(nodes(), relationships());
    }

    @Override
    public double weight() {
        return path.weight();
    }
}
