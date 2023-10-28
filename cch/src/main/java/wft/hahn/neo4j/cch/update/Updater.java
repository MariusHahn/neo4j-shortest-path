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
    public static final String LAST_CCH_COST_WHILE_INDEXING = "last_cch_cost_while_indexing";
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
            final double newWeight = getNewWeight(arc, triangleBuilder.lower());
            if (weightHasChanged(arc, newWeight)) {
                float oldWeight = arc.weight;
                updateWeight(arc, newWeight);
                checkTriangles(oldWeight, triangleBuilder.upper());
                checkTriangles(oldWeight, triangleBuilder.intermediate());
            }
        }
        return highestVertex;
    }

    private static boolean weightHasChanged(Arc arc, double newWeight) {
        return arc.weight != newWeight;
    }

    private void updateWeight(Arc arc, double newWeight) {
        arc.weight = (float) newWeight;
        getRelationship(arc.start.rank, arc.end.rank)
                .ifPresent(r -> r.setProperty(LAST_CCH_COST_WHILE_INDEXING, (float) newWeight));
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

    private double getNewWeight(Arc arc, Collection<Triangle> lowerTriangles) {
        double newWeight = inputGraphWeight(arc);
        for (Triangle lowerTriangle : lowerTriangles) newWeight = Math.min(newWeight, lowerTriangle.weight());
        return newWeight;
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
