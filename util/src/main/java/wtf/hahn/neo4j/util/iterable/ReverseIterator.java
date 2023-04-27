package wtf.hahn.neo4j.util.iterable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicInteger;

public class ReverseIterator<T> implements Iterable<T> {

    private final List<T> list;

    public ReverseIterator(Iterable<T> iterable) {
        if (iterable instanceof RandomAccess) {
            list = (List<T>) iterable;
        } else {
            list = new ArrayList<>();
            for (T t : iterable) list.add(t);
        }
    }

    @Override
    public Iterator<T> iterator() {
        AtomicInteger position = new AtomicInteger(list.size()-1);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return position.get() >= 0;
            }

            @Override
            public T next() {
                return list.get(position.getAndDecrement());
            }
        };
    }
}
