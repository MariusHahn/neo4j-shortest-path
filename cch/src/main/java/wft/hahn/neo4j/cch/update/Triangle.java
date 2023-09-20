package wft.hahn.neo4j.cch.update;

import wft.hahn.neo4j.cch.model.Arc;

public record Triangle(Arc a, Arc b, Arc c) {
    public double weight() {
        return b.weight + c.weight;
    }
}
