import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import wtf.hahn.neo4j.contractionHierarchies.search.BidirectionChDijkstra;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.util.importer.GrFileImporter;
import wtf.hahn.neo4j.util.importer.GrFileLoader;
import wtf.hahn.neo4j.util.SimpleSetting;
import wtf.hahn.neo4j.util.StoppedResult;
import static wtf.hahn.neo4j.util.EntityHelper.*;

@Slf4j
public class Application {

    private final Parser parser;
    private final GraphDatabaseService db;

    public String connectionString() {
        return "jdbc:sqlite:"+ parser.getWorkingDirectory() + ".sqlite";
    }

    public Application(String[] args) throws SQLException, IOException {
        parser = new Parser(args);
        if (!parser.hasImportFileDefined()) {
            FileUtils.deleteDirectory(Paths.get(parser.getWorkingDirectory()).toFile());
        }
        Neo4j neo4j = createNeo4j();
        createSqlite();
        db = neo4j.defaultDatabaseService();
        log.debug("Start importing");
        GrFileImporter grFileImporter = new GrFileImporter(new GrFileLoader(parser.getCypherLocation()), db);
        grFileImporter.importAllNodes();
        log.debug("end importing");
        insertShortcuts(parser.getRelationshipType(), parser.getCostProperty());
        sourceTargetTests(parser.getSourceTargetCsvLocation());
        neo4j.close();
    }

    @SneakyThrows
    private void sourceTargetTests(Path sourceTargetCsvLocation) {
        val sql = "INSERT INTO search_result VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        val costProperty = parser.getCostProperty();
        val relationshipType = RelationshipType.withName(parser.getRelationshipType());
        try (val transaction = db.beginTx(); val sqlite = DriverManager.getConnection(connectionString())) {
            sqlite.setAutoCommit(false);
            PreparedStatement ps = sqlite.prepareStatement(sql);
            val evaluationContext = new BasicEvaluationContext(transaction, db);
            Dijkstra dijkstra2 = new Dijkstra(relationshipType, costProperty);
            BidirectionChDijkstra bidirectionChDijkstra = new BidirectionChDijkstra(relationshipType, costProperty);
            Files.lines(sourceTargetCsvLocation)
                    .map(line -> Arrays.stream(line.split(",")).map(Integer::parseInt).toArray(Integer[]::new))
                    .forEach(nodeIds -> {
                        val source = transaction.findNode(() -> "Location", "id", nodeIds[0]);
                        val target = transaction.findNode(() -> "Location", "id", nodeIds[1]);
                        val dijkstraResult = new StoppedResult<>(() -> dijkstra2.find(source, target));
                        if (Objects.nonNull(dijkstraResult.getResult())) {
                            val chResult = new StoppedResult<>(() -> bidirectionChDijkstra.find(source, target));
                            saveSearchResults(ps, chResult, dijkstraResult);
                        }
                    });
            sqlite.commit();
        }
    }

    @SneakyThrows
    private void saveSearchResults(PreparedStatement ps,
                                   StoppedResult<? extends WeightedPath> chResult,
                                   StoppedResult<? extends WeightedPath> dijkstraResult) {
        ps.setLong(1, getLongProperty(dijkstraResult.getResult().startNode(), "id"));
        ps.setLong(2, getLongProperty(dijkstraResult.getResult().endNode(), "id"));
        ps.setInt(3, dijkstraResult.getResult().length());
        WeightedPath chWPath = chResult.getResult();
        ps.setInt(4, chWPath== null ? -1 : chWPath.length());
        ps.setDouble(5, dijkstraResult.getResult().weight());
        ps.setDouble(6, chWPath== null ? -1 : chWPath.weight());
        ps.setLong(7, dijkstraResult.getMillis());
        ps.setLong(8, chResult.getMillis());
        ps.executeUpdate();
    }

    @SneakyThrows
    private void insertShortcuts(String relationshipType, String costProperty) {
        try (Transaction transaction = db.beginTx();
             Connection sqlite = DriverManager.getConnection(connectionString())) {
            Supplier<Integer> insertSupplier = () -> parser.getContractionAlgorithm(relationshipType, costProperty, transaction, db)
                    .insertShortcuts();
            StoppedResult<Integer> stoppedResult = new StoppedResult<>(insertSupplier);
            String sql = "INSERT INTO contraction_result VALUES (?, ?, ?, ?)";
            PreparedStatement ps = sqlite.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2, parser.getContractionAlgorithmName());
            ps.setLong(3, stoppedResult.getMillis());
            ps.setInt(4, stoppedResult.getResult());
            ps.executeUpdate();
            transaction.commit();
        }
    }

    private Neo4j createNeo4j() {
        return Neo4jBuilders
                .newInProcessBuilder()
                .withConfig(new SimpleSetting<>("dbms.security.allow_csv_import_from_file_urls", true), true)
                .withWorkingDir(createIfNotExisting(parser.getWorkingDirectory()))
                .withDisabledServer()
                .build();
    }

    @SneakyThrows(IOException.class)
    private static Path createIfNotExisting(String folderName) {
        Path path = Path.of(folderName);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    @SneakyThrows(IOException.class)
    private static String getCypherFromFile(Path path) {
        return Files.readString(path);
    }

    public static void main(String[] args) throws SQLException, IOException {
        new Application(args);
    }

    private void createSqlite() throws SQLException, IOException {
        try (Connection c = DriverManager.getConnection(connectionString())){
            System.err.println(Paths.get(".").toAbsolutePath());
            String readString = Files.readString(Paths.get("src", "main", "resources", "schema.sql"));
            Iterable<String> statements =  Arrays.stream(readString.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())::iterator;
            for (String sql : statements) {
                log.debug(sql);
                c.createStatement().execute(sql);
            }
        }
    }
}
