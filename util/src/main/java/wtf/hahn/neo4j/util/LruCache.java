package wtf.hahn.neo4j.util;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class LruCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    public LruCache(int capacity) {
        super(Double.valueOf(Math.ceil(capacity / 0.75)).intValue()
                , 0.75f
                , true
        );
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    @Override
    public V get(Object key) {
        if (!containsKey(key)) insertValues((K) key);
        return super.get(key);
    }

    protected abstract void insertValues(K key);
}
