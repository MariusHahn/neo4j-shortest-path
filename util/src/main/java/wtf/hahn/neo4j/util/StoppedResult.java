package wtf.hahn.neo4j.util;

import lombok.Value;

import java.util.function.Supplier;

@Value
public class StoppedResult<T> {
    T result;
    long millis;

    public StoppedResult(Supplier<T> supplier) {
        long start = System.nanoTime()/1000;
        this.result = supplier.get();
        this.millis = System.nanoTime()/1000 - start;
    }
}
