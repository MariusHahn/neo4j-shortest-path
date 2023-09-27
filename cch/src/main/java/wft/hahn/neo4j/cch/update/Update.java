package wft.hahn.neo4j.cch.update;

import java.util.Objects;

record Update(int fromRank, int toRank, double newWeightCandidate, double oldIndexWeight)
        implements Comparable<Update> {

    public Update(Triangle triangle, double newWeightCandidate, float oldIndexWeight) {
        this(triangle.c().start.rank, triangle.c().end.rank, newWeightCandidate, oldIndexWeight);
    }

    @Override
    public int compareTo(Update o) {
        return Integer.compare(Math.min(fromRank, toRank), Math.min(o.fromRank, o.toRank));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Update update = (Update) o;
        return fromRank == update.fromRank && toRank == update.toRank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromRank, toRank);
    }
}
