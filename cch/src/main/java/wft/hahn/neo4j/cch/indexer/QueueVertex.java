package wft.hahn.neo4j.cch.indexer;

import wft.hahn.neo4j.cch.model.Vertex;

import java.util.Objects;

record QueueVertex(double importance, Vertex vertex) implements Comparable<QueueVertex> {
    public QueueVertex(Contraction e) {
        this(e.importance(), e.vertexToContract);
    }

    @Override
    public int compareTo(QueueVertex o) {
        return Double.compare(importance(), o.importance());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueueVertex that = (QueueVertex) o;
        return vertex.equals(that.vertex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertex);
    }
}
