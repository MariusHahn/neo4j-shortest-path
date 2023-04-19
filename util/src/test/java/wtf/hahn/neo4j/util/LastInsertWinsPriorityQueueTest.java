package wtf.hahn.neo4j.util;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;


public class LastInsertWinsPriorityQueueTest {
    @RequiredArgsConstructor @EqualsAndHashCode(of = "name") @ToString
    static final class TestObject implements Comparable<TestObject> {
        private final int id;
        private final String name;

        @Override
        public int compareTo(TestObject o) {
            return Integer.compare(this.id, o.id);
        }
    }

    @Test
    void a() {
        Stream<TestObject> testObjs = IntStream.rangeClosed(1, 10).mapToObj(i -> new TestObject(i + 10, "V" + i));
        LastInsertWinsPriorityQueue<TestObject> queue = new LastInsertWinsPriorityQueue<>(testObjs);
        System.out.println(queue.poll());
        queue.offer(new TestObject(19, "V7"));
        while (!queue.isEmpty()) System.out.println(queue.poll());
    }
}
