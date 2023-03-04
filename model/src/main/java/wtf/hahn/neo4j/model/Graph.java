package wtf.hahn.neo4j.model;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.Iterables;

import static wtf.hahn.neo4j.model.Shortcut.WEIGHT_PROPERTY_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Graph {

    private final Map<String, Node> nodes;
    private final Map<Node, Set<Relationship>> adjacencyMapOutgoing = new HashMap<>();
    private final Map<Node, Set<Relationship>> adjacencyMapIngoing = new HashMap<>();

    public Graph(Iterator<Relationship> relationshipIterator) {
        List<Relationship> relationships = Iterables.stream(relationshipIterator).toList();
        nodes = relationships.stream()
                .flatMap(r -> Arrays.stream(new Node[] {r.getStartNode(), r.getEndNode()}))
                .distinct()
                .collect(Collectors.toUnmodifiableMap(Entity::getElementId, NodeInMem::new));

        for (Relationship relationship : relationships) {
            Node inMemStartNode = nodes.get(relationship.getStartNode().getElementId());
            Node inMemEndNode = nodes.get(relationship.getEndNode().getElementId());
            RelationshipInMem relationshipInMem = new RelationshipInMem(relationship);
            adjacencyMapOutgoing.computeIfAbsent(inMemStartNode, x -> new HashSet<>());
            adjacencyMapIngoing.computeIfAbsent(inMemEndNode, x -> new HashSet<>());
            adjacencyMapOutgoing.get(inMemStartNode).add(relationshipInMem);
            adjacencyMapIngoing.get(inMemEndNode).add(relationshipInMem);
        }
    }

    public RelationshipInMem createShortcut(RelationshipType type, Node startNode, Node endNode, String costPropertyName, Relationship in, Relationship out, double cost) {
        RelationshipInMem relationship = new RelationshipInMem(type, startNode.getElementId(), endNode.getElementId());
        adjacencyMapOutgoing.get(startNode).add(relationship);
        adjacencyMapIngoing.get(endNode).add(relationship);
        relationship.setProperty(Shortcut.IN_RELATION, in.getElementId());
        relationship.setProperty(Shortcut.OUT_RELATION, out.getElementId());
        relationship.setProperty(WEIGHT_PROPERTY_KEY, costPropertyName);
        relationship.setProperty(costPropertyName, cost);
        return relationship;
    }

    public void persistAddedRelationships(Transaction transaction) {
        adjacencyMapOutgoing.entrySet()
                .stream()
                .filter(nodeRelationships -> nodeRelationships.getValue().stream().anyMatch(r -> r.getElementId() == null))
                .forEach(nodeRelationships -> {
                    Node persistNode = transaction.getNodeByElementId(nodeRelationships.getKey().getElementId());
                    ((NodeInMem)nodeRelationships.getKey()).getAllModifiedProperties().forEach(persistNode::setProperty);
                    Set<Relationship> relationships = nodeRelationships.getValue();
                    relationships.forEach(r -> {
                        Relationship persistRelationship = persistNode.createRelationshipTo(transaction.getNodeByElementId(r.getEndNode().getElementId()), r.getType());
                        Map<String, Object> allModifiedProperties = ((RelationshipInMem) r).getAllModifiedProperties();
                        allModifiedProperties.forEach(persistRelationship::setProperty);
                    });
                });
    }

    public Collection<Node> nodes() {
        return nodes.values();
    }


    class RelationshipInMem extends EntityInMem implements Relationship {

        private final RelationshipType type;
        private final String startNodeElementId;
        private final String endNodeElementId;

        public RelationshipInMem(RelationshipType type, String startNodeElementId,
                                 String endNodeElementId) {
            super();
            this.type = type;
            this.startNodeElementId = startNodeElementId;
            this.endNodeElementId = endNodeElementId;
        }

        public RelationshipInMem(Relationship relationship) {
            super(relationship);
            this.type = relationship.getType();
            this.startNodeElementId = relationship.getStartNode().getElementId();
            this.endNodeElementId = relationship.getEndNode().getElementId();
        }

        @Override
        public Node getStartNode() {
            return nodes.get(startNodeElementId);
        }

        @Override
        public Node getEndNode() {
            return nodes.get(endNodeElementId);
        }

        @Override
        public Node getOtherNode(Node node) {
            return node.getElementId().equals(startNodeElementId) ? getStartNode() : getEndNode();
        }

        @Override
        public Node[] getNodes() {
            return new Node[]{getStartNode(), getEndNode()};
        }

        @Override
        public RelationshipType getType() {
            return type;
        }

        @Override
        public boolean isType(RelationshipType type) {
            return this.type.equals(type);
        }
    }

    class NodeInMem extends EntityInMem implements Node {

        private final Set<Label> labels;

        public NodeInMem(Node node) {
            super(node);
            labels = Iterables.stream(node.getLabels()).collect(Collectors.toSet());
        }

        @Override
        public ResourceIterable<Relationship> getRelationships() {
            return new ResourceIterableInMem<>(adjacencyMapOutgoing.get(this));
        }

        @Override
        public boolean hasRelationship() {
            return adjacencyMapOutgoing.containsKey(this);
        }

        @Override
        public ResourceIterable<Relationship> getRelationships(RelationshipType... types) {
            Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
            List<Relationship> relationships = getRelationships().stream()
                    .filter(relationship -> typez.contains(relationship.getType()))
                    .toList();
            return new ResourceIterableInMem<>(relationships);
        }

        @Override
        public ResourceIterable<Relationship> getRelationships(Direction direction, RelationshipType... types) {
            Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
            Set<Relationship> relationships = (Direction.OUTGOING.equals(direction)
                    ? adjacencyMapOutgoing : adjacencyMapIngoing)
                    .get(this)
                    .stream()
                    .filter(relationship -> typez.contains(relationship.getType()))
                    .collect(Collectors.toSet());
            return new ResourceIterableInMem<>(relationships);
        }

        @Override
        public boolean hasRelationship(RelationshipType... types) {
            Set<RelationshipType> typez = Arrays.stream(types).collect(Collectors.toSet());
            return adjacencyMapOutgoing.get(this).stream().anyMatch(relationship -> typez.contains(relationship.getType()));
        }

        @Override
        public boolean hasRelationship(Direction direction, RelationshipType... types) {
            return hasRelationship(types);
        }

        @Override
        public ResourceIterable<Relationship> getRelationships(Direction dir) {
            return new ResourceIterableInMem<>(adjacencyMapOutgoing.get(this));
        }

        @Override
        public boolean hasRelationship(Direction dir) {
            return dir == Direction.OUTGOING;
        }

        @Override
        public Relationship getSingleRelationship(RelationshipType type, Direction dir) {
            throw new UnsupportedOperationException("This is not supported yet");

        }

        @Override
        public Relationship createRelationshipTo(Node otherNode, RelationshipType type) {
            throw new UnsupportedOperationException("This is not supported yet");
        }

        @Override
        public Iterable<RelationshipType> getRelationshipTypes() {
            return Stream.concat(adjacencyMapOutgoing.get(this).stream()
                    , adjacencyMapIngoing.get(this).stream())
                    .map(Relationship::getType)
                    .distinct().toList();
        }

        @Override
        public int getDegree() {
            return (int) Stream.concat(adjacencyMapOutgoing.get(this).stream()
                    , adjacencyMapIngoing.get(this).stream()).distinct().count();
        }

        @Override
        public int getDegree(RelationshipType type) {
            return (int) Stream.concat(adjacencyMapOutgoing.get(this).stream()
                    , adjacencyMapIngoing.get(this).stream())
                    .distinct()
                    .filter(relationship -> relationship.isType(type))
                    .count();
        }

        @Override
        public int getDegree(Direction direction) {
            Map<Node, Set<Relationship>> nodeSetMap = direction == Direction.OUTGOING
                    ? adjacencyMapOutgoing
                    : adjacencyMapIngoing;
            return nodeSetMap.get(this).size();
        }

        @Override
        public int getDegree(RelationshipType type, Direction direction) {
            Map<Node, Set<Relationship>> nodeSetMap = direction == Direction.OUTGOING
                    ? adjacencyMapOutgoing
                    : adjacencyMapIngoing;
            return (int) nodeSetMap.get(this).stream().filter(relationship -> relationship.isType(type)).count();
        }

        @Override
        public void addLabel(Label label) {
            throw new UnsupportedOperationException("This is not supported yet");
        }

        @Override
        public void removeLabel(Label label) {
            throw new UnsupportedOperationException("This is not supported yet");
        }

        @Override
        public boolean hasLabel(Label label) {
            return labels.contains(label);
        }

        @Override
        public Iterable<Label> getLabels() {
            return labels;
        }
    }

    static class EntityInMem implements Entity {
        record ObservedProperty(boolean modified, Object property) {
            public ObservedProperty(Object property) {
                this(false, property);
            }
        }
        private final Map<String, ObservedProperty> properties;
        private final long id;
        private final String elementId;

        @Override
        public String toString() {
            return "EntityInMem{" +
                    "properties=" + properties +
                    '}';
        }

        public EntityInMem() {
            this.properties = new HashMap<>();
            this.id = -1;
            this.elementId = null;
        }

        EntityInMem(Entity entity) {
            properties = entity.getAllProperties().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new ObservedProperty(e.getValue())));
            id = entity.getId();
            elementId = entity.getElementId();
        }

        public Map<String, Object> getAllModifiedProperties() {
            return properties.entrySet().stream()
                    .filter(e -> e.getValue().modified())
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().property));
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getElementId() {
            return elementId;
        }

        @Override
        public boolean hasProperty(String key) {
            return properties.containsKey(key);
        }

        @Override
        public Object getProperty(String key) {
            return properties.get(key).property;
        }

        @Override
        public Object getProperty(String key, Object defaultValue) {
            ObservedProperty p = properties.get(key);
            return p == null ? defaultValue : p.property();
        }

        @Override
        public void setProperty(String key, Object value) {
            properties.put(key, new ObservedProperty(true, value));
        }

        @Override
        public Object removeProperty(String key) {
            throw new UnsupportedOperationException("This is not supported yet");
        }

        @Override
        public Iterable<String> getPropertyKeys() {
            return properties.keySet();
        }

        @Override
        public Map<String, Object> getProperties(String... keys) {
            HashMap<String, Object> someProperties = new HashMap<>(keys.length * 4 / 3);
            for (String key : keys) {
                if (properties.containsKey(key)) {
                    someProperties.put(key, properties.get(key));
                }
            }
            return someProperties;
        }

        @Override
        public Map<String, Object> getAllProperties() {
            return properties.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().property));
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException("This is not supported yet");
        }
    }

    private static class ResourceIterableInMem<T> implements ResourceIterable<T> {

        private final Collection<T> collection;

        public ResourceIterableInMem(Collection<T> collection) {
            this.collection = collection;
        }

        @Override
        public ResourceIterator<T> iterator() {
            Iterator<T> iterator = collection.iterator();
            return new ResourceIterator<T>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public T next() {
                    return iterator.next();
                }

                @Override
                public void close() {

                }
            };
        }

        @Override
        public void close() {

        }
    }
}
