package wtf.hahn.neo4j.util;

import java.util.Iterator;
import java.util.function.Function;

public class MappingIterator<T, R> implements Iterator<R> {

    private final Function<T, R> mapping;
    private final Iterator<T> iterator;

    public MappingIterator(Iterable<T> iterable, Function<T,R> mapping) {
        this.mapping = mapping;
        this.iterator = iterable.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public R next() {
        if (!hasNext()) throw new UnsupportedOperationException();
        return mapping.apply(iterator.next());
    }
}
