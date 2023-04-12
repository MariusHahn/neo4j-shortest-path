package wtf.hahn.neo4j.dijkstra;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Paths;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(of = "endNode")
public class DijkstraState implements Comparable<DijkstraState>{
    private final Node endNode;
    private final Path path;
    private final double cost;
    public boolean settled;

    public DijkstraState(Node endNode) {
        this.endNode = endNode;
        path = Paths.singleNodePath(endNode);
        cost = 0.0;
        settled = false;
    }

    @Override
    public int compareTo(DijkstraState o) {

        return Double.compare(cost, o.cost);
    }
}
