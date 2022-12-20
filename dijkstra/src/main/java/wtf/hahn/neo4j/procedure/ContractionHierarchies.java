package wtf.hahn.neo4j.procedure;

import java.util.Comparator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.contractionHierarchies.CH;

public class ContractionHierarchies {

    @Context
    public GraphDatabaseService graphDatabaseService;

    @SuppressWarnings("unused")
    @Procedure(mode = Mode.WRITE)
    public void createContractionHierarchiesIndex(@Name("type") String type,
                                                  @Name("costProperty") String costProperty) {
        try (Transaction transaction = graphDatabaseService.beginTx()) {
            new CH(type, costProperty, transaction, Comparator.comparingInt(Node::getDegree)).insertShortcuts();
            transaction.commit();
        }
    }

}
