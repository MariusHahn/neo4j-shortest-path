package wtf.hahn.neo4j.procedure;

import java.util.Iterator;
import java.util.stream.Stream;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.model.Dijkstra;
import wtf.hahn.neo4j.model.PathResult;
import wtf.hahn.neo4j.model.ShortestPropertyPath;
import static org.neo4j.graphdb.Direction.OUTGOING;

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
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(expander, costProperty, 1);
        WeightedPath singlePath = pathFinder.findSinglePath(startNode, endNode);
        return Stream.of(new PathResult(singlePath));
    }
}
