package wtf.hahn.neo4j.util;

import lombok.Value;

import java.util.function.Supplier;

@Value
public class StoppedResult<T> {
    T result;
    long micros;

    public StoppedResult(Supplier<T> supplier) {
        long start = System.nanoTime()/1000;
        this.result = supplier.get();
        this.micros = System.nanoTime()/1000 - start;
    }
}
