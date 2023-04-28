package wtf.hahn.neo4j.util.iterable;

import java.util.Iterator;

public class JoinIterable<T> implements Iterable<T> {

    private final Iterable<? extends T> first;
    private final Iterable<? extends T> second;

    public JoinIterable(Iterable<? extends T> first, Iterable<? extends T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<? extends T> first = this.first.iterator();
        Iterator<? extends T> second = this.second.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return first.hasNext() || second.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) throw new UnsupportedOperationException();
                return first.hasNext() ? first.next() : second.next();

            }
        };
    }


}
