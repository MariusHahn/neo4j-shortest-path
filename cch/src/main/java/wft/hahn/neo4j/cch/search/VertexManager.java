package wft.hahn.neo4j.cch.search;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.val;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.DiskArc;
import wft.hahn.neo4j.cch.storage.FifoBuffer;
import wft.hahn.neo4j.cch.storage.Mode;

@RequiredArgsConstructor
public class VertexManager {
    private final FifoBuffer fifoBuffer;
    private final Map<Integer, SearchVertex> vertices = new HashMap<>();

    public SearchVertex getVertex(int rank) {
        if (!vertices.containsKey(rank) && rank > -1) {
            SearchVertex start = vertices.computeIfAbsent(rank, SearchVertex::new);
            Iterable<DiskArc> arcs = fifoBuffer.arcs(rank);
            for (DiskArc arc : arcs) {
                SearchVertex end = vertices.computeIfAbsent(fifoBuffer.mode == Mode.OUT ? arc.end() : arc.start(), SearchVertex::new);
                if (arc.middle() != Vertex.UNSET) {
                    vertices.computeIfAbsent(arc.middle(), SearchVertex::new);
                }
                start.addArc(new SearchArc(start, end, vertices.get(arc.middle()), arc.weight()));
            }
        }
        return vertices.get(rank);
    }

    void addArcs(SearchVertex vertex) {
        for (DiskArc arc : fifoBuffer.arcs(vertex.rank)) {
            val target = getVertex(fifoBuffer.mode == Mode.OUT ? arc.end() : arc.start());
            val middle = getVertex(arc.middle());
            vertex.addArc(new SearchArc(vertex, target, middle, arc.weight()));
        }
    }
}
