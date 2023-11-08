package wtf.hahn.neo4j.util.importer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

@RequiredArgsConstructor
public class FileImporter {
    public static final Label LABEL = Label.label("Location");
    private final FileLoader fileLoader;
    private final GraphDatabaseService db;
    private final Map<Integer, String> idMapping = new HashMap<>();
    private final Map<LoadFileRelationship,LoadFileRelationship> seen = new HashMap<>();
    private final int periodicCommitEvery = 10000;

    public void importAllNodes() throws IOException {
        int counter = 0;
        val relationshipIterator = fileLoader.loadFileRelationships().iterator();
        while (relationshipIterator.hasNext()) {
            try (Transaction transaction = db.beginTx()){
                Map<Integer, Node> nodes = new HashMap<>();
                for (int i = 0; i < periodicCommitEvery && relationshipIterator.hasNext(); i++) {
                    if (counter++ % 10000 == 0) System.out.println(counter + " vertices imported");
                    importRelationship(nodes, relationshipIterator, transaction);
                }
                transaction.commit();
            }
        }
    }

    private void importRelationship(Map<Integer, Node> nodes, Iterator<LoadFileRelationship> relationshipIterator,
                           Transaction transaction) {
        LoadFileRelationship grLine = relationshipIterator.next();
        nodes.computeIfAbsent(grLine.startId(), key -> getOrCreateNode(transaction, key));
        Node s = nodes.computeIfAbsent(grLine.startId(), key -> getOrCreateNode(transaction, key));
        idMapping.put(grLine.startId(), s.getElementId());
        Node t = nodes.computeIfAbsent(grLine.endId(), key -> getOrCreateNode(transaction, key));
        idMapping.put(grLine.endId(), t.getElementId());
        if (!seen.containsKey(grLine)) {
            Relationship road = s.createRelationshipTo(t, RelationshipType.withName("ROAD"));
            road.setProperty("cost", grLine.distance());
            seen.put(grLine, grLine);
        }
    }

    private Node getOrCreateNode(Transaction transaction, Integer id) {
        if (idMapping.containsKey(id)) return transaction.getNodeByElementId(idMapping.get(id));
        Node node = transaction.createNode();
        node.addLabel(LABEL);
        node.setProperty("id", id);
        node.setProperty("name", "V%d".formatted(id));
        return node;
    }
}
