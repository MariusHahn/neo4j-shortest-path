package wtf.hahn.neo4j.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

public class ReverseIteratorTest {


    @Test
    void reverseTest() {
        List<Integer> sampleList = List.of(1, 2, 3, 4, 5);
        Iterator<Integer> integers = new ReverseIterator<>(sampleList);
        int listPosition = sampleList.size()-1;
        while (integers.hasNext()) {
            Assertions.assertEquals(sampleList.get(listPosition--), integers.next());
        }
    }
}
