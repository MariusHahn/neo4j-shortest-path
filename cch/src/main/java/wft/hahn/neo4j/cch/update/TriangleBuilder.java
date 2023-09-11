package wft.hahn.neo4j.cch.update;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

/**
 *       (z)
 *      ↗ ↑
 *    /   |
 * (y)   /
 *   ↖  /
 *    \/
 *    (x)
 * The triple {x, y, z} is a lower triangle of the arc (y, z), an intermediate triangle of
 * the arc (x, z), and an upper triangle of the arc (x, y).
 * */


public record TriangleBuilder(Vertex start, Vertex end) {

    public Collection<Triangle> lower() {
        final Vertex x = start, y = end;
        if (x.rank < y.rank) return Collections.emptyList();
        final Collection<Triangle> triangles = new LinkedList<>();
        final Set<Vertex> zs = intersect(x.inNeighbors(), y.inNeighbors());
        for (final Vertex z : zs) if (z.rank < x.rank) {
            triangles.add(new Triangle(x.getArcTo(z), z.getArcTo(y)));
        }
        return triangles;
    }

    public Collection<Triangle> intermediate() {
        final Vertex x = start, y = end;
        if (x.rank < y.rank) return Collections.emptyList();
        final Collection<Triangle> triangles = new LinkedList<>();
        final Set<Vertex> zs = intersect(x.outNeighbors(), y.inNeighbors());
        for (final Vertex z : zs) if (x.rank < z.rank && z.rank < y.rank) {
            triangles.add(new Triangle(x.getArcTo(z), z.getArcTo(y)));
        }
        return triangles;
    }

    public Collection<Triangle> upper() {
        final Vertex x = start, y = end;
        if (y.rank < x.rank) return Collections.emptyList();
        final Collection<Triangle> triangles = new LinkedList<>();
        final Set<Vertex> zs = intersect(x.outNeighbors(), y.outNeighbors());
        for (final Vertex z : zs) if (y.rank < z.rank) {
            triangles.add(new Triangle(x.getArcTo(z), y.getArcTo(z)));
        }
        return triangles;
    }

    private static <T> Set<T> intersect(Set<T> one, Set<T> other) {
        final Set<T> intersection = new HashSet<>(one);
        intersection.retainAll(other);
        return intersection;
    }
}
