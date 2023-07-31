package wft.hahn.neo4j.cch.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class SearchVertexPaths {

    public static boolean contains(SearchPath path, SearchVertex node) {
        for (SearchVertex pVertex : path.vertices()) if (pVertex.equals(node)) {return true;}
        return false;
    }

    public static SearchPath singleSearchVertexPath(SearchVertex vertex) {
        assert vertex != null;
        return new SingleSearchVertexPath(vertex);
    }

    private record SingleSearchVertexPath(SearchVertex vertex) implements SearchPath {

        @Override
        public SearchVertex start() {
            return vertex;
        }

        @Override
        public SearchVertex end() {
            return vertex;
        }

        @Override
        public SearchArc lastArc() {
            return null;
        }

        @Override
        public Iterable<SearchArc> arcs() {
            return Collections.emptyList();
        }

        @Override
        public Iterable<SearchArc> reverseArcs() {
            return arcs();
        }

        @Override
        public Iterable<SearchVertex> vertices() {
            return Arrays.asList(vertex);
        }

        @Override
        public Iterable<SearchVertex> reverseVertices() {
            return vertices();
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public Iterator<SearchPathElement> iterator() {
            return Arrays.<SearchPathElement>asList(vertex).iterator();
        }

        @Override
        public float weight() {
            return 0;
        }

        @Override
        public String toString() {
            return SearchVertexPaths.toString(this);
        }
    }

    public static String toString(SearchPath path) {
        StringBuilder builder = new StringBuilder("len(%3.2f): (%d)".formatted(path.weight(), path.start().rank));
        for (SearchArc arc : path.arcs()) builder.append("-[%.2f]->(%d)".formatted(arc.weight, arc.end.rank));
        return builder.toString();
    }
}
