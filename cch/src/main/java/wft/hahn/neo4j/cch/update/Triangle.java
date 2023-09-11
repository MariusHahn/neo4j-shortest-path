package wft.hahn.neo4j.cch.update;

import wft.hahn.neo4j.cch.model.Arc;

public record Triangle(Arc first, Arc second) {
    public double weight() {
        return first.weight + second.weight;
    }
}
