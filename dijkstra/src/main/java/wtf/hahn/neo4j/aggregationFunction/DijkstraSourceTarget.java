package wtf.hahn.neo4j.aggregationFunction;

import java.util.stream.Stream;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.model.DijkstraHeap;
import wtf.hahn.neo4j.model.ShortestPropertyPath;
import wtf.hahn.neo4j.util.ReverseIterator;

public class DijkstraSourceTarget {

    @Context
    public GraphDatabaseService graphDatabaseService;

    @SuppressWarnings("unused")
    @Procedure
    public Stream<PathResult> sourceTarget(
            @Name("startNode") Node startNode,
            @Name("endNode") Node endNode,
            @Name("type") String type,
            @Name("propertyKey") String propertyKey) {
        final RelationshipType relationshipType = RelationshipType.withName(type);
        final DijkstraHeap heap = new DijkstraHeap(startNode);
        try (final Transaction transaction = graphDatabaseService.beginTx()) {
            while (heap.getClosestNotSettled() != null && !heap.isSettled(endNode)) {
                final Node toSettle = heap.getClosestNotSettled();
                for (Relationship relationship : toSettle.getRelationships(Direction.OUTGOING, relationshipType)) {
                    heap.setNodeDistance(propertyKey, toSettle, relationship);
                }
                heap.setSettled(toSettle);
            }
            ReverseIterator<Relationship> relationships = new ReverseIterator<>(heap.getPath(endNode));
            ShortestPropertyPath path = new ShortestPropertyPath(relationships, relationshipType, propertyKey);
            return Stream.of(new PathResult(path));
        }
    }


    public static class PathResult {
        public Double pathCost;
        public Path path;

        public PathResult(WeightedPath path) {
            this.path = path;
            pathCost = path.weight();
        }

    }
}
