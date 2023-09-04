package wft.hahn.neo4j.cch.storage;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wtf.hahn.neo4j.util.LruCache;

public class LruBuffer extends LruCache<Integer, Set<DiskArc>> implements Buffer {

    private final Mode mode;
    private final ArcReader arcReader;
    private int loadInvocations = 0;

    public LruBuffer(int capacity, Mode mode, Path basePath) {
        super(capacity);
        this.mode = mode;
        arcReader = new ArcReader(this.mode, basePath);

    }

    @Override
    public Set<DiskArc> arcs(int rank) {
        Set<DiskArc> diskArcs = get(rank);
        return (diskArcs == null) ? Collections.emptySet() : diskArcs;
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public int getLoadInvocations() {
        return loadInvocations;
    }

    private int getKey(DiskArc arc) {
        return mode == Mode.OUT ? arc.start() : arc.end();
    }

    @Override
    public void close() throws Exception {
        arcReader.close();
    }

    @Override
    protected void insertValues(Integer rankToLoad) {
        loadInvocations++;
        if (!containsKey(rankToLoad)) put(rankToLoad, new HashSet<>());
        for (DiskArc arc : arcReader.getArcs(rankToLoad)) {
            int rank = getKey(arc);
            if (!containsKey(rank)) put(rank, new HashSet<>());
            get(rank).add(arc);
        }
    }
}
