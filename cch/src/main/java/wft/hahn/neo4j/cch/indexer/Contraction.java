package wft.hahn.neo4j.cch.indexer;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import wft.hahn.neo4j.cch.model.Vertex;

import java.util.Collection;

@RequiredArgsConstructor
@EqualsAndHashCode(of = "vertexToContract")
class Contraction implements Comparable<Contraction> {
    public final Vertex vertexToContract;
    public final Integer edgeDifference;
    public final Collection<Shortcut> shortcuts;

    @Override
    public int compareTo(Contraction o) {
        return Double.compare(importance(), o.importance());
    }

    double importance() {
        return vertexToContract.contractedLevel // L(x)
                + (shortcuts.size() * 1.0 / vertexToContract.getDegree()) // |A(x)| / |D(x)|
                + (shortcutsHopLength(shortcuts) / vertexToContract.sumOfAtoDxHa())
                ;
    }

    private static double shortcutsHopLength(Collection<Shortcut> shortcuts) {
        int c = 0;
        for (Shortcut shortcut : shortcuts) c += shortcut.hopLength();
        return c;
    }
}
