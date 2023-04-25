package wtf.hahn.neo4j.util;

import java.util.Iterator;
import java.util.stream.Stream;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

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

    public static boolean contains(Path path, Node node) {
        for (Node pNode : path.nodes()) if (pNode.equals(node)) {return true;}
        return false;
    }

    public static WeightedPath joinSameMiddleNode(WeightedPath inwards, WeightedPath outwards) {
        assert inwards.startNode() == outwards.startNode();
        return new WeightedPath() {
            @Override
            public double weight() {
                return inwards.weight() + outwards.weight();
            }

            @Override
            public Node startNode() {
                return inwards.lastRelationship().getEndNode();
            }

            @Override
            public Node endNode() {
                return outwards.lastRelationship().getEndNode();
            }

            @Override
            public Relationship lastRelationship() {
                return outwards.lastRelationship();
            }

            @Override
            public Iterable<Relationship> relationships() {
                return new JoinIterator<>(inwards.reverseRelationships(), outwards.relationships());
            }

            @Override
            public Iterable<Relationship> reverseRelationships() {
                return new JoinIterator<>(outwards.reverseRelationships(), inwards.relationships());
            }

            @Override
            public Iterable<Node> nodes() {
                return new JoinIterator<>(inwards.reverseNodes(), outwards.nodes());
            }

            @Override
            public Iterable<Node> reverseNodes() {
                return new JoinIterator<>(outwards.reverseNodes(),inwards.nodes());
            }

            @Override
            public int length() {
                return inwards.length() + outwards.length();
            }

            @Override
            public Iterator<Entity> iterator() {
                return new ZipIterator<>(nodes(), relationships());
            }
        };
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
                return new JoinIterator<>(forward.relationships(), backward.reverseRelationships());
            }

            @Override
            public Iterable<Relationship> reverseRelationships() {
                return new JoinIterator<>(forward.reverseRelationships(), backward.relationships());
            }

            @Override
            public Iterable<Node> nodes() {
                return new JoinIterator<>(forward.nodes(), backward.reverseNodes());
            }

            @Override
            public Iterable<Node> reverseNodes() {
                return new JoinIterator<>(backward.nodes(), forward.reverseNodes());
            }

            @Override
            public int length() {
                return forward.length() + backward.length();
            }

            @Override
            public Iterator<Entity> iterator() {
                Iterable<Entity> backwardEntity = new ZipIterator<>(backward.reverseNodes(), backward.reverseRelationships());
                return new JoinIterator<>(forward, backwardEntity);
            }
        };
    }

}
