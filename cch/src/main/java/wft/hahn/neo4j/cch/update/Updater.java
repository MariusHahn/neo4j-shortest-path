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
    private final Queue<Update> updates = new PriorityQueue<>();
    private final Vertex highestVertex;
    private final IndexGraphLoader indexLoader;

    public Updater(Transaction transaction, Path path) {
        this.transaction = transaction;
        indexLoader = new IndexGraphLoader(path);
        highestVertex = indexLoader.load();
        this.updates.addAll(scanNeo(transaction));
    }

    private static Set<Update> scanNeo(Transaction transaction) {
        final Set<Update> updates = new HashSet<>();
        transaction.findRelationships(() -> "ROAD").forEachRemaining(r -> {
            final double cost = getDoubleProperty(r, "cost");
            final double indexCost = getDoubleProperty(r, LAST_CCH_COST_WHILE_INDEXING);
            if (cost != indexCost) {
                final int fromRank = (int) getLongProperty(r.getStartNode(), "ROAD_rank");
                final int toRank = (int) getLongProperty(r.getEndNode(), "ROAD_rank");
                updates.add(new Update(fromRank, toRank, cost, indexCost));
            }
        });
        return updates;
    }

    public Vertex update() {
        while (!updates.isEmpty()) {
            final Update update = updates.poll();
            final Arc arc = indexLoader.getArc(update.fromRank(), update.toRank());
            final TriangleBuilder triangleBuilder = new TriangleBuilder(arc);
            final double newWeight = getNewWeight(update, triangleBuilder.lower());
            if (weightHasChanged(update, newWeight)) {
                updateWeight(arc, newWeight);
                checkTriangles(update, newWeight, triangleBuilder.upper());
                checkTriangles(update, newWeight, triangleBuilder.intermediate());
            }
        }
        return highestVertex;
    }

    private static boolean weightHasChanged(Update update, double newWeight) {
        return newWeight != update.oldIndexWeight();
    }

    private void updateWeight(Arc arc, double newWeight) {
        arc.weight = (float) newWeight;
        getRelationship(arc.start.rank, arc.end.rank)
                .ifPresent(r -> r.setProperty(LAST_CCH_COST_WHILE_INDEXING, (float) newWeight));
    }

    private void checkTriangles(Update update, double newWeight, Collection<Triangle> triangles) {
        for (final Triangle triangle : triangles) if (arcWeightCouldRelyOnTriangle(triangle, update)) {
                updates.add(new Update(triangle, triangle.b().weight + newWeight));
        }
    }

    private static boolean arcWeightCouldRelyOnTriangle(Triangle triangle, Update update) {
        return triangle.c().weight == triangle.b().weight + update.oldIndexWeight();
    }

    private double getNewWeight(Update update, Collection<Triangle> lowerTriangles) {
        double newWeight = Math.min(update.newWeightCandidate(), inputGraphWeight(update));
        for (Triangle lowerTriangle : lowerTriangles) newWeight = Math.min(newWeight, lowerTriangle.weight());
        return newWeight;
    }

    private double inputGraphWeight(Update update) {
        return getRelationship(update).map(r -> getDoubleProperty(r, "cost")).orElse(Double.MAX_VALUE);
    }

    private Optional<Relationship> getRelationship(Update update) {
        return getRelationship(update.fromRank(), update.toRank());
    }

    private Optional<Relationship> getRelationship(int fromRank, int toRank) {
        return transaction
                .findNode(() -> "Location", "ROAD_rank", fromRank)
                .getRelationships(Direction.OUTGOING, () -> "ROAD").stream()
                .filter(r -> getLongProperty(r.getEndNode(), "ROAD_rank") == toRank)
                .findFirst();
    }
}
