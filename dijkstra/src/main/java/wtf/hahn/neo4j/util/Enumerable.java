package wtf.hahn.neo4j.util;

import java.util.function.BiConsumer;

public class Enumerable<T> {

    private final Iterable<T> iterable;
    private int index;

    public Enumerable(Iterable<T> iterable) {
        this(0, iterable);
    }

    public Enumerable(int startIndex, Iterable<T> iterable) {
        this.iterable = iterable;
        index = startIndex;
    }

    public void forEachIndexed(BiConsumer<Integer, T> consumer) {
        for (T element : iterable) {
            consumer.accept(index, element);
            index++;
        }
    }

    public static <T> Enumerable<T> enumerate(Iterable<T> iterable) {
        return new Enumerable<>(iterable);
    }

    public static <T> Enumerable<T> enumerate(int startIndex, Iterable<T> iterable) {
        return new Enumerable<>(startIndex, iterable);
    }
}
