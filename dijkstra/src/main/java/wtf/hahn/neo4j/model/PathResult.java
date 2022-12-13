package wtf.hahn.neo4j.model;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Path;

public class PathResult {
    public Double pathCost;
    public Path path;

    public PathResult(WeightedPath path) {
        this.path = path;
        pathCost = path.weight();
    }
}
