package wft.hahn.neo4j.cch.update;

import wft.hahn.neo4j.cch.model.Arc;
import wft.hahn.neo4j.cch.model.Vertex;

public record Triangle(Arc a, Arc b, Arc c, Vertex middle) {
}
