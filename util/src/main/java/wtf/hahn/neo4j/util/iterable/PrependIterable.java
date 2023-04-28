package wtf.hahn.neo4j.util.iterable;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrependIterable<T> implements Iterable<T> {

    private final T first;
    private final Iterable<T> iterable;


    public PrependIterable(T first, Iterable<T> iterable) {
        this.first = first;
        this.iterable = iterable;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> iterator = iterable.iterator();
        AtomicBoolean getFirst = new AtomicBoolean(true);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                if (getFirst.get()) {
                    getFirst.set(false);
                    return first;
                }
                if (!hasNext()) throw new UnsupportedOperationException();
                return iterator.next();
            }
        };
    }
}
