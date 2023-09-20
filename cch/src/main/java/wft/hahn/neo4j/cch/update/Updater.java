package wft.hahn.neo4j.cch.update;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;
import static wtf.hahn.neo4j.util.EntityHelper.getLongProperty;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Vertex;

public class Updater {
    private final Transaction transaction;
    private final Queue<Update> arcs = new PriorityQueue<>();
    private final Vertex highestVertex;
    private final IndexGraphLoader indexLoader;

    public Updater(Transaction transaction, Path path) {
        this.transaction = transaction;
        indexLoader = new IndexGraphLoader(path);
        highestVertex = indexLoader.load();
        this.arcs.addAll(scanNeo(transaction, indexLoader));
    }

    private static Set<Update> scanNeo(Transaction transaction, IndexGraphLoader loader) {
        final Set<Update> updates = new HashSet<>();
        transaction.findRelationships(() -> "ROAD").forEachRemaining(r -> {
            final double cost = getDoubleProperty(r, "cost");
            final double indexCost = getDoubleProperty(r, "last_cch_cost_while_indexing");
            if (cost != indexCost) {
                final int fromRank = (int) getLongProperty(r.getStartNode(), "ROAD_rank");
                final int toRank = (int) getLongProperty(r.getEndNode(), "ROAD_rank");
                updates.add(new Update(fromRank, toRank, cost, indexCost));
            }
        });
        return updates;
    }

    public Vertex update() {
        while (!arcs.isEmpty()) {
            final Update update = arcs.poll();
            final TriangleBuilder triangleBuilder = new TriangleBuilder(indexLoader.getArc(update.fromRank, update.toRank));
            final double newWeight = getNewWeight(update, triangleBuilder.lower());
            if (newWeight == update.oldIndexWeight()) continue;
            indexLoader.getArc(update.fromRank, update.toRank).weight = (float) newWeight;
            updateTriangles(update, newWeight, triangleBuilder.upper());
            updateTriangles(update, newWeight, triangleBuilder.intermediate());
        }
        return highestVertex;
    }

    private void updateTriangles(Update update, double newWeight, Collection<Triangle> triangles) {
        for (final Triangle triangle : triangles) {
            if (triangle.c.weight == triangle.b.weight + update.oldIndexWeight()) {
                final float oldIndexWeight = triangle.c.weight;
                //triangle.c.weight = (float) (triangle.b.weight + newWeight);
                arcs.add(new Update(triangle.c.start.rank, triangle.c.end.rank, triangle.b.weight + newWeight, oldIndexWeight));
            }
        }
    }

    private  double getNewWeight(Update update, Collection<Triangle> lowerTriangles) {
        double newWeight = Math.min(update.newWeightCandidate(), inputGraphWeight(update));
        for (Triangle lowerTriangle : lowerTriangles) newWeight = Math.min(newWeight, lowerTriangle.weight());
        return newWeight;
    }

    private double inputGraphWeight(Update update) {
        final Node startNode = transaction.findNode(() -> "Location", "ROAD_rank", update.fromRank);
        return startNode.getRelationships(Direction.OUTGOING, () -> "ROAD").stream()
                .filter(r -> getLongProperty(r.getEndNode(), "ROAD_rank") == update.toRank)
                .mapToDouble(r -> getDoubleProperty(r, "cost"))
                .findFirst()
                .orElse(Double.MAX_VALUE);
    }

    record Update(int fromRank, int toRank, double newWeightCandidate, double oldIndexWeight)
            implements Comparable<Update> {

        @Override
        public int compareTo(Update o) {
            int compare = Integer.compare(fromRank, toRank);
            return (toRank < fromRank) ? compare : compare * -1;
        }
    }
}
