import static java.util.List.of;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.IndexerByImportanceWithSearchGraph;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.storage.IndexStoreFunction;
import wft.hahn.neo4j.cch.storage.Mode;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.importer.FileImporter;
import wtf.hahn.neo4j.util.importer.GrFileLoader;

public class OldenburgTest extends IntegrationTest {

    private final String costProperty = "cost";
    private final String edgeLabel = "ROAD";

    public OldenburgTest() {
        super(of(), of(), of(), null);
    }

    @Test
    void x() throws Exception {
        FileImporter fileImporter = new FileImporter(new GrFileLoader(resourcePath().resolve("oldenburg2.gr")), database());
        fileImporter.importAllNodes();
        try (Transaction transaction = database().beginTx()) {
            Vertex topNode = new IndexerByImportanceWithSearchGraph(
                    edgeLabel, costProperty, transaction
            ).insertShortcuts();
            try (IndexStoreFunction outStoreFunction = new IndexStoreFunction(topNode, Mode.OUT);
                 IndexStoreFunction inStoreFunction = new IndexStoreFunction(topNode, Mode.IN)) {
                outStoreFunction.go();
                inStoreFunction.go();
            }
        }
    }
}
