package wtf.hahn.neo4j.model;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;

public record WeightedPathImpl(CostEvaluator<Double> costEvaluator, Path path) implements WeightedPath, Comparable<WeightedPath> {


    @Override
    public double weight() {
        double cost = 0;
        for (Relationship relationship : path.relationships()) {
            cost += costEvaluator.getCost(relationship, Direction.OUTGOING);
        }
        return cost;
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

    @Override
    public int compareTo(WeightedPath o) {
        return Double.compare(weight(), o.weight());
    }
}
