package wft.hahn.neo4j.cch.storage;

import java.util.Set;

public interface Buffer extends AutoCloseable {
    Set<DiskArc> arcs(int rank);
    Mode mode();
    int getLoadInvocations();
}
