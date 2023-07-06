package wft.hahn.neo4j.cch.model;

public record Arc(Vertex start, Vertex end, float weight, Vertex middle) implements PathElement {
    public Vertex otherVertex(Vertex vertex) {
        return start.equals(vertex) ? end : start;
    }
}
