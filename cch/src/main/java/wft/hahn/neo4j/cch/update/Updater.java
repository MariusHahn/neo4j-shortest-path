package wft.hahn.neo4j.cch.update;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

import java.nio.file.Path;
import java.util.*;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;
import static wtf.hahn.neo4j.util.EntityHelper.getLongProperty;

public class Updater {
    private final Transaction transaction;

    private final Queue<Arc> updates = new PriorityQueue<>(Updater::arcComparator);

    private final Vertex highestVertex;
    private final IndexGraphLoader indexLoader;

    public Updater(Transaction transaction, Path path) {
        this.transaction = transaction;
        indexLoader = new IndexGraphLoader(path);
        highestVertex = indexLoader.load();
        this.updates.addAll(scanNeo(transaction, indexLoader));
    }

    private static Set<Arc> scanNeo(Transaction transaction, IndexGraphLoader indexLoader) {
        final Set<Arc> updates = new HashSet<>();
        Iterable<Relationship> relationships = transaction.findRelationships(() -> "ROAD").stream()::iterator;
        for (Relationship relationship : relationships) if (relationship.hasProperty("changed")) {
            relationship.removeProperty("changed");
            final int fromRank = (int) getLongProperty(relationship.getStartNode(), "ROAD_rank");
            final int toRank = (int) getLongProperty(relationship.getEndNode(), "ROAD_rank");
            updates.add(indexLoader.getArc(fromRank, toRank));
        }
        return updates;
    }

    public Vertex update() {
        while (!updates.isEmpty()) {
            final Arc arc = updates.poll();
            final TriangleBuilder triangleBuilder = new TriangleBuilder(arc);
            float oldWeight = arc.weight;
            updateArc(arc, triangleBuilder.lower());
            if (oldWeight != arc.weight) {
                checkTriangles(oldWeight, triangleBuilder.upper());
                checkTriangles(oldWeight, triangleBuilder.intermediate());
            }
        }
        return highestVertex;
    }

    private void checkTriangles(double oldWeight, Collection<Triangle> triangles) {
        for (final Triangle triangle : triangles) {
            if (arcWeightCouldRelyOnTriangle(triangle, oldWeight)) {
                updates.offer(indexLoader.getArc(triangle.c().start.rank, triangle.c().end.rank));
            }
        }
    }

    private static boolean arcWeightCouldRelyOnTriangle(Triangle triangle, double oldWeight) {
        return triangle.c().weight == triangle.b().weight + oldWeight;
    }

    private void updateArc(Arc arc, Collection<Triangle> lowerTriangles) {
        arc.weight = (float) inputGraphWeight(arc);
        arc.middle = null;
        arc.hopLength = 1;
        for (Triangle lowerTriangle : lowerTriangles) {
            final double lowerTriangleWeight = lowerTriangle.b().weight + lowerTriangle.c().weight;
            if (lowerTriangleWeight < arc.weight) {
                arc.weight = (float) lowerTriangleWeight;
                arc.middle = lowerTriangle.middle();
                arc.hopLength = lowerTriangle.b().hopLength + lowerTriangle.c().hopLength;
            }
        }
    }

    private double inputGraphWeight(Arc update) {
        return getRelationship(update).map(r -> getDoubleProperty(r, "cost")).orElse(Double.MAX_VALUE);
    }

    private Optional<Relationship> getRelationship(Arc update) {
        return getRelationship(update.start.rank, update.end.rank);
    }

    private Optional<Relationship> getRelationship(int fromRank, int toRank) {
        return transaction
                .findNode(() -> "Location", "ROAD_rank", fromRank)
                .getRelationships(Direction.OUTGOING, () -> "ROAD").stream()
                .filter(r -> getLongProperty(r.getEndNode(), "ROAD_rank") == toRank)
                .findFirst();
    }

    private static int arcComparator(Arc o1, Arc o2) {
        return Integer.compare(Math.min(o1.start.rank, o2.start.rank), Math.min(o1.end.rank, o2.end.rank));
    }
}
