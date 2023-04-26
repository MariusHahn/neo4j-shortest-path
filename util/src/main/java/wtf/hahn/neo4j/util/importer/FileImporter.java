package wtf.hahn.neo4j.util.importer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
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

    public void importAllNodes() throws IOException {
        Map<Integer, Node> nodes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        Iterator<LoadFileRelationship> relationshipIterator = fileLoader.loadFileRelationships().iterator();
        while (relationshipIterator.hasNext()) {
            try (Transaction transaction = db.beginTx()){
                for (int i = 0; i < 10000 && relationshipIterator.hasNext(); i++) {
                    LoadFileRelationship grLine = relationshipIterator.next();
                    nodes.computeIfAbsent(grLine.startId(), key -> getOrCreateNode(transaction, key));
                    Node s = nodes.computeIfAbsent(grLine.startId(), key -> getOrCreateNode(transaction, key));
                    idMapping.put(grLine.startId(), s.getElementId());
                    Node t = nodes.computeIfAbsent(grLine.endId(), key -> getOrCreateNode(transaction, key));
                    idMapping.put(grLine.endId(), s.getElementId());
                    Relationship road = s.createRelationshipTo(t, RelationshipType.withName("ROAD"));
                    road.setProperty("cost", grLine.distance());
                    counter.incrementAndGet();
                }
                System.out.printf("%d nodes relationships%n", counter.get());
                nodes.clear();
                transaction.commit();
            }
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
