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
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

public class Updater {
    private final Transaction transaction;
    private final Queue<ArcUpdate> arcs = new PriorityQueue<>();
    private final Vertex highestVertex;
    private final IndexGraphLoader indexGraphLoader;

    public Updater(Transaction transaction, Path path) {
        this.transaction = transaction;
        indexGraphLoader = new IndexGraphLoader(path);
        highestVertex = indexGraphLoader.load();
        this.arcs.addAll(scanNeo(transaction, indexGraphLoader));
    }

    private static Set<ArcUpdate> scanNeo(Transaction transaction, IndexGraphLoader loader) {
        final Set<ArcUpdate> arcUpdates = new HashSet<>();
        transaction.findRelationships(() -> "ROAD").forEachRemaining(r -> {
            final double cost = getDoubleProperty(r, "cost");
            final double indexCost = getDoubleProperty(r, "last_cch_cost_while_indexing");
            if (cost != indexCost) {
                final int fromRank = (int) getLongProperty(r.getStartNode(), "ROAD_rank");
                final int toRank = (int) getLongProperty(r.getEndNode(), "ROAD_rank");
                Arc arc = loader.getArc(fromRank, toRank);
                arcUpdates.add(new ArcUpdate(arc, cost, arc.weight));
            }
        });
        return arcUpdates;
    }

    public Vertex update() {
        while (!arcs.isEmpty()) {
            final ArcUpdate arcUpdate = arcs.poll();
            final TriangleBuilder triangleBuilder = new TriangleBuilder(arcUpdate.start(), arcUpdate.end());
            final double newWeight = Math.min(inputGraphWeight(arcUpdate), getNewWeight(arcUpdate, triangleBuilder.lower()));
            if (newWeight == arcUpdate.oldWeight()) continue;
            arcUpdate.arc().weight = (float) newWeight;
            Collection<Triangle> upper =  triangleBuilder.upper();
            updateTriangles(arcUpdate, newWeight, upper);
            updateTriangles(arcUpdate, newWeight, triangleBuilder.intermediate());
        }
        return highestVertex;
    }

    private void updateTriangles(ArcUpdate arcUpdate, double newWeight, Collection<Triangle> triangles) {
        for (final Triangle triangle : triangles) {
            if (triangle.second().weight == triangle.first().weight + arcUpdate.oldWeight()) {
                final float oldWeight = triangle.second().weight;
                triangle.second().weight = (float) (triangle.first().weight + newWeight);
                arcs.add(new ArcUpdate(triangle.second(), oldWeight));
            }
        }
    }

    private static double getNewWeight(ArcUpdate arc, Collection<Triangle> lowerTriangles) {
        double newWeight = arc.weight();
        for (Triangle lowerTriangle : lowerTriangles) newWeight = Math.min(newWeight, lowerTriangle.weight());
        return newWeight;
    }

    private double inputGraphWeight(ArcUpdate arc) {
        final Node startNode = transaction.findNode(() -> "Location", "ROAD_rank", arc.start().rank);
        return startNode.getRelationships(Direction.OUTGOING, () -> "ROAD").stream()
                .filter(r -> getLongProperty(r.getEndNode(), "ROAD_rank") == arc.end().rank)
                .mapToDouble(r -> getDoubleProperty(r, "cost"))
                .findFirst()
                .orElse(Double.MAX_VALUE);
    }

    record ArcUpdate(Arc arc, double weight, double oldWeight) implements Comparable<ArcUpdate> {
        public ArcUpdate(Arc arc, double oldWeight) {
            this(arc , arc.weight, oldWeight);
        }

        public Vertex start() {
            return arc.start;
        }

        public Vertex end() {
            return arc.end;
        }

        boolean upwards() {
            return start().rank < end().rank;
        }

        @Override
        public int compareTo(ArcUpdate o) {
            int compare = Integer.compare(start().rank, o.start().rank);
            return upwards() ? compare : compare * -1;
        }
    }
}
