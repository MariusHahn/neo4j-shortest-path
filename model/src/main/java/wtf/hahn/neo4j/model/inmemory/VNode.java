package wtf.hahn.neo4j.model.inmemory;

import static wtf.hahn.neo4j.model.inmemory.Modification.CREATED;
import static wtf.hahn.neo4j.model.inmemory.Modification.DELETED;
import static wtf.hahn.neo4j.model.inmemory.Modification.MODIFIED;
import static wtf.hahn.neo4j.model.inmemory.Modification.NONE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.Iterables;

public class VNode extends VEntity implements Node {

    private final Map<Label, Modification> labels;
    private final List<Relationship> outgoing;
    private final List<Relationship> ingoing;
    private final Transaction transaction;

    public VNode(Node node, Transaction transaction) {
        super(node);
        this.transaction = transaction;
        outgoing = new ArrayList<>();
        ingoing = new ArrayList<>();
        labels = Iterables.stream(node.getLabels())
                .collect(Collectors.toMap(Function.identity(), x -> NONE));
    }

    public boolean addOutRelationship(VRelationship relationship) {
        return outgoing.add(relationship);
    }

    public boolean addInRelationship(VRelationship relationship) {
        return ingoing.add(relationship);
    }

    public Stream<Map.Entry<Label, Modification>> getLabels(Modification modification) {
        return labels.entrySet().stream().filter(entry -> entry.getValue().equals(modification));
    }

    public Stream<VRelationship> getRelationships(Modification modification) {
        return outgoing.stream().map(VRelationship.class::cast).filter(r -> r.modification().equals(modification));
    }

    @Override
    public ResourceIterable<Relationship> getRelationships() {
        return new VResourceIterable<>(relationshipsByDirection(Direction.BOTH));
    }

    @Override
    public boolean hasRelationship() {
        return !outgoing.isEmpty() || !ingoing.isEmpty();
    }

    @Override
    public ResourceIterable<Relationship> getRelationships(RelationshipType... types) {
        Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
        return new VResourceIterable<>(relationshipsByDirection(Direction.BOTH).filter(r -> typez.contains(r.getType())));
    }

    @Override
    public ResourceIterable<Relationship> getRelationships(Direction direction, RelationshipType... types) {
        Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
        return new VResourceIterable<>(relationshipsByDirection(direction).filter(r -> typez.contains(r.getType())));
    }

    @Override
    public boolean hasRelationship(RelationshipType... types) {
        Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
        return relationshipsByDirection(Direction.BOTH).map(Relationship::getType).anyMatch(typez::contains);
    }

    @Override
    public boolean hasRelationship(Direction direction, RelationshipType... types) {
        Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
        return relationshipsByDirection(direction).map(Relationship::getType).anyMatch(typez::contains);
    }

    @Override
    public ResourceIterable<Relationship> getRelationships(Direction dir) {
        return new VResourceIterable<>(relationshipsByDirection(dir));
    }

    @Override
    public boolean hasRelationship(Direction dir) {
        return relationshipsByDirection(dir).findAny().isPresent();
    }

    @Override
    public Relationship getSingleRelationship(RelationshipType type, Direction dir) {
        List<Relationship> relationship = relationshipsByDirection(dir).filter(r -> r.isType(type)).toList();
        if (relationship.size() <= 1) {
            return relationship.stream().findFirst().orElse(null);
        }
        throw new IllegalStateException(
                "There is more than one Relationship for type[%s] and direction[%s]".formatted(type, dir)
        );
    }

    @Override
    public Relationship createRelationshipTo(Node otherNode, RelationshipType type) {
        Node s = transaction.getNodeByElementId(this.getElementId());
        Node t = transaction.getNodeByElementId(otherNode.getElementId());
        Relationship relationship = s.createRelationshipTo(t, type);
        VNode other = (VNode) otherNode;
        VRelationship vRelationship = new VRelationship(relationship, this, other);
        this.addOutRelationship(vRelationship);
        other.addInRelationship(vRelationship);
        return vRelationship;
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return () -> relationshipsByDirection(Direction.BOTH).map(Relationship::getType).iterator();
    }

    @Override
    public int getDegree() {
        return outgoing.size() + ingoing.size();
    }

    @Override
    public int getDegree(RelationshipType type) {
        return (int) relationshipsByDirection(Direction.BOTH).filter(r -> r.isType(type)).count();
    }

    @Override
    public int getDegree(Direction direction) {
        return (int) relationshipsByDirection(direction).count();
    }

    @Override
    public int getDegree(RelationshipType type, Direction direction) {
        return (int) relationshipsByDirection(direction).filter(r -> r.isType(type)).count();
    }



    public Stream<Relationship> relationshipsByDirection(Direction direction) {
        return switch (direction) {
            case OUTGOING -> outgoing.stream();
            case INCOMING -> ingoing.stream();
            case BOTH -> Stream.of(outgoing, ingoing).flatMap(Collection::stream).distinct();
        };
    }

    @Override
    public void addLabel(Label label) {
        modification = MODIFIED;
        labels.put(label, CREATED);
    }

    @Override
    public void removeLabel(Label label) {
        modification = MODIFIED;
        labels.put(label, DELETED);
    }

    @Override
    public boolean hasLabel(Label label) {
        return labels.containsKey(label) && !DELETED.equals(labels.get(label));
    }

    @Override
    public Iterable<Label> getLabels() {
        return labels.entrySet().stream()
                .filter(entry -> !DELETED.equals(entry.getValue()))
                .map(Map.Entry::getKey)::iterator;
    }
}
