package wtf.hahn.neo4j.procedure;

import static org.neo4j.graphdb.Direction.OUTGOING;

import java.util.Iterator;
import java.util.stream.Stream;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.dijkstra.Neo4jDijkstra;
import wtf.hahn.neo4j.model.PathResult;
import wtf.hahn.neo4j.model.ShortestPropertyPath;

public class DijkstraSourceTarget {

    @SuppressWarnings("unused")
    @Procedure
    public Stream<PathResult> sourceTarget(
            @Name("startNode") Node startNode,
            @Name("endNode") Node endNode,
            @Name("type") String type,
            @Name("costProperty") String costProperty) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        final Dijkstra dijkstra = new Dijkstra(startNode, endNode, costProperty, relationshipType);
        dijkstra.calcSourceTarget();
        Iterator<Relationship> relationships = dijkstra.getRelationships();
        ShortestPropertyPath path = new ShortestPropertyPath(relationships, relationshipType, costProperty);
        return Stream.of(new PathResult(path));
    }

    @SuppressWarnings("unused")
    @Procedure
    public Stream<PathResult> sourceTargetNative(
            @Name("startNode") Node startNode,
            @Name("endNode") Node endNode,
            @Name("type") String type,
            @Name("costProperty") String costProperty) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        PathExpander<Double> expander = PathExpanders.forTypeAndDirection(relationshipType, OUTGOING);
        return Stream.of(new PathResult(new Neo4jDijkstra().shortestPath(startNode, endNode, expander, costProperty)));
    }
}
