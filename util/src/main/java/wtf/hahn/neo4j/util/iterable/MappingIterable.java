package wtf.hahn.neo4j.util.iterable;

import java.util.Iterator;
import java.util.function.Function;

public class MappingIterable<T, R> implements Iterable<R> {

    private final Function<T, R> mapping;
    private final Iterable<T> iterable;

    public MappingIterable(Iterable<T> iterable, Function<T,R> mapping) {
        this.mapping = mapping;
        this.iterable = iterable;
    }

    @Override
    public Iterator<R> iterator() {
        Iterator<T> iterator = iterable.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) throw new UnsupportedOperationException();
                return mapping.apply(iterator.next());
            }
        };
    }
}
