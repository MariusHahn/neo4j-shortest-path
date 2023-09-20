package wft.hahn.neo4j.cch.update;

record Update(int fromRank, int toRank, double newWeightCandidate, double oldIndexWeight)
        implements Comparable<Update> {

    public Update(Triangle triangle, double newWeightCandidate) {
        this(triangle.c().start.rank, triangle.c().end.rank, newWeightCandidate, triangle.c().weight);
    }

    @Override
    public int compareTo(Update o) {
        int compare = Integer.compare(fromRank, toRank);
        return upwards() ? compare : compare * -1;
    }

    private boolean upwards() {
        return fromRank < toRank;
    }
}
