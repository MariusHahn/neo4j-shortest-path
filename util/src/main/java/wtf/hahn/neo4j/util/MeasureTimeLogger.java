package wtf.hahn.neo4j.util;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.Supplier;

public final class MeasureTimeLogger<T> {
    private final T result;

    public MeasureTimeLogger(Supplier<T> s, String className, String methodName) {
        long start = System.currentTimeMillis();
        result = s.get();
        System.err.printf("%d ms for %s.%s%n", System.currentTimeMillis() - start, className, methodName);

    }
    public MeasureTimeLogger(Runnable s, String className, String methodName) {
        result = null;
        long start = System.currentTimeMillis();
        s.run();
        System.err.printf("%d ms for %s.%s%n", System.currentTimeMillis() - start, className, methodName);
    }

    public T get() {
        return result;
    }

}
