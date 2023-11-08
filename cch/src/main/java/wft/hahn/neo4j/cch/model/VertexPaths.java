package wft.hahn.neo4j.cch.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class VertexPaths {

    public static boolean contains(VertexPath path, Vertex node) {
        for (Vertex pVertex : path.vertices()) if (pVertex.equals(node)) {return true;}
        return false;
    }

    public static VertexPath singleVertexPath(Vertex vertex) {
        return new SingleVertexPath(vertex);
    }

    private record SingleVertexPath(Vertex vertex) implements VertexPath {

        @Override
        public Vertex start() {
            return vertex;
        }

        @Override
        public Vertex end() {
            return vertex;
        }

        @Override
        public Arc lastArc() {
            return null;
        }

        @Override
        public Iterable<Arc> arcs() {
            return Collections.emptyList();
        }

        @Override
        public Iterable<Arc> reverseArcs() {
            return arcs();
        }

        @Override
        public Iterable<Vertex> vertices() {
            return Arrays.asList(vertex);
        }

        @Override
        public Iterable<Vertex> reverseVertices() {
            return vertices();
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public Iterator<PathElement> iterator() {
            return Arrays.<PathElement>asList(vertex).iterator();
        }

        @Override
        public int weight() {
            return 0;
        }
    }
}
