package wtf.hahn.neo4j.util;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;
import java.util.stream.Stream;

public class PathUtils {


    public static boolean samePath(WeightedPath shortestPath, WeightedPath includePath) {
        if (shortestPath.weight() != includePath.weight()) {
            return false;
        }
        return samePath(shortestPath, (Path) includePath);
    }

    public static boolean samePath(Path shortestPath, Path includePath) {
        if (shortestPath.length() != includePath.length()) {
            return false;
        }
        Iterator<Relationship> shortestPathIterator = shortestPath.relationships().iterator();
        Iterator<Relationship> includePathIterator = includePath.relationships().iterator();
        while (shortestPathIterator.hasNext() && includePathIterator.hasNext()) {
            if (!shortestPathIterator.next().equals(includePathIterator.next())) {
                return false;
            }
        }
        return true;
    }

    public static Path bidirectional(Path forward, Path backward) {
        return new Path() {
            @Override
            public Node startNode() {
                return forward.startNode();
            }

            @Override
            public Node endNode() {
                return backward.startNode();
            }

            @Override
            public Relationship lastRelationship() {
                return backward.relationships().iterator().next();
            }

            @Override
            public Iterable<Relationship> relationships() {
                return Stream.concat(
                        Iterables.stream(forward.relationships())
                        , Iterables.stream(backward.reverseRelationships())
                )::iterator;
            }

            @Override
            public Iterable<Relationship> reverseRelationships() {
                return Stream.concat(
                        Iterables.stream(forward.reverseRelationships())
                        , Iterables.stream(backward.relationships())
                )::iterator;
            }

            @Override
            public Iterable<Node> nodes() {
                return Stream.concat(Iterables.stream(forward.nodes()), Iterables.stream(backward.reverseNodes()))::iterator;
            }

            @Override
            public Iterable<Node> reverseNodes() {
                return Stream.concat(Iterables.stream(backward.nodes()), Iterables.stream(forward.reverseNodes()))::iterator;
            }

            @Override
            public int length() {
                return forward.length() + backward.length();
            }

            @Override
            public Iterator<Entity> iterator() {
                Stream<Entity> forwardEntity = Iterables.stream(forward.iterator());
                Stream<Entity> backwardEntity = Iterables.stream(
                        (Iterable<Entity>) new ZipIterator<>(backward.reverseNodes(), backward.reverseRelationships()));
                return Stream.concat(forwardEntity, backwardEntity).iterator();
            }
        };
    }

}
