package wtf.hahn.neo4j.model;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import java.util.Collection;
import java.util.Set;

public interface GraphLoader {
    Set<Node> loadAllNodes(RelationshipType type);

    void saveAllNode(Collection<Node> nodes);
}
