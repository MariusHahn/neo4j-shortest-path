package wft.hahn.neo4j.cch.update;

import lombok.RequiredArgsConstructor;
import wft.hahn.neo4j.cch.model.Arc;

@RequiredArgsConstructor
public class Triangle {
    public final Arc a,b,c;
    public double weight() {
        return b.weight + c.weight;
    }
}
