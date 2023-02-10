package wtf.hahn.neo4j.model;

import lombok.AllArgsConstructor;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Path;
@AllArgsConstructor
public class PathResult {
    public Double pathCost;
    public Path path;


    public static PathResult noPath() {
        return new PathResult(Double.MAX_VALUE, null);
    }

    public PathResult(WeightedPath path) {
        this.path = path;
        pathCost = path.weight();
    }
}
