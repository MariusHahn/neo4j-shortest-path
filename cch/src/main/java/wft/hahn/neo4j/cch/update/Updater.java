package wft.hahn.neo4j.cch.update;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;
import static wtf.hahn.neo4j.util.EntityHelper.getLongProperty;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class Updater {
    private final Transaction transaction;
    private final Queue<ArcUpdate> arcs = new PriorityQueue<>();

    public Updater(Transaction transaction, Set<ArcUpdate> arcs) {
        this.transaction = transaction;
        this.arcs.addAll(arcs);
        transaction.findRelationships(w)
    }

    void update() {
        while (!arcs.isEmpty()) {
            final ArcUpdate arc = arcs.poll();
            final TriangleBuilder triangleBuilder = new TriangleBuilder(arc.start, arc.end);
            final double oldWeight = arc.oldWeight;
            final Collection<Triangle> lowerTriangles = triangleBuilder.lower();
            final double newWeight = getNewWeight(arc, lowerTriangles);
            final InputGraphChecker inputGraphChecker = new InputGraphChecker(transaction, arc);
            if (inputGraphChecker.isArcInInputGraph()) {
                if (newWeight <= inputGraphChecker.weight) continue;
                for (final Triangle upperTriangle : triangleBuilder.upper()) {
                    if (upperTriangle.second().weight == upperTriangle.first().weight + oldWeight) {
                        upperTriangle.second().weight = (float) (upperTriangle.first().weight + newWeight);
                        arcs.add(new ArcUpdate(upperTriangle.second()));
                    }
                }
                for (final Triangle intermediateTriangle : triangleBuilder.intermediate()) {
                    if (intermediateTriangle.second().weight == intermediateTriangle.first().weight + oldWeight) {
                        intermediateTriangle.second().weight = (float) (intermediateTriangle.first().weight + newWeight);
                        arcs.add(new ArcUpdate(intermediateTriangle.second()));
                    }
                }
            }

        }
    }

    private static double getNewWeight(ArcUpdate arc, Collection<Triangle> lowerTriangles) {
        double newWeight = arc.weight;
        for (Triangle lowerTriangle : lowerTriangles) newWeight = Math.min(newWeight, lowerTriangle.weight());
        return newWeight;
    }

    private static class InputGraphChecker {
        private final Relationship relationship;
        private final Double weight;

        InputGraphChecker(Transaction transaction, ArcUpdate arc) {
            Node startNode = transaction.findNode(() -> "ROAD", "ROAD_rank", arc.start);
            relationship = startNode.getRelationships(Direction.OUTGOING, () -> "ROAD").stream()
                    .filter(r -> getLongProperty(r, "ROAD_rank") == arc.end.rank).findFirst()
                    .orElse(null);
            weight = relationship == null ? null : getDoubleProperty(relationship, "cost");
        }

        public boolean isArcInInputGraph() {
            return relationship != null;
        }

        public double getInputGraphWeight() {
            return weight;
        }
    }

    record ArcUpdate(Vertex start, Vertex end, double weight, double oldWeight) implements Comparable<ArcUpdate> {

        public ArcUpdate(Arc arc) {
            this(arc.start, arc.end, arc.weight);
        }

        @Override
        public int compareTo(ArcUpdate o) {
            return Integer.compare(start.rank, o.start.rank);
        }
    }
}
