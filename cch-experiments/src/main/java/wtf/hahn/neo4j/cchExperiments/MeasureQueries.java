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
import wtf.hahn.neo4j.dijkstra.DijkstraInfo;
import wtf.hahn.neo4j.util.StoppedResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static wtf.hahn.neo4j.util.EntityHelper.*;

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

    public void go2() {
        Path workDirPath = Paths.get(workingDirectory);
        try (final Transaction transaction = db.beginTx();
             final FifoBuffer outBuffer = new FifoBuffer(bufferSize, Mode.OUT, workDirPath);
             final FifoBuffer inBuffer = new FifoBuffer(bufferSize, Mode.IN, workDirPath);
             final BufferedWriter bufferedWriter = getBufferedWriter();
             final DiskChDijkstra diskChDijkstra = new DiskChDijkstra(outBuffer, inBuffer)) {
            Map<Integer, Map<Node, WeightedPath>> dijkstras = new HashMap<>();
            Map<Integer, Map<Node, DijkstraInfo>> dijkstrasInfos = new HashMap<>();
            Dijkstra dijkstra = new Dijkstra(() -> ImportAndIndex.RELATIONSHIP_NAME, ImportAndIndex.WEIGH_PROPERTY);
            List<Pair> sourceTargetPair = getPairs();
            sourceTargetPair.forEach(pair -> {
                int x = pair.x;
                int y = pair.y;
                final Node from = transaction.findNode(LABEL, ROAD_RANK_PROPERTY, x);
                final Node to = transaction.findNode(LABEL, ROAD_RANK_PROPERTY, y);
                Map<Node, WeightedPath> dijkstraPaths = dijkstras.computeIfAbsent(x, z -> dijkstra.find(from, Set.of()));
                Map<Node, DijkstraInfo> dijkstraInfos = dijkstrasInfos.computeIfAbsent(x, z -> dijkstra.latestQuery.getExpansionInfo());
                val chPath = new StoppedResult<>(() -> diskChDijkstra.find(x, y));
                WeightedPath dijkstraPath = dijkstraPaths.get(to);
                if (dijkstraPath != null) {
                    if (chPath.getResult().weight() != dijkstraPath.weight()) {
                        throw new AssertionError(chPath.getResult().weight() + "vs: " + dijkstraPath.weight());
                    }
                    DijkstraInfo dijkstraInfo = dijkstraInfos.get(to);
                    String measure = String.format("%9d,%9d,%9d,%9d,%9d,%9d,%4d,%4d,%9d,%9d%n"
                            , getId(from)
                            , getId(to)
                            , chPath.getMicros()
                            , chPath.getResult().weight()
                            , chPath.getResult().length()
                            , diskChDijkstra.expandedNodes
                            , diskChDijkstra.getLoadInvocationsLatestQuery()
                            , dijkstraPath.length()
                            , dijkstraInfo.micros()
                            , dijkstraInfo.expandedNodes()
                    );
                    try {
                        bufferedWriter.append(measure);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Pair> getPairs() {
        Random fromRandom = new Random(37);
        LinkedList<Pair> c = new LinkedList<>();
        for (int i = 1; i <= 100; i++) {
            int x = fromRandom.nextInt(i);
            Random toRandom = new Random(37);
            for (int j = 1; j <= 100; j++) {
                c.add(new Pair(x, toRandom.nextInt(j)));
            }
        }
        Collections.shuffle(c);
        return c;
    }

    public void go() {
        Path workDirPath = Paths.get(workingDirectory);
        try (final Transaction transaction = db.beginTx();
             final FifoBuffer outBuffer = new FifoBuffer(bufferSize, Mode.OUT, workDirPath);
             final FifoBuffer inBuffer = new FifoBuffer(bufferSize, Mode.IN, workDirPath);
             final BufferedWriter bufferedWriter = getBufferedWriter();
             final DiskChDijkstra diskChDijkstra = new DiskChDijkstra(outBuffer, inBuffer)) {
            System.out.println(transaction.getAllRelationships().stream().count() + " arcs in the graph");
            int max = (int) transaction.getAllNodes().stream().mapToLong(n -> getLongProperty(n, ROAD_RANK_PROPERTY)).max().orElseThrow();
            Random fromRandom = new Random(37);
            Dijkstra dijkstra = new Dijkstra(() -> ImportAndIndex.RELATIONSHIP_NAME, ImportAndIndex.WEIGH_PROPERTY);
            bufferedWriter.append("from,to,cchTime,weight,cchHops,cchExpanded,cchLoadInvocations,dijkstraHops,dijkstraTime,DijkstraExpandedNodes"+"\n");
            for (int i = 0; i < 100; i++) {
                final int x = fromRandom.nextInt(max);
                final Node from = transaction.findNode(LABEL, ROAD_RANK_PROPERTY, x);
                final Map<Node, WeightedPath> dijkstraPaths = dijkstra.find(from, Set.of());
                val dijkstraQueryInfos = dijkstra.latestQuery.getExpansionInfo();
                Map<Integer, Map<Node, WeightedPath>> chuckedByDistance = getChuckedByDistance(dijkstraPaths);
                for (Map<Node, WeightedPath> nodeAndPath : chuckedByDistance.values()) {
                    Iterator<Map.Entry<Node, WeightedPath>> it = nodeAndPath.entrySet().iterator();
                    for (int j = 0; j < 100 && it.hasNext(); j++) {
                        Map.Entry<Node, WeightedPath> entry = it.next();
                        Node to = entry.getKey();
                        int y = getIntProperty(to, ROAD_RANK_PROPERTY);
                        WeightedPath dijkstraPath = entry.getValue();
                        val chPath = new StoppedResult<>(() -> diskChDijkstra.find(x, y));
                        if (dijkstraPath != null) {
                            if (chPath.getResult().weight() != dijkstraPath.weight())
                                throw new AssertionError(chPath.getResult().weight() + "vs: " + dijkstraPath.weight());
                            DijkstraInfo dijkstraInfo = dijkstraQueryInfos.get(to);
                            String measure = String.format("%9d,%9d,%9d,%9d,%9d,%9d,%4d,%4d,%9d,%9d%n"
                                    , getId(from)
                                    , getId(to)
                                    , chPath.getMicros()
                                    , chPath.getResult().weight()
                                    , chPath.getResult().length()
                                    , diskChDijkstra.expandedNodes
                                    , diskChDijkstra.getLoadInvocationsLatestQuery()
                                    , dijkstraPath.length()
                                    , dijkstraInfo.micros()
                                    , dijkstraInfo.expandedNodes()
                            );
                            bufferedWriter.append(measure);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private  Map<Integer, Map<Node, WeightedPath>> getChuckedByDistance(Map<Node, WeightedPath> dijkstraPaths) {
        Map<Integer, Map<Node, WeightedPath>> xMap = new HashMap<>();
        xMap.put(10, new HashMap<>());
        xMap.put(100, new HashMap<>());
        xMap.put(1000, new HashMap<>());
        xMap.put(10000, new HashMap<>());
        xMap.put(100000, new HashMap<>());
        dijkstraPaths.forEach((node, path) -> {
            double weight = path.weight();
            if (weight < 10) xMap.get(10).put(node, path);
            else if (weight < 100) xMap.get(100).put(node, path);
            else if (weight < 1000) xMap.get(1000).put(node, path);
            else if (weight < 10000) xMap.get(10000).put(node, path);
            else xMap.get(100000).put(node, path);
        });
        return xMap;
    }
    private  Map<Integer, Map<Node, WeightedPath>> getChuckedByHops(Map<Node, WeightedPath> dijkstraPaths) {
        Map<Integer, Map<Node, WeightedPath>> xMap = new HashMap<>();
        xMap.put(10, new HashMap<>());
        xMap.put(100, new HashMap<>());
        xMap.put(1000, new HashMap<>());
        xMap.put(10000, new HashMap<>());
        xMap.put(100000, new HashMap<>());
        dijkstraPaths.forEach((node, path) -> {
            double hops = path.length();
            if (hops < 10) xMap.get(10).put(node, path);
            else if (hops < 100) xMap.get(100).put(node, path);
            else if (hops < 1000) xMap.get(1000).put(node, path);
            else if (hops < 10000) xMap.get(10000).put(node, path);
            else xMap.get(100000).put(node, path);
        });
        return xMap;
    }

    private BufferedWriter getBufferedWriter() throws IOException {
        File file = measureFile.toFile();
        if (!file.exists()) file.createNewFile();
        return new BufferedWriter(new FileWriter(file, true));
    }
    record Pair(int x, int y) { }
}
