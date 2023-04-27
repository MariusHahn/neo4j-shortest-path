package wtf.hahn.neo4j.util.iterable;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZipIterable<T> implements Iterable<T> {

    private final Iterable<? extends T> first;
    private final Iterable<? extends T> second;

    public ZipIterable(Iterable<? extends T> first, Iterable<? extends T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<? extends T> first = this.first.iterator();
        Iterator<? extends T> second = this.second.iterator();
        AtomicBoolean takeFirstIterable = new AtomicBoolean(true);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return first.hasNext() || second.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) throw new UnsupportedOperationException();
                if (takeFirstIterable.getAndSet(false)) {

                    return first.hasNext() ? first.next() : next();
                } else {
                    takeFirstIterable.set(true);
                    return second.hasNext() ? second.next() : next();
                }
            }
        };
    }
}
