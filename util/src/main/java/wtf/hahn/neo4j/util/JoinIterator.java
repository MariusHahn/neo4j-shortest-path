package wtf.hahn.neo4j.util;

import java.util.Iterator;

public class JoinIterator<T> implements Iterator<T>, Iterable<T> {

    private final Iterator<? extends T> first;
    private final Iterator<? extends T> second;

    public JoinIterator(Iterable<? extends T> first, Iterable<? extends T> second) {
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
        return first.hasNext() ? first.next() : second.next();

    }
}
