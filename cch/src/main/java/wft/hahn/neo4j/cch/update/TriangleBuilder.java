package wft.hahn.neo4j.cch.update;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


public class TriangleBuilder {
    private final Arc arc;
    private final Set<Vertex> neighbors;
    private final Vertex x, y;

    public TriangleBuilder(Arc arc) {
        this.arc = arc;
        neighbors = intersect(arc.start.outNeighbors(), arc.end.inNeighbors());
        x = upwards() ? arc.start : arc.end;
        y = upwards() ? arc.end : arc.start;
    }

    public Collection<Triangle> lower() {
        final Collection<Triangle> triangles = new LinkedList<>();
        for (final Vertex z : neighbors) if (lower(x, y, z)) {
            if (upwards()) {
                triangles.add(new Triangle(x.getArcTo(y), x.getArcTo(z), z.getArcTo(y), z));
            } else {
                triangles.add(new Triangle(y.getArcTo(x), z.getArcTo(x), y.getArcTo(z), z));
            }
        }
        return triangles;
    }

    public Collection<Triangle> intermediate() {
        final Collection<Triangle> triangles = new LinkedList<>();
        for (final Vertex z : neighbors) if (intermediate(x, y, z)) {
            if (upwards()) {
                triangles.add(new Triangle(x.getArcTo(y), z.getArcTo(x), z.getArcTo(y), z));
            } else {
                triangles.add(new Triangle(y.getArcTo(x), x.getArcTo(z), y.getArcTo(z), z));
            }
        }
        return triangles;
    }

    public Collection<Triangle> upper() {
        final Collection<Triangle> triangles = new LinkedList<>();
        for (final Vertex z : neighbors) if (upper(x, y, z)) {
            if (upwards()) {
                triangles.add(new Triangle(x.getArcTo(y), z.getArcTo(x), z.getArcTo(y), z));
            } else {
                triangles.add(new Triangle(y.getArcTo(x), x.getArcTo(z), y.getArcTo(z), z));
            }
        }
        return triangles;
    }

    private boolean upwards() {
        return arc.start.smallerThan(arc.end);
    }

    private static boolean lower(Vertex x, Vertex y, Vertex z) {
        return z.smallerThan(x) && z.smallerThan(y);
    }

    private static boolean intermediate(Vertex x, Vertex y, Vertex z) {
        return x.smallerThan(z) && z.smallerThan(y);
    }

    private static boolean upper(Vertex x, Vertex y, Vertex z) {
        return x.smallerThan(z) && y.smallerThan(z);
    }

    private static <T> Set<T> intersect(Set<T> one, Set<T> other) {
        final Set<T> intersection = new HashSet<>(one);
        intersection.retainAll(other);
        return intersection;
    }
}
