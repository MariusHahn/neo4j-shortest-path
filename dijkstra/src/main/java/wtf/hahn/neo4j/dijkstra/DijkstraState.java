package wtf.hahn.neo4j.dijkstra;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Paths;
import wtf.hahn.neo4j.model.WeightedPathImpl;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(of = "endNode")
public class DijkstraState implements Comparable<DijkstraState>{
    private final Node endNode;
    private final WeightedPath path;
    public boolean settled;

    public DijkstraState(Node endNode) {
        this.endNode = endNode;
        path = new WeightedPathImpl(0.0, Paths.singleNodePath(endNode));
        settled = false;
    }

    public double weight() {
        return path.weight();
    }

    @Override
    public int compareTo(DijkstraState o) {
        return Double.compare(path.weight(), o.path.weight());
    }
}
