package wtf.hahn.neo4j.model;

import java.util.Iterator;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Paths;

public record ShortestPathResult(Path path, double weight, long rank, long searchSpaceSize) implements WeightedPath {

    public ShortestPathResult() {
        this(null, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, 0);
    }

    public ShortestPathResult(Node node) {
        this(Paths.singleNodePath(node), 0.0d, 0, 0);
    }

    @Override
    public Node startNode() {
        return path.startNode();
    }

    @Override
    public Node endNode() {
        return path.endNode();
    }

    @Override
    public Relationship lastRelationship() {
        return path.lastRelationship();
    }

    @Override
    public Iterable<Relationship> relationships() {
        return path.relationships();
    }

    @Override
    public Iterable<Relationship> reverseRelationships() {
        return path.reverseRelationships();
    }

    @Override
    public Iterable<Node> nodes() {
        return path.nodes();
    }

    @Override
    public Iterable<Node> reverseNodes() {
        return path.reverseNodes();
    }

    @Override
    public int length() {
        return path.length();
    }

    @Override
    public Iterator<Entity> iterator() {
        return path.iterator();
    }
}
