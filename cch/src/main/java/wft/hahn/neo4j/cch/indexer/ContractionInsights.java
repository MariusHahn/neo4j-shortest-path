package wft.hahn.neo4j.cch.indexer;

import lombok.Getter;
import lombok.ToString;
import wft.hahn.neo4j.cch.model.Vertex;

@ToString @Getter
public final class ContractionInsights {
    public long contractionTime;
    private int maxDegree = 0;
    private int insertionCounter = 0;
    private int outDegree = 0;
    private int inDegree = 0;

    public void updateMaxDegree(Vertex vertex) {
        inDegree = Math.max(vertex.inArcs().size(), inDegree);
        outDegree = Math.max(vertex.outArcs().size(), outDegree);
        maxDegree = Math.max(vertex.getDegree(), maxDegree);
    }

    public void addToInsertionCounter(int toAdd) {
        insertionCounter += toAdd;
    }
}
