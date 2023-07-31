package wft.hahn.neo4j.cch.search;

import java.util.Iterator;

import org.neo4j.internal.helpers.collection.PrefetchingIterator;
import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.PathElement;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.model.VertexPath;

public class ExtendedSearchPath implements SearchPath {
    private final SearchPath start;
    private final SearchArc lastArc;
    private final SearchVertex end;

    public ExtendedSearchPath(SearchPath start, SearchArc lastArc) {
        this.start = start;
        this.lastArc = lastArc;
        this.end = lastArc.otherVertex(start.end());
    }

    @Override
    public SearchVertex start() {
        return start.start();
    }

    @Override
    public SearchVertex end() {
        return end;
    }

    @Override
    public SearchArc lastArc() {
        return lastArc;
    }

    @Override
    public Iterable<SearchArc> arcs() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<SearchArc> startArcs =
                    start.arcs().iterator();
            boolean lastReturned;

            @Override
            protected SearchArc fetchNextOrNull() {
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
    public Iterable<SearchArc> reverseArcs() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<SearchArc> startRelationships =
                    start.reverseArcs().iterator();
            boolean endReturned;

            @Override
            protected SearchArc fetchNextOrNull() {
                if (!endReturned) {
                    endReturned = true;
                    return lastArc;
                }
                return startRelationships.hasNext() ? startRelationships.next() : null;
            }
        };
    }

    @Override
    public Iterable<SearchVertex> vertices() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<SearchVertex> startNodes = start.vertices().iterator();
            boolean lastReturned;

            @Override
            protected SearchVertex fetchNextOrNull() {
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
    public Iterable<SearchVertex> reverseVertices() {
        return () -> new PrefetchingIterator<>() {
            final Iterator<SearchVertex> startNodes = start.reverseVertices().iterator();
            boolean endReturned;

            @Override
            protected SearchVertex fetchNextOrNull() {
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
    public Iterator<SearchPathElement> iterator() {
        return new PrefetchingIterator<>() {
            final Iterator<SearchPathElement> startEntities = start.iterator();
            int lastReturned = 2;

            @Override
            protected SearchPathElement fetchNextOrNull() {
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
    public float weight() {
        return start.weight() + lastArc.weight;
    }

    public static SearchPath extend(SearchPath path, SearchArc withArc) {
        return new ExtendedSearchPath(path, withArc);
    }

    @Override
    public String toString() {
        return SearchVertexPaths.toString(this);
    }
}
