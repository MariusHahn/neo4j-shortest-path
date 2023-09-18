package wft.hahn.neo4j.cch.update;

import java.util.Collection;
import java.util.Comparator;
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


public record TriangleBuilder(Vertex start, Vertex end, Comparator<Vertex> c, Set<Vertex> neighbors) {

    public TriangleBuilder(Vertex start, Vertex end) {
        this(
                start
                , end
                , (start.rank < end.rank) ? Vertex::compareTo : ((Comparator<Vertex>) Vertex::compareTo).reversed()
                , intersect(start.outNeighbors(), end.inNeighbors())
        );
    }

    public Collection<Triangle> lower() {
        final Collection<Triangle> triangles = new LinkedList<>();
        for (final Vertex neighbor : neighbors) if (c.compare(neighbor, start) < 0) {
                triangles.add(new Triangle(start.getArcTo(neighbor), neighbor.getArcTo(end)));
            }
        return triangles;
    }

    public Collection<Triangle> intermediate() {
        final Collection<Triangle> triangles = new LinkedList<>();
        for (final Vertex neighbor : neighbors) if (c.compare(start, neighbor) < 0 && c.compare(neighbor, end) < 0) {
                triangles.add(new Triangle(start.getArcTo(neighbor), neighbor.getArcTo(end)));
            }
        return triangles;
    }

    public Collection<Triangle> upper() {
        final Collection<Triangle> triangles = new LinkedList<>();
        for (final Vertex neighbor : neighbors) {
            if (end.compareTo(neighbor) < 0) {
                Arc first = start.compareTo(end) < 0 ? start.getArcTo(neighbor) : neighbor.getArcTo(end);
                Arc second = start.compareTo(end) < 0 ? neighbor.getArcTo(end) : start.getArcTo(neighbor);
                triangles.add(new Triangle(first, second));
            }
        }
        return triangles;
    }

    private static <T> Set<T> intersect(Set<T> one, Set<T> other) {
        final Set<T> intersection = new HashSet<>(one);
        intersection.retainAll(other);
        return intersection;
    }
}
