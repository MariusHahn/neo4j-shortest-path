package wtf.hahn.neo4j.procedure;

import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import wtf.hahn.neo4j.contractionHierarchies.ContractionHierarchies;
import wtf.hahn.neo4j.model.PathResult;

public class CHProcedures {

    @Context public GraphDatabaseService graphDatabaseService;
    @Context public Transaction transaction;


    @SuppressWarnings("unused")
    @Procedure(mode = Mode.WRITE)
    public void createContractionHierarchiesIndex(@Name("type") String type,
                                                  @Name("costProperty") String costProperty) {
            new ContractionHierarchies(graphDatabaseService, transaction)
                    .createContractionHierarchiesIndex(type, costProperty);
    }
    @SuppressWarnings("unused")
    @Procedure
    public Stream<PathResult> sourceTargetCH(@Name("startNode") Node startNode,
                                             @Name("endNode") Node endNode,
                                             @Name("type") String type,
                                             @Name("costProperty") String costProperty
    ){
        return new ContractionHierarchies(graphDatabaseService, transaction)
                .sourceTargetCH(
                        startNode
                        , endNode
                        , type
                        , costProperty);

    }
}
