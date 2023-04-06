package wtf.hahn.neo4j.util;

import lombok.RequiredArgsConstructor;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class GrFileImporter {
    public static final Label LABEL = Label.label("Location");
    public static final String ID = "id";
    private final GrFileLoader grFileLoader;
    private final GraphDatabaseService db;

    public void importAllNodes() {
        Map<Integer, Node> nodes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        try (Transaction transaction = db.beginTx()) {
            grFileLoader.grLines().forEach(grLine -> {
                nodes.computeIfAbsent(grLine.startId(), key -> getOrCreateNode(transaction, key));
                Node s = nodes.computeIfAbsent(grLine.startId(), key -> getOrCreateNode(transaction, key));
                Node t = nodes.computeIfAbsent(grLine.endId(), key -> getOrCreateNode(transaction, key));
                Relationship road = s.createRelationshipTo(t, RelationshipType.withName("ROAD"));
                road.setProperty("cost", grLine.distance());
                if (counter.incrementAndGet() % 1000 == 0) {
                    System.out.printf("%d nodes relationships%n", counter.get());
                }
            });
            transaction.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void importLine(GrFileLoader.GrLine grLine, Transaction transaction) {
        Node s = getOrCreateNode(transaction, grLine.startId());
        Node t = getOrCreateNode(transaction, grLine.endId());
        s.createRelationshipTo(t, RelationshipType.withName("ROAD"));

    }

    private static Node getOrCreateNode(Transaction transaction, Integer id) {
        Node node = transaction.createNode();
        node.addLabel(LABEL);
        node.setProperty("id", id);
        node.setProperty("name", "V%d".formatted(id));
        return node;
    }
}
