package wft.hahn.neo4j.cch.indexer;

import wft.hahn.neo4j.cch.model.Arc;

public record Shortcut(Arc in, Arc out, int weight, int hopLength) {
    Shortcut(Arc in, Arc out) {
        this(in, out, in.weight + out.weight, in.hopLength + out.hopLength);
    }
}
