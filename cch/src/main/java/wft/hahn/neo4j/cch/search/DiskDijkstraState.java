package wft.hahn.neo4j.cch.search;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexPath;
import wft.hahn.neo4j.cch.model.VertexPaths;
import wft.hahn.neo4j.cch.storage.BufferManager;

import java.util.Collection;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(of = "endVertex")
public class DiskDijkstraState implements Comparable<DiskDijkstraState>{
    private final SearchVertex endVertex;
    private final SearchPath path;
    private final DiskDijkstra.VertexManager vertexManager;
    public boolean settled;

    public void settle() {
        vertexManager.addArcs(endVertex);
        this.settled = true;
    }

    public DiskDijkstraState(SearchVertex endVertex, DiskDijkstra.VertexManager vertexManager) {
        this.endVertex = endVertex;
        path = SearchVertexPaths.singleSearchVertexPath(endVertex);
        settled = false;
        this.vertexManager = vertexManager;
    }

    public double weight() {
        return path.weight();
    }

    @Override
    public int compareTo(DiskDijkstraState o) {
        return Double.compare(path.weight(), o.path.weight());
    }
}
