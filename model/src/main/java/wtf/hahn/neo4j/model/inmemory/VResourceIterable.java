package wtf.hahn.neo4j.model.inmemory;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public class VResourceIterable<T> implements ResourceIterable<T> {

    private final Iterable<T> collection;

    public VResourceIterable(Iterable<T> collection) {
        this.collection = collection;
    }
    public VResourceIterable(Stream<T> collection) {
        this.collection = collection::iterator;
    }

    @Override
    public ResourceIterator<T> iterator() {
        Iterator<T> iterator = collection.iterator();
        return new ResourceIterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void close() {

    }
}
