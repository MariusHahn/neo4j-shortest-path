package wft.hahn.neo4j.cch.update;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.ArcReader;
import wft.hahn.neo4j.cch.storage.Mode;

public class IndexGraphLoader {
    private final ArcReader outReader;
    private final Map<Integer, Vertex> vertices;
    private final ArcReader inReader;

    public IndexGraphLoader(Path path) {
        outReader = new ArcReader(Mode.OUT, path);
        inReader = new ArcReader(Mode.IN, path);
        vertices = new HashMap<>();
    }

    public Vertex load() {
        loadArcs(outReader);
        loadArcs(inReader);
        return vertices.values().stream().max(Comparator.comparingInt(o -> o.rank)).orElseThrow();
    }

    private void loadArcs(ArcReader reader) {
        reader.getAllArcs().forEach(diskArc -> {
            final Vertex start = vertices.computeIfAbsent(diskArc.start(), Vertex::new);
            final Vertex end = vertices.computeIfAbsent(diskArc.end(), Vertex::new);
            final Vertex middle = diskArc.middle() == -1 ? null : vertices.computeIfAbsent(diskArc.middle(), Vertex::new);
            start.addArc(end,middle, diskArc.weight(), -1);
        });
    }

    public Arc getArc(int fromRank, int toRank) {
        if (!vertices.containsKey(fromRank)|| ! vertices.containsKey(toRank)) throw new IllegalStateException();
        return vertices.get(fromRank).getArcTo(vertices.get(toRank));
    }
}
