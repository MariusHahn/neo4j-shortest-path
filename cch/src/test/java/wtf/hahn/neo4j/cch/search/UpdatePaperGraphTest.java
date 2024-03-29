package wtf.hahn.neo4j.cch.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.model.Vertex;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.search.SearchPath;
import wft.hahn.neo4j.cch.search.SearchVertexPaths;
import wft.hahn.neo4j.cch.update.Updater;
import wtf.hahn.neo4j.cch.TestDataset;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.testUtil.IntegrationTest;
import wtf.hahn.neo4j.util.PathUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.List.of;
import static wtf.hahn.neo4j.cch.search.DiskChDijkstraTest.setupPaperGraphTest;
import static wtf.hahn.neo4j.util.EntityHelper.*;

public class UpdatePaperGraphTest extends IntegrationTest {

    @TempDir private static Path path;

    public UpdatePaperGraphTest() {
        super(of(), of(), of(), TestDataset.PAPER_GRAPH_UPDATE);

    }
    @BeforeAll void setUp() {
        Vertex[] vertices = fillVertices();
        fillUpwards(vertices);
        fillDownwards(vertices);
        setupPaperGraphTest(vertices[0], path);
    }

    @Test
    void updateIndexTest() {
        Dijkstra dijkstra = new Dijkstra(relationshipType(), costProperty());
        try (Transaction transaction = database().beginTx()) {
            List<Relationship> relationships = transaction.findRelationships(() -> "ROAD").stream().collect(Collectors.toList());
            Collections.shuffle(relationships, new Random(0));
            Random random = new Random(2); //2
            Updater updater = new Updater(transaction, path);
            Vertex top = null;
            for (int i = 0; i < 1000; i++) { //17
                Relationship relationship = relationships.get(i% (relationships.size()));
                double current = getDoubleProperty(relationship, "cost");
                double newWeight;
                if (Math.round(random.nextDouble()) == 0) newWeight = current * 2;
                else newWeight = current / 2;
                relationship.setProperty("cost", newWeight);
                relationship.setProperty("changed", true);
                Long fromRank = getProperty(relationship.getStartNode(), "ROAD_rank");
                Long toRank = getProperty(relationship.getEndNode(), "ROAD_rank");
                System.out.printf("(%d)-[%.2f]->(%d)%n", fromRank, newWeight, toRank);
                top = updater.update();
            }
            setupPaperGraphTest(top, path);
            try (DiskChDijkstra diskChDijkstra = new DiskChDijkstra(path)) {
                for (int i=0; i <=10; i++) for (int j = 0; j<=10; j++) {
                    Node start = transaction.findNode(() -> "Location", "id", i);
                    Node end = transaction.findNode(() -> "Location", "id", j);
                    WeightedPath expectPath = dijkstra.find(start, end);
                    int startRank = (int) getLongProperty(start, "ROAD_rank");
                    int endRank = (int) getLongProperty(end, "ROAD_rank");
                    SearchPath cchPath = diskChDijkstra.find(startRank, endRank);
                    System.out.println(SearchVertexPaths.toString(cchPath));
                    System.out.println(PathUtils.toRankString(expectPath));
                    Assertions.assertEquals(expectPath.weight(), cchPath.weight(), i + " -> " + j);
                }
            }
        }
    }

    private static void fillUpwards(Vertex[] vertices) {
        vertices[1].addArc(vertices[0], null, 1, 1);
        vertices[1].addArc(vertices[4], null, 1, 1);
        vertices[1].addArc(vertices[2], null, 2, 1);

        vertices[2].addArc(vertices[4], null, 2, 1);
        vertices[2].addArc(vertices[0], vertices[10], 3, 2);
        vertices[2].addArc(vertices[5], vertices[3], 4, 2);

        vertices[3].addArc(vertices[4], null, 1, 1);
        vertices[3].addArc(vertices[5], null, 2, 1);
        vertices[3].addArc(vertices[2], null, 2, 1);

        vertices[4].addArc(vertices[5], null, 3, 1);
        vertices[4].addArc(vertices[0], vertices[1], 2, 2);

        vertices[5].addArc(vertices[0], vertices[4], 5, 3);

        vertices[6].addArc(vertices[0], null, 2, 1);
        vertices[6].addArc(vertices[5], vertices[9], 5, 3);
        vertices[6].addArc(vertices[7], null, 2, 1);

        vertices[7].addArc(vertices[5], null, 4, 1);
        vertices[7].addArc(vertices[0], null, 4, 1);

        vertices[8].addArc(vertices[9], null, 2, 1);
        vertices[8].addArc(vertices[5], null, 2, 1);

        vertices[9].addArc(vertices[7], null, 1, 1);
        vertices[9].addArc(vertices[6], null, 1, 1);
        vertices[9].addArc(vertices[5], vertices[8], 4, 2);
        
        vertices[10].addArc(vertices[0], null, 1, 1);
        vertices[10].addArc(vertices[2], null, 2, 1);
    }
    
    private static void fillDownwards(Vertex[] vertices) {
        vertices[0].addArc(vertices[1], null, 1, 1);
        vertices[4].addArc(vertices[1], null, 1, 1);
        vertices[2].addArc(vertices[1], null, 2, 1);

        vertices[4].addArc(vertices[2], null, 2, 1);
        vertices[0].addArc(vertices[2], vertices[10], 3, 2);
        vertices[5].addArc(vertices[2], vertices[3], 4, 2);

        vertices[4].addArc(vertices[3], null, 1, 1);
        vertices[5].addArc(vertices[3], null, 2, 1);
        vertices[2].addArc(vertices[3], null, 2, 1);

        vertices[5].addArc(vertices[4], null, 3, 1);
        vertices[0].addArc(vertices[4], vertices[1], 2, 2);

        vertices[0].addArc(vertices[5], vertices[4], 5, 3);

        vertices[0].addArc(vertices[6], null, 2, 1);
        vertices[5].addArc(vertices[6], vertices[9], 5, 3);
        vertices[7].addArc(vertices[6], null, 2, 1);

        vertices[5].addArc(vertices[7], null, 4, 1);
        vertices[0].addArc(vertices[7], null, 4, 1);

        vertices[9].addArc(vertices[8], null, 2, 1);
        vertices[5].addArc(vertices[8], null, 2, 1);

        vertices[7].addArc(vertices[9], null, 1, 1);
        vertices[6].addArc(vertices[9], null, 1, 1);
        vertices[5].addArc(vertices[9], vertices[8], 4, 2);

        vertices[0].addArc(vertices[10], null, 1, 1);
        vertices[2].addArc(vertices[10], null, 2, 1);
        
    }

    private static Vertex[] fillVertices() {
        Vertex[] vertices = new Vertex[11];
        vertices[0] = new Vertex(null, "V0");
        vertices[0].rank = 10;
        vertices[1] = new Vertex(null, "V1");
        vertices[1].rank = 6;
        vertices[2] = new Vertex(null, "V2");
        vertices[2].rank = 7;
        vertices[3] = new Vertex(null, "V3");
        vertices[3].rank = 5;
        vertices[4] = new Vertex(null, "V4");
        vertices[4].rank = 8;
        vertices[5] = new Vertex(null, "V5");
        vertices[5].rank = 9;
        vertices[6] = new Vertex(null, "V6");
        vertices[6].rank = 3;
        vertices[7] = new Vertex(null, "V7");
        vertices[7].rank = 4;
        vertices[8] = new Vertex(null, "V8");
        vertices[8].rank = 0;
        vertices[9] = new Vertex(null, "V9");
        vertices[9].rank = 2;
        vertices[10] = new Vertex(null, "V10");
        vertices[10].rank = 1;
        return vertices;
    }
}
