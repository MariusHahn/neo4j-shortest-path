package wft.hahn.neo4j.cch.update;

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
    private final Map<Arc, Double> inputWeights;

    public Updater(Transaction transaction, Path path) {
        this.transaction = transaction;
        indexLoader = new IndexGraphLoader(path);
        highestVertex = indexLoader.load();
        inputWeights = new HashMap<>();
    }

    private List<Arc> scanNeo(Transaction transaction, IndexGraphLoader indexLoader) {
        final List<Arc> updates = new LinkedList<>();
        Iterable<Relationship> relationships = transaction.findRelationships(() -> "ROAD").stream()::iterator;
        for (Relationship relationship : relationships) {
            final int fromRank = (int) getLongProperty(relationship.getStartNode(), "ROAD_rank");
            final int toRank = (int) getLongProperty(relationship.getEndNode(), "ROAD_rank");
            Arc arc = indexLoader.getArc(fromRank, toRank);
            inputWeights.put(arc, getDoubleProperty(relationship, "cost"));
            if (relationship.hasProperty("changed")) {
                relationship.removeProperty("changed");
                updates.add(arc);
            }
        }
        return updates;
    }

    public Vertex update() {
        Set<Arc> seen = new HashSet<>();
        updates.addAll(scanNeo(transaction, indexLoader));
        while (!updates.isEmpty()) {
            final Arc arc = updates.poll();
            if (!seen.add(arc)) continue;
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
        return triangle.c().weight == triangle.b().weight + oldWeight
                //|| triangle.b().weight + triangle.a().weight < triangle.c().weight
                //  triangle.c().weight <= triangle.b().weight + oldWeight
                ;
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
        return inputWeights.getOrDefault(update, Double.MAX_VALUE);
    }

    private static int arcComparator(Arc o1, Arc o2) {
        int compare = Integer.compare(o1.start.rank, o2.start.rank);
        if (compare == 0) return Integer.compare(o1.end.rank, o2.end.rank);
        return compare;
    }
}
