package wft.hahn.neo4j.cch.model;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.iterable.JoinIterable;

@RequiredArgsConstructor
public class VertexLoader {

    private final Transaction transaction;

    public Set<Vertex> loadAllVertices(String weightProperty, RelationshipType... type) {
        final Map<Vertex, Vertex> nodes = new HashMap<>();
        for (final Relationship relationship : getRelationships(type)) {
            final Vertex start = nodes.computeIfAbsent(new Vertex(relationship.getStartNode()), Function.identity());
            final Vertex end = nodes.computeIfAbsent(new Vertex(relationship.getEndNode()), Function.identity());
            final double weight = getDoubleProperty(relationship, weightProperty);
            final Arc arc = new Arc(start, end, (float) weight);
            start.addArc(arc);
            end.addArc(arc);
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

    public void commit(){
        transaction.commit();
    }
}
