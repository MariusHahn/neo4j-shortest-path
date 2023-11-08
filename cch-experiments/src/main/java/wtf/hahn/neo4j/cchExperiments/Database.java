package wtf.hahn.neo4j.cchExperiments;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class Database implements AutoCloseable {

    private final DatabaseManagementService managementService;

    public Database(String fileName) {
        Path path = Paths.get(Application.dbDirName(fileName));
        managementService = new DatabaseManagementServiceBuilder(path.toAbsolutePath()).build();
    }

    GraphDatabaseService getDb() {
        return managementService.database(DEFAULT_DATABASE_NAME);
    }

    @Override
    public void close() {
        managementService.shutdown();
    }
}
