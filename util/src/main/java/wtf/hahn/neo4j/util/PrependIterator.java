package wtf.hahn.neo4j.util;

import java.util.Iterator;

public class PrependIterator<T> implements Iterator<T> {

    private final T first;
    private final Iterator<T> iterator;
    private boolean getFirst = true;

    public PrependIterator(T first, Iterable<T> iterable) {
        this.first = first;
        this.iterator = iterable.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        if (getFirst) {
            getFirst = false;
            return first;
        }
        if (!hasNext()) throw new UnsupportedOperationException();
        return iterator.next();
    }
}
