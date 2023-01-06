package wtf.hahn.neo4j.util;

import java.util.Iterator;

public class ZipIterator<T> implements Iterator<T>, Iterable<T> {

    private final Iterator<? extends T> first;
    private final Iterator<? extends T> second;
    boolean takeFirstIterable = true;
    
    public ZipIterator(Iterable<? extends T> first, Iterable<? extends T> second) {
        this.first = first.iterator();
        this.second = second.iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return first.hasNext() || second.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) throw new UnsupportedOperationException();
        if (takeFirstIterable) {
            takeFirstIterable = false;
            return first.hasNext() ? first.next() : next();
        } else {
            takeFirstIterable = true;
            return second.hasNext() ? second.next() : next();
        }
    }
}
