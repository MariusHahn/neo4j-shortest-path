package wtf.hahn.neo4j.aggregationFunction;

import java.util.Iterator;
import java.util.stream.Stream;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.model.Dijkstra;
import wtf.hahn.neo4j.model.PathResult;
import wtf.hahn.neo4j.model.ShortestPropertyPath;

public class DijkstraSourceTarget {

    @SuppressWarnings("unused")
    @Procedure
    public Stream<PathResult> sourceTarget(
            @Name("startNode") Node startNode,
            @Name("endNode") Node endNode,
            @Name("type") String type,
            @Name("propertyKey") String propertyKey) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        final Dijkstra heap = new Dijkstra(startNode, endNode, propertyKey, relationshipType);
        heap.calcSourceTarget();
        Iterator<Relationship> relationships = heap.getRelationships();
        ShortestPropertyPath path = new ShortestPropertyPath(relationships, relationshipType, propertyKey);
        return Stream.of(new PathResult(path));
    }
}
