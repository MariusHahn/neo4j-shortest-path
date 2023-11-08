package wtf.hahn.neo4j.cchExperiments;

import lombok.val;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.indexer.ContractionInsights;
import wft.hahn.neo4j.cch.indexer.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.StoreFunction;
import wft.hahn.neo4j.cch.storage.Writer;
import wtf.hahn.neo4j.util.importer.FileImporter;
import wtf.hahn.neo4j.util.importer.GrFileLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImportAndIndex {

    public static final String RELATIONSHIP_NAME = "ROAD";
    public static final String WEIGH_PROPERTY = "cost";
    private final String fileName;
    private final FileImporter fileImporter;
    private final GraphDatabaseService db;
    private final Path databasePath;

    public ImportAndIndex(String fileName, GraphDatabaseService db) {
        this.fileName = fileName;
        databasePath = createIfNotExisting(Application.dbDirName(fileName));
        this.db = db;
        fileImporter = new FileImporter(new GrFileLoader(Paths.get(fileName)), db);
    }

    public void go() throws IOException {
        System.out.println("Start import!");
        fileImporter.importAllNodes();
        System.out.println("End import!");
        System.out.println("Start indexing!");
        Vertex vertex;
        try (Transaction transaction = db.beginTx()) {
            val indexer = new IndexerByImportanceWithSearchGraph(RELATIONSHIP_NAME, WEIGH_PROPERTY, transaction);
            vertex = indexer.insertShortcuts();
            writeInfoFile(indexer.contractionInsights);
        }
        System.out.println("End Indexing!");
        System.out.println("Save Index!");
        try (StoreFunction storeFunctionOut = new StoreFunction(vertex, Mode.OUT, databasePath);
             StoreFunction storeFunctionIn = new StoreFunction(vertex, Mode.IN, databasePath)) {
            storeFunctionOut.go();
            storeFunctionIn.go();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("index persisted!");

    }

    private void writeInfoFile(ContractionInsights contractionInsights) {
        final Path infoFIle = databasePath.resolve("contraction.info");
        try {
            infoFIle.toFile().createNewFile();
        final String format1 = String.format("""
                contraction time:    %10d
                shortcuts inserted:  %10d
                max ingoing degree:  %10d
                max outgoing degree: %10d
                disk block size:     %10d
                """.stripIndent()
                , contractionInsights.contractionTime
                , contractionInsights.getInsertionCounter()
                , contractionInsights.getInDegree()
                , contractionInsights.getOutDegree()
                , Writer.DISK_BLOCK_SIZE
        );
            Files.writeString(infoFIle, format1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static Path createIfNotExisting(String folderName) {
        Path path = Path.of(folderName);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }
}
