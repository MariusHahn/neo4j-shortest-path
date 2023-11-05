package wtf.hahn.neo4j.cchExperiments;

import lombok.val;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import wft.hahn.neo4j.cch.indexer.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.storage.FifoBuffer;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.util.StoppedResult;
import wtf.hahn.neo4j.util.importer.FileImporter;
import wtf.hahn.neo4j.util.importer.GrFileLoader;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class Application {
    public Application(String[] args) throws IOException {
        Neo4j neo4j = createNeo4j("db-new-york");
        GraphDatabaseService db = neo4j.defaultDatabaseService();
        FileImporter fileImporter = new FileImporter(new GrFileLoader(Paths.get("USA-road-d.W.gr")), db);
        fileImporter.importAllNodes();
        Vertex vertex = null;
        try (Transaction transaction = db.beginTx()) {
            Relationship relationship = transaction.getAllRelationships().stream().findFirst().orElseThrow();
            System.out.println(relationship);
            val indexer = new IndexerByImportanceWithSearchGraph("ROAD", "cost", transaction);
            long l = System.currentTimeMillis();
            vertex = indexer.insertShortcuts();
            System.out.println((System.currentTimeMillis() - l) /1000 + "seconds to contract New york");
        }
        try (StoreFunction storeFunctionOut = new StoreFunction(vertex, Mode.OUT, Paths.get("."));
             StoreFunction storeFunctionIn = new StoreFunction(vertex, Mode.IN, Paths.get("."))) {
            storeFunctionOut.go();
            storeFunctionIn.go();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try (Transaction transaction = db.beginTx();
             FifoBuffer outBuffer = new FifoBuffer(213474044, Mode.OUT, Paths.get("."));
             FifoBuffer inBuffer = new FifoBuffer(213_474_044, Mode.OUT, Paths.get("."));
             DiskChDijkstra diskChDijkstra = new DiskChDijkstra(outBuffer, inBuffer)) {
            Random fromRandom = new Random(37);
            for (int i = 0; i < 100; i++) {
                Random toRandom = new Random(73);
                for (int j = 0; j < 100; j++) {
                    int x = fromRandom.nextInt(vertex.rank), y = toRandom.nextInt(vertex.rank);
                    Node from = transaction.findNode(() -> "Location", "ROAD_rank", x);
                    Node to = transaction.findNode(() -> "Location", "ROAD_rank", y);
                    Dijkstra dijkstra = new Dijkstra(() -> "ROAD", "cost");
                    val dijkstraPath = new StoppedResult<>(() -> dijkstra.find(from, to));
                    val chPath = new StoppedResult<>(() -> diskChDijkstra.find(x, y));
                    if (dijkstraPath.getResult() != null) {
                    assert chPath.getResult().weight() == dijkstraPath.getResult().weight();
                    System.out.printf("%9d,%9d,%12.2f,%4d,%4d%n",
                            dijkstraPath.getMillis(), chPath.getMillis(), chPath.getResult().weight()
                            , dijkstraPath.getResult().length(), chPath.getResult().length()
                            );
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        neo4j.close();
    }

    private Neo4j createNeo4j(String workingDirectory) throws IOException {
        return Neo4jBuilders
                .newInProcessBuilder()
                .withWorkingDir(createIfNotExisting(workingDirectory))
                .withDisabledServer()
                .build();
    }

    private static Path createIfNotExisting(String folderName) throws IOException {
        Path path = Path.of(folderName);
        if (!Files.exists(path)) {
                Files.createDirectories(path);
        }
        return path;
    }

    public static void main(String[] args) throws IOException {
        new Application(args);
    }
}