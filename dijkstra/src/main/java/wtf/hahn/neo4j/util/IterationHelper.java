package wtf.hahn.neo4j.util;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterationHelper {
    public static <T> Stream<T> stream(Iterable<T> it){
        return StreamSupport.stream(it.spliterator(), false);
    }

    public static <T> Stream<T> stream(Iterator<T> it){
        return stream(() -> it);
    }
}
