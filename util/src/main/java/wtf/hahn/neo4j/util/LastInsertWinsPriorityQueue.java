package wtf.hahn.neo4j.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LastInsertWinsPriorityQueue<E extends Comparable<E>> {
    private final Map<E, Integer> insertionCounter = new HashMap<>();
    private final Map<E, QueueElement<E>> control = new HashMap<>();
    private final Queue<QueueElement<E>> queue;

    public LastInsertWinsPriorityQueue(Stream<? extends Comparable<E>> c) {
        queue = c.map(e -> new QueueElement<>((E) e, 0)).distinct()
                .collect(Collectors.toCollection(PriorityQueue::new));
    }

    public boolean offer(E e) {
        Objects.requireNonNull(e);
        final QueueElement<E> newE = !control.containsKey(e)
                ? new QueueElement<>(e, 1)
                : new QueueElement<>(e, control.get(e).v + 1);
        insertionCounter.computeIfPresent(e, (key, oldValue) ->  oldValue + 1);
        insertionCounter.putIfAbsent(e, 2);
        control.put(e, newE);
        return queue.offer(newE);
    }

    public E poll() {
        if (isEmpty()) {
            return null;
        }
        QueueElement<E> poll = queue.poll();
        decrementControlAndInsertCounter(poll);
        return poll.e;
    }

    private void decrementControlAndInsertCounter(QueueElement<E> poll) {
        insertionCounter.computeIfPresent(poll.e, (key, oldValue) -> oldValue -1);
        if (insertionCounter.getOrDefault(poll.e(), -1) == 0) {
            insertionCounter.remove(poll.e());
            control.remove(poll.e());
        }
    }

    public boolean isEmpty() {
        while (wrongVersion()) {
            QueueElement<E> poll = queue.poll();
            decrementControlAndInsertCounter(poll);
        }
        return queue.isEmpty();
    }

    private boolean wrongVersion() {
        return !(queue.isEmpty()
                || !control.containsKey(queue.peek().e)
                || control.get(queue.peek().e).v == queue.peek().v);
    }

    private record QueueElement<E extends Comparable<E>>(E e, int v) implements Comparable<QueueElement<E>>{
        @Override
        public int compareTo(QueueElement<E> o) {
            return e.compareTo(o.e);
        }
    }
}
