package wtf.hahn.neo4j.cchExperiments;

import lombok.val;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import wft.hahn.neo4j.cch.search.DiskChDijkstra;
import wft.hahn.neo4j.cch.storage.FifoBuffer;
import wft.hahn.neo4j.cch.storage.Mode;
import wtf.hahn.neo4j.dijkstra.Dijkstra;
import wtf.hahn.neo4j.util.StoppedResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static wtf.hahn.neo4j.util.EntityHelper.getId;
import static wtf.hahn.neo4j.util.EntityHelper.getLongProperty;

public class MeasureQueries {

    public static final Label LABEL = () -> "Location";
    public static final String ROAD_RANK_PROPERTY = "ROAD_rank";
    private final String fileName;
    private final int bufferSize;
    private final GraphDatabaseService db;
    private final Path measureFile;
    private final String workingDirectory;

    public MeasureQueries(String fileName, int bufferSize, String measureFileName, GraphDatabaseService db) {
        this.fileName = fileName;
        workingDirectory = Application.dbDirName(fileName);
        this.bufferSize = bufferSize;
        this.db = db;
        measureFile = Paths.get(workingDirectory, measureFileName);
    }

    public void go() {
        Path workDirPath = Paths.get(workingDirectory);
        try (final Transaction transaction = db.beginTx();
             final FifoBuffer outBuffer = new FifoBuffer(bufferSize, Mode.OUT, workDirPath);
             final FifoBuffer inBuffer = new FifoBuffer(bufferSize, Mode.IN, workDirPath);
             final BufferedWriter bufferedWriter = getBufferedWriter();
             final DiskChDijkstra diskChDijkstra = new DiskChDijkstra(outBuffer, inBuffer)) {
            int max = (int) transaction.getAllNodes().stream().mapToLong(n -> getLongProperty(n, ROAD_RANK_PROPERTY)).max().orElseThrow();
            Random fromRandom = new Random(37);
            Dijkstra dijkstra = new Dijkstra(() -> ImportAndIndex.RELATIONSHIP_NAME, ImportAndIndex.WEIGH_PROPERTY);
            bufferedWriter.append("from,to,cchTime,weight,cchHops,dijkstraHops,bufferSize="+bufferSize+"\n");
            for (int i = 0; i < 100; i++) {
                Random toRandom = new Random(73);
                final int x = fromRandom.nextInt(max);
                final Node from = transaction.findNode(LABEL, ROAD_RANK_PROPERTY, x);
                final Map<Node, WeightedPath> dijkstraPaths = dijkstra.find(from, Set.of());
                for (int j = 0; j < 100; j++) {
                    int y = toRandom.nextInt(max);
                    Node to = transaction.findNode(LABEL, ROAD_RANK_PROPERTY, y);
                    val dijkstraPath = dijkstraPaths.get(to);
                    val chPath = new StoppedResult<>(() -> diskChDijkstra.find(x, y));
                    if (dijkstraPath != null) {
                        if (chPath.getResult().weight() != dijkstraPath.weight()) System.out.println(chPath.getResult().weight() + "vs: " + dijkstraPath.weight());
                        String measure = String.format("%9d,%9d,%9d,%12.2f,%4d,%4d%n",
                                getId(from), getId(to), chPath.getMicros(), chPath.getResult().weight()
                                , chPath.getResult().length()
                                , dijkstraPath.length()
                        );
                        bufferedWriter.append(measure);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedWriter getBufferedWriter() throws IOException {
        File file = measureFile.toFile();
        if (!file.exists()) file.createNewFile();
        return new BufferedWriter(new FileWriter(file, true));
    }
}
