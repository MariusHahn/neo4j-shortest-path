package wtf.hahn.neo4j.testUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilder;
import org.neo4j.harness.Neo4jBuilders;
import wtf.hahn.neo4j.util.SimpleSetting;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    protected final Neo4j neo4j;
    protected final Dataset dataset;

    public IntegrationTest(Collection<Class> aggregationFunctions, Collection<Class> procedures,
                           Collection<Class> functions, DatasetEnum datasetEnum) {
        final Dataset dataset = datasetEnum == null ? null : datasetEnum.dataset();
        Neo4jBuilder neo4jBuilder = Neo4jBuilders
                .newInProcessBuilder()
                .withDisabledServer()
                .withConfig(new SimpleSetting<>("server.directories.import", resourcePath().toAbsolutePath()), resourcePath().toAbsolutePath())
                ;
        if (dataset != null) neo4jBuilder = neo4jBuilder.withFixture(dataset.cypherPath());
        aggregationFunctions.forEach(neo4jBuilder::withAggregationFunction);
        procedures.forEach(neo4jBuilder::withProcedure);
        functions.forEach(neo4jBuilder::withFunction);
        neo4j = neo4jBuilder.build();
        this.dataset = dataset;
    }

    protected String costProperty() {
        return dataset.costProperty;
    }

    protected RelationshipType relationshipType() {
        return RelationshipType.withName(dataset.relationshipTypeName);
    }

    protected GraphDatabaseService database() {
        return neo4j.defaultDatabaseService();
    }

    @AfterAll
    public void stopNeo4j() {
        neo4j.close();
    }

    public static class  Dataset {

        public final String fileName;
        public final Path resources;
        public final String costProperty;
        public final String relationshipTypeName;

        Dataset(Path resources, String fileName, String costProperty, String relationshipTypeName) {
            this.fileName = fileName;
            this.resources = resources;
            this.costProperty = costProperty;
            this.relationshipTypeName = relationshipTypeName;
        }

        public Dataset(String fileName, String costProperty, String relationshipTypeName) {
            this(resourcePath(), fileName, costProperty, relationshipTypeName);
        }

        public Path cypherPath() {
            return resources.resolve(fileName);
        }
    }

    public static Path resourcePath() {
        return Paths.get("src", "test", "resources");
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
