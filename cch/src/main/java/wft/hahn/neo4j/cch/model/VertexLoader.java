package wft.hahn.neo4j.cch.model;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.iterable.JoinIterable;

import java.util.*;
import java.util.function.Function;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

public class VertexLoader {

    private final Transaction transaction;
    private final String costProperty;
    private final RelationshipType[] types;

    public VertexLoader(Transaction transaction, String costProperty, RelationshipType... types) {
        this.transaction = transaction;
        this.costProperty = costProperty;
        this.types = types;
    }

    public Set<Vertex> loadAllVertices() {
        final Map<Vertex, Vertex> nodes = new HashMap<>();
        int counter = 0;
        for (final Relationship relationship : getRelationships(types)) {
            final Vertex start = nodes.computeIfAbsent(new Vertex(relationship.getStartNode()), Function.identity());
            final Vertex end = nodes.computeIfAbsent(new Vertex(relationship.getEndNode()), Function.identity());
            final double weight = getDoubleProperty(relationship,  costProperty);
            start.addArc(end, (float) weight);
            if (counter++ % 10000 == 0) System.out.println(counter + " arcs imported");
        }
        return Collections.unmodifiableSet(nodes.keySet());
    }

    private Iterable<Relationship> getRelationships(RelationshipType[] type) {
        Iterator<RelationshipType> iterator = Arrays.stream(type).iterator();
        RelationshipType first = iterator.next();
        Iterable<Relationship> relationships = () -> transaction.findRelationships(first);
        while (iterator.hasNext()) {
            RelationshipType next = iterator.next();
            relationships = new JoinIterable<>(relationships, () -> transaction.findRelationships(next));
        }
        return relationships;
    }

    public void setRankProperty(Vertex vertex, int rank, String rankPropertyName) {
        transaction.getNodeByElementId(vertex.elementId).setProperty(rankPropertyName, rank);
    }

    public void commit() {
        transaction.commit();
    }
}
