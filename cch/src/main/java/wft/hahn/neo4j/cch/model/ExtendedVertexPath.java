package wft.hahn.neo4j.cch.model;

import org.neo4j.internal.helpers.collection.PrefetchingIterator;

import java.util.Iterator;

public class ExtendedVertexPath implements VertexPath {
    private final VertexPath start;
    private final Arc lastArc;
    private final Vertex end;

    public ExtendedVertexPath(VertexPath start, Arc lastArc) {
        this.start = start;
        this.lastArc = lastArc;
        this.end = lastArc.otherVertex(start.end());
    }

    @Override
    public Vertex start() {
        return start.start();
    }

    @Override
    public Vertex end() {
        return end;
    }

    @Override
    public Arc lastArc() {
        return lastArc;
    }

    @Override
    public Iterable<Arc> arcs() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<Arc> startArcs =
                    start.arcs().iterator();
            boolean lastReturned;

            @Override
            protected Arc fetchNextOrNull() {
                if (startArcs.hasNext()) {
                    return startArcs.next();
                }
                if (!lastReturned) {
                    lastReturned = true;
                    return lastArc;
                }
                return null;
            }
        };
    }

    @Override
    public Iterable<Arc> reverseArcs() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<Arc> startRelationships =
                    start.reverseArcs().iterator();
            boolean endReturned;

            @Override
            protected Arc fetchNextOrNull() {
                if (!endReturned) {
                    endReturned = true;
                    return lastArc;
                }
                return startRelationships.hasNext() ? startRelationships.next() : null;
            }
        };
    }

    @Override
    public Iterable<Vertex> vertices() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<Vertex> startNodes = start.vertices().iterator();
            boolean lastReturned;

            @Override
            protected Vertex fetchNextOrNull() {
                if (startNodes.hasNext()) {
                    return startNodes.next();
                }
                if (!lastReturned) {
                    lastReturned = true;
                    return end;
                }
                return null;
            }
        };
    }

    @Override
    public Iterable<Vertex> reverseVertices() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<Vertex> startNodes = start.reverseVertices().iterator();
            boolean endReturned;

            @Override
            protected Vertex fetchNextOrNull() {
                if (!endReturned) {
                    endReturned = true;
                    return end;
                }
                return startNodes.hasNext() ? startNodes.next() : null;
            }
        };
    }

    @Override
    public int length() {
        return start.length() + 1;
    }

    @Override
    public Iterator<PathElement> iterator() {
        return new PrefetchingIterator<>() {
            final Iterator<PathElement> startEntities = start.iterator();
            int lastReturned = 2;

            @Override
            protected PathElement fetchNextOrNull() {
                if (startEntities.hasNext()) {
                    return startEntities.next();
                }
                switch (lastReturned--) {
                    case 2:
                        return end;
                    case 1:
                        return lastArc;
                    default:
                        return null;
                }
            }
        };
    }

    @Override
    public int weight() {
        return start.weight() + lastArc.weight;
    }

    public static VertexPath extend(VertexPath path, Arc withArc) {
        return new ExtendedVertexPath(path, withArc);
    }
}
