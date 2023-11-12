package wtf.hahn.neo4j.cchExperiments;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wft.hahn.neo4j.cch.update.Updater;
import wtf.hahn.neo4j.util.StoppedResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static wtf.hahn.neo4j.util.EntityHelper.getIntProperty;

public class ChangeAndUpdate {
    private final Path path;
    private final GraphDatabaseService db;

    public ChangeAndUpdate(Path path, GraphDatabaseService db) {
        this.path = path;
        this.db = db;
    }

    long go() {
        try (Transaction transaction = db.beginTx()) {
            List<Relationship> relationships = transaction.findRelationships(() -> "ROAD").stream().collect(Collectors.toList());
            Collections.shuffle(relationships);
            System.out.println("Start neo update");
            for (int i = 0; i < relationships.size(); i++) {
                Relationship relationship = relationships.get(i);
                int current = getIntProperty(relationship, "cost");
                if (i % 2 == 0) relationship.setProperty("cost", current * 2);
                else relationship.setProperty("cost", ((int) current / 2));
                relationship.setProperty("changed", true);
            }
            System.out.println("End neo update");

            StoppedResult<Class<Void>> updateMeasure = new StoppedResult<>(() -> {
                Updater updater = new Updater(transaction, path);
                System.out.println("Start arc update");
                Vertex update = updater.update();
                System.out.println("End arc update");
                try (StoreFunction storeFunctionOut = new StoreFunction(update, Mode.OUT, path);
                     StoreFunction storeFunctionIn = new StoreFunction(update, Mode.IN, path)
                ) {
                    storeFunctionOut.go();
                    storeFunctionIn.go();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return Void.class;
            });
            transaction.commit();
            long time = System.currentTimeMillis();
            try {
                Files.writeString(path.resolve("update_%d.info".formatted(time)),
                        "update took: %d micro seconds".formatted(updateMeasure.getMicros()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return time;
        }
    }
}