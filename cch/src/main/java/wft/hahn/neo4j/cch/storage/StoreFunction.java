package wft.hahn.neo4j.cch.storage;

import static wft.hahn.neo4j.cch.storage.Mode.IN;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import wft.hahn.neo4j.cch.model.Vertex;

public class StoreFunction implements AutoCloseable {

    private final Deque<Iterator<Vertex>> stack;
    private final Mode mode;
    private final PositionWriter positionWriter;
    private final ArcWriter arcWriter;

    public StoreFunction(Vertex topVertex, Mode mode, Path basePath) {
        this.mode = mode;
        stack = new LinkedList<>();
        positionWriter = new PositionWriter(mode, basePath, topVertex.rank + 1);
        arcWriter = new ArcWriter(mode, basePath);
        stack.add(List.of(topVertex).iterator());
    }

    public void go() {
        while (!stack.isEmpty()) {
            if (stack.peekFirst().hasNext()) {
                final Vertex vertex = stack.peekFirst().next();
                if (!positionWriter.alreadyWritten(vertex.rank)) {
                    final int position = arcWriter.write(vertex);
                    positionWriter.write(vertex.rank, position);
                    stack.addFirst(neighbors(vertex));
                }
            } else {
                stack.pollFirst();
            }
        }
    }

    private Iterator<Vertex> neighbors(Vertex vertex) {
        return (mode == IN ? vertex.outNeighbors() : vertex.inNeighbors()).sorted(
                (Comparator.comparingInt(o -> o.rank))).iterator();
    }

    @Override
    public void close() throws Exception {
        arcWriter.close();
        positionWriter.close();
    }
}
