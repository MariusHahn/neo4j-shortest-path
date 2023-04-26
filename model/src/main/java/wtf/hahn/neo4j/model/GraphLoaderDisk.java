package wtf.hahn.neo4j.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

@RequiredArgsConstructor
public class GraphLoaderDisk implements GraphLoader {

    private final Transaction transaction;

    @Override
    public Set<Node> loadAllNodes(RelationshipType type) {
        Set<Node> nodes = new HashSet<>();
        Iterable<Relationship> relationships = () -> transaction.findRelationships(type);
        for (Relationship relationship : relationships) {
            nodes.add(relationship.getStartNode());
            nodes.add(relationship.getEndNode());
        }
        return nodes;
    }

    @Override
    public void saveAllNode(Collection<Node> nodes) {
    }
}
