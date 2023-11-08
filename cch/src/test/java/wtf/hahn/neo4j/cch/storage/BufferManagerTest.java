package wtf.hahn.neo4j.cch.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wft.hahn.neo4j.cch.storage.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class BufferManagerTest {

    public static final int BUFFER_SIZE = 1024*10;
    @TempDir private static Path basePath;

    @Test
    void baseReadTest() throws Exception {
        Map<Integer, List<DiskArc>> arcGroup = Files.lines(Paths.get("src", "test", "resources", "rome99.csv"))
                .map(line -> Arrays.stream(line.split(",")))
                .map(stream -> stream.map(String::trim).toArray(String[]::new))
                .map(line -> new DiskArc(parseInt(line[0]), parseInt(line[1]), -1, parseInt(line[2])))
                .filter(diskArc -> diskArc.start() < diskArc.end())
                .collect(Collectors.groupingBy(DiskArc::start));
        int sum = arcGroup.values().stream().mapToInt(Collection::size).sum();
        try (ArcWriter arcWriter = new ArcWriter(Mode.OUT, basePath);
             PositionWriter positionWriter = new PositionWriter(Mode.OUT, basePath, sum)) {
            arcGroup.forEach((rank, arcs) -> positionWriter.write(rank, arcWriter.write(arcs)));
        }
        try (FifoBuffer fifoBuffer = new FifoBuffer(BUFFER_SIZE, Mode.OUT, basePath)){
            arcGroup.forEach((rank, arcs) -> {
                System.out.println(rank);
                List<DiskArc> loadArcs = new ArrayList<>(fifoBuffer.arcs(rank));
                assertThat(loadArcs).hasSameElementsAs(arcs);
            });
        }
    }

    @Test
    void readShuffleTest() throws Exception {
        Map<Integer, List<DiskArc>> arcGroup = Files.lines(Paths.get("src", "test", "resources", "rome99.csv"))
                .map(line -> Arrays.stream(line.split(",")))
                .map(stream -> stream.map(String::trim).toArray(String[]::new))
                .map(line -> new DiskArc(parseInt(line[0]), parseInt(line[1]), -1, parseInt(line[2])))
                .filter(diskArc -> diskArc.start() < diskArc.end())
                .distinct()
                .collect(Collectors.groupingBy(DiskArc::start));
        int sum = arcGroup.values().stream().mapToInt(Collection::size).sum();
        Mode out = Mode.OUT;
        try (ArcWriter arcWriter = new ArcWriter(out, basePath);
             PositionWriter positionWriter = new PositionWriter(out, basePath, sum)) {
            arcGroup.forEach((rank, arcs) -> positionWriter.write(rank, arcWriter.write(arcs)));
        }
        try (FifoBuffer fifoBuffer = new FifoBuffer(BUFFER_SIZE, out, basePath)) {
            for (int i = 0; i < 1000; i++) {
                final List<Integer> ranks = new ArrayList<>(arcGroup.keySet());
                Collections.shuffle(ranks, new Random(i));
                for (Integer rank : ranks) {
                    List<DiskArc> arcs = arcGroup.get(rank);
                    List<DiskArc> loadArcs = new ArrayList<>(fifoBuffer.arcs(rank));
                    assertThat(loadArcs).hasSameElementsAs(arcs);
                }
            }
        }
    }

    @Test
    void readShuffleInTest() throws Exception {
        Map<Integer, List<DiskArc>> arcGroup = Files.lines(Paths.get("src", "test", "resources", "rome99.csv"))
                .map(line -> Arrays.stream(line.split(",")))
                .map(stream -> stream.map(String::trim).toArray(String[]::new))
                .map(line -> new DiskArc(parseInt(line[0]), parseInt(line[1]), -1, parseInt(line[2])))
                .filter(diskArc -> diskArc.start() > diskArc.end())
                .distinct()
                .collect(Collectors.groupingBy(DiskArc::end));
        int sum = arcGroup.values().stream().mapToInt(Collection::size).sum();
        Mode mode = Mode.IN;
        try (ArcWriter arcWriter = new ArcWriter(mode, basePath);
             PositionWriter positionWriter = new PositionWriter(mode, basePath, sum)) {
            arcGroup.forEach((rank, arcs) -> positionWriter.write(rank, arcWriter.write(arcs)));
        }
        try (FifoBuffer fifoBuffer = new FifoBuffer(BUFFER_SIZE, mode, basePath)) {
            for (int i = 0; i < 1000; i++) {
                final List<Integer> ranks = new ArrayList<>(arcGroup.keySet());
                Collections.shuffle(ranks, new Random(i));
                for (Integer rank : ranks) {
                    List<DiskArc> arcs = arcGroup.get(rank);
                    List<DiskArc> loadArcs = new ArrayList<>(fifoBuffer.arcs(rank));
                    assertThat(loadArcs).hasSameElementsAs(arcs);
                }
            }
        }
    }
}
