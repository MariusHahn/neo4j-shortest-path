package wft.hahn.neo4j.cch.model;

import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.EntityHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public class VertexLoader {

    private final Transaction transaction;

    public Set<Vertex> loadAllVertices(RelationshipType type, String weightProperty) {
        final Map<Vertex, Vertex> nodes = new HashMap<>();
        for (final Relationship relationship : (Iterable<Relationship>) () -> transaction.findRelationships(type)) {
            final Vertex start = nodes.computeIfAbsent(new Vertex(relationship.getStartNode()), Function.identity());
            final Vertex end = nodes.computeIfAbsent(new Vertex(relationship.getEndNode()), Function.identity());
            long weight = EntityHelper.getLongProperty(relationship, weightProperty);
            final Arc arc = new Arc(start, end, (int) weight, null);
            start.addArc(arc);
            end.addArc(arc);
        }
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public void setRankProperty(Vertex vertex, int rank, String rankPropertyName) {
        transaction.getNodeByElementId(vertex.elementId).setProperty(rankPropertyName, rank);
    }

    public void commit(){
        transaction.commit();
    }
}
