package wtf.hahn.neo4j.testUtil;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Config;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilder;
import org.neo4j.harness.Neo4jBuilders;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    protected final Config build;
    protected final Neo4j neo4j;
    protected final URI uri;
    protected static final String DB_NAME = "neo4j"; // default name give by neo4j community edition
    private final Dataset dataset;

    public IntegrationTest(Collection<Class> aggregationFunctions, Collection<Class> procedures,
                           Collection<Class> functions, DatasetEnum datasetEnum) {
        final Dataset dataset = datasetEnum.dataset();
        build = Config.builder().withoutEncryption().build();
        Neo4jBuilder neo4jBuilder = Neo4jBuilders.newInProcessBuilder();
        aggregationFunctions.forEach(neo4jBuilder::withAggregationFunction);
        procedures.forEach(neo4jBuilder::withProcedure);
        functions.forEach(neo4jBuilder::withFunction);
        neo4j = neo4jBuilder.withDisabledServer().withFixture(dataset.cypherPath()).build();
        uri = neo4j.boltURI();
        this.dataset = dataset;
    }

    protected String costProperty() {
        return dataset.costProperty;
    }

    protected RelationshipType relationshipType() {
        return RelationshipType.withName(dataset.relationshipTypeName);
    }

    protected GraphDatabaseService database() {
        return neo4j.databaseManagementService().database(DB_NAME);
    }

    @AfterAll
    public void stopNeo4j() {
        neo4j.close();
    }

    public static class Dataset {

        private final String fileName;
        private final Path resources;
        private final String costProperty;
        private final String relationshipTypeName;

        Dataset(Path resources, String fileName, String costProperty, String relationshipTypeName) {
            this.fileName = fileName;
            this.resources = resources;
            this.costProperty = costProperty;
            this.relationshipTypeName = relationshipTypeName;
        }

        public Dataset(String fileName, String costProperty, String relationshipTypeName) {
            this(Paths.get("src", "test", "resources"), fileName, costProperty, relationshipTypeName);
        }

        public Path cypherPath() {
            return resources.resolve(fileName);
        }
    }

    public interface DatasetEnum {
        String getFileName();
        String getCostProperty();
        String getRelationshipTypeName();

        default Dataset dataset() {
            return new Dataset(getFileName(),getCostProperty(), getRelationshipTypeName());
        }
    }
}
