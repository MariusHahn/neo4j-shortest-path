package wtf.hahn.neo4j.util.iterable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Iterables {
    public static <T> Stream<T> stream(Iterable<T> it){
        return StreamSupport.stream(it.spliterator(), false);
    }

    public static <T> Stream<T> stream(Iterator<T> it){
        return stream(() -> it);
    }

    public static <T> int size(Iterable<T> iterable) {
        if (iterable instanceof Collection<T> c) {return c.size();}
        int size = 0; for (T ignored : iterable) size++;
        return size;
    }

    public static <T> T lastElement(Iterable<T> iterable) {
        if (iterable instanceof List<T> l) return l.get(l.size()-1);
        T last = null; for (T t : iterable) last = t;
        return last;
    }
}