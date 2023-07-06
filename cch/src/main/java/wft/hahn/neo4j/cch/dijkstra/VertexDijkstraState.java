package wft.hahn.neo4j.cch.dijkstra;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.traversal.Paths;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexPath;
import wft.hahn.neo4j.cch.model.VertexPaths;
import wtf.hahn.neo4j.model.WeightedPathImpl;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(of = "endVertex")
public class VertexDijkstraState implements Comparable<VertexDijkstraState>{
    private final Vertex endVertex;
    private final VertexPath path;
    public boolean settled;

    public VertexDijkstraState(Vertex endVertex) {
        this.endVertex = endVertex;
        path = VertexPaths.singleVertexPath(endVertex);
        settled = false;
    }

    public double weight() {
        return path.weight();
    }

    @Override
    public int compareTo(VertexDijkstraState o) {
        return Double.compare(path.weight(), o.path.weight());
    }
}
