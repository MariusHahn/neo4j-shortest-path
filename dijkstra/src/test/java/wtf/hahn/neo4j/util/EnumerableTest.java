package wtf.hahn.neo4j.util;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class EnumerableTest {

    private final List<String> strings = List.of("Fritz", "Franz", "Hugo");

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 99,})
    void iterationTest(int startIndex) {
        AtomicInteger testIndex = new AtomicInteger(startIndex);
        Iterator<String> elements = strings.iterator();
        new Enumerable<>(startIndex, strings).forEachIndexed((index, element) -> {
            Assertions.assertEquals(testIndex.getAndIncrement(), index);
            Assertions.assertEquals(elements.next(), element);
            System.out.printf("%d: %s%n", index, element);
        });
    }

}
