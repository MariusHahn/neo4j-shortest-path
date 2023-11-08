package wtf.hahn.neo4j.util.importer;

record LoadFileRelationship(Integer startId, Integer endId, Integer distance) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoadFileRelationship that = (LoadFileRelationship) o;

        if (!startId.equals(that.startId)) return false;
        if (!endId.equals(that.endId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startId.hashCode();
        result = 31 * result + endId.hashCode();
        return result;
    }
}
